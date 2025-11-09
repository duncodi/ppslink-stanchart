package com.duncodi.ppslink.stanchart.service;

import com.duncodi.ppslink.stanchart.dto.*;
import com.duncodi.ppslink.stanchart.dto.audittrail.AuditTrailRequestDto;
import com.duncodi.ppslink.stanchart.enums.*;
import com.duncodi.ppslink.stanchart.exceptions.CustomErrorCode;
import com.duncodi.ppslink.stanchart.exceptions.CustomException;
import com.duncodi.ppslink.stanchart.model.StraightToBankBatch;
import com.duncodi.ppslink.stanchart.model.StraightToBankBatchLine;
import com.duncodi.ppslink.stanchart.repository.StraightToBankBatchLineRepository;
import com.duncodi.ppslink.stanchart.repository.StraightToBankBatchRepository;
import com.duncodi.ppslink.stanchart.repository.StraightToBankBatchSpecifications;
import com.duncodi.ppslink.stanchart.repository.StraightToBankLineRepository;
import com.duncodi.ppslink.stanchart.service.otherServices.AdminServiceHelper;
import com.duncodi.ppslink.stanchart.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StraightToBankService {

    private final StraightToBankBatchLineRepository bankBatchLineRepository;
    private final StraightToBankBatchRepository batchRepository;
    private final StraightToBankXMLGeneratorService xmlGeneratorService;
    private final StraightToBankConfigService configService;
    private final SshAndPgpEncryptionService sshAndPgpEncryptionService;
    private final SftpFileUploadService sftpFileUploadService;
    private final AdminServiceHelper adminServiceHelper;
    private final AuditTrailService auditTrailService;

    private final StraightToBankLineRepository lineRepository;

    public StraightToBankPayloadDto getNextMessageId(StraightToBankPayloadDto payload){

        Long messageIdSeq = batchRepository.findMaxMessageIdSeqNative()+1;

        String msgIdPadded = NumbersUtil.leftPadNumber(messageIdSeq, 7);
        String schemeCode = payload.getSchemeCode()==null?"PPS":payload.getSchemeCode();
        Date valueDate = DateUtil.convertStringToDate(payload.getValueDate());
        String preparerInitials = payload.getPreparerInitials();

        if(preparerInitials==null){
            preparerInitials = (payload.getPreparedByName()==null?"PL":payload.getPreparedByName()).substring(0, 4);
        }

        int year = DateUtil.getYearFromDate(valueDate);
        CustomMonth month = DateUtil.getMonthFromDate(valueDate);

        String messageId = (schemeCode+month.name()+year+msgIdPadded+preparerInitials).toUpperCase();

        messageId = StringUtil.tameStringCustom(StringUtil.replaceSpecialCharactersLeave(messageId), 28);

        payload.setMessageIdSeq(messageIdSeq);
        payload.setMessageId(messageId);

        log.info("messageId>>>>"+messageId);

        return payload;

    }

    public StraightToBankBatchResponseDto findById(Long id){

        BasicValidationUtil.validateIdentifierAndThrowException(id);

        StraightToBankBatch batch = batchRepository.findById(id).orElse(null);

        if(batch==null){
            throw new CustomException("Batch not found using identifier "+id);
        }

        return this.convertEntityToResponseDto(batch);

    }

    @Transactional
    public StraightToBankResultsDto process(StraightToBankPayloadDto request, HttpServletRequest servletRequest){

        if(request==null){
            throw new CustomException(CustomErrorCode.REQ_404);
        }

        UserDto actor = adminServiceHelper.getUserFromToken(JWTUtil.getTokenFromServletRequest(servletRequest));

        String ipAddress = HttpServletRequestUtil.getClientIp(servletRequest);

        request = this.getNextMessageId(request);

        StraightToBankBatch batch = this.convertPaymentDtoToEntity(request);

        String schemeCode = batch.getSchemeCode();

        if(schemeCode==null){
            throw new CustomException(CustomErrorCode.OBJ_404, "Scheme Code Not Provided on Payload");
        }

        StraightToBankConfigDto config = configService.findOne(true);

        if(config==null){
            throw new CustomException(CustomErrorCode.OBJ_404, "Configuration Not Found for Scheme "+schemeCode);
        }

        log.info("Processing Payment File ...."+batch.getMessageId());

        List<StraightToBankPayloadLineDto> linesAll = request.getLines()==null?new ArrayList<>():request.getLines();

        if(linesAll.isEmpty()){
            throw new CustomException(CustomErrorCode.REQ_404, "Payment Lines Not Provided");
        }

        List<StraightToBankBatchLine> linesConverted = linesAll.stream()
                .map(this::convertLineDtoToEntity)
                .toList();

        List<StraightToBankBatchLine> linesWithoutGroup = linesConverted.stream()
                .filter(l->l.getInstructionGroup()==null)
                .toList();

        List<StraightToBankBatchLine> linesWithGroup = linesConverted.stream()
                .filter(l->l.getInstructionGroup()!=null)
                .toList();

        String defaultGroupCode = StringUtil.tameStringCustom(batch.getMessageId()+StringUtil.generateRandomString(4), 35).toUpperCase();

        for(StraightToBankBatchLine line : linesWithoutGroup){

            line.setInstructionGroup(defaultGroupCode);

        }

        for(StraightToBankBatchLine line : linesWithGroup){

            String grp = line.getInstructionGroup();
            String updatedGroupCode = StringUtil.tameStringCustom(batch.getMessageId()+grp, 35).toUpperCase();

            line.setInstructionGroup(updatedGroupCode);

        }

        List<StraightToBankBatchLine> lines = new ArrayList<>(linesWithGroup);

        if(!linesWithoutGroup.isEmpty()){
            lines.addAll(linesWithoutGroup);
        }

        batch.addLines(lines);

        boolean promoteToProduction = YesNo.YES.equals(config.getPromoteToProduction());

        boolean encrypt = false;

        String testOrProduction = "_TST_";

        if(promoteToProduction){
            testOrProduction = "_PRD_";
        }

        String countryCodeFileName = batch.getCountryCode()==null?"UG":request.getCountryCode();

        DateFormat outputFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

        String fileNameNative = "_Pain001v3_"+countryCodeFileName+testOrProduction+outputFormat.format(new Date())+".xml";

        String fileName = "/"+fileNameNative;

        log.info("file path:::::::::"+fileName);

        try{
            this.generateXmlFile(null, fileName);
        }catch (Exception e){
            e.printStackTrace(System.err);
            throw new CustomException("Unable to generate Document. "+e.getMessage());
        }

        String encryptedFileName = fileName;

        String accessToken = JWTUtil.getTokenFromServletRequest(servletRequest);

        log.info("encrypt>>>>"+encrypt);

        batch.setDecryptedFileName(fileName);
        batch.setFileNameNative(fileNameNative);
        batch.setEncryptedFileName(encryptedFileName);

        batch = batchRepository.save(batch);

        auditTrailService.buildAndSendSingleTrail(CrudOperationType.CREATE, actor.getId(),
                "Straight to Bank Batch Saved "+fileNameNative, JsonUtil.convertToJsonString(batch),
                JsonUtil.convertToJsonString(batch), actor, ipAddress, accessToken);

        List<AuditTrailRequestDto> auditList = new ArrayList<>();

        for(StraightToBankBatchLine line : batch.getLines()){

            String currentState = JsonUtil.convertToJsonString(line);

            String objectDescription = "Straight to Bank Payment Line "+line.getParticulars()+" Saved";

            AuditTrailRequestDto auditTrailRequest = auditTrailService.buildAuditTrailRequest(CrudOperationType.CREATE, line.getId(),
                    objectDescription, currentState, currentState, actor, ipAddress);

            auditList.add(auditTrailRequest);

        }

        auditTrailService.sendAuditTrail(auditList, accessToken);

        StraightToBankResultsDto results = new StraightToBankResultsDto();
        results.setFilePath(fileName);
        results.setFileName(fileNameNative);
        results.setStatus("Done Sending Payment File to Stanbic Bank");
        results.setBatchId(batch.getId());

        return results;

    }

    public void generateXmlFile(String xmlSource, String path) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlSource)));

        // Write the parsed document to an xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);

        StreamResult result =  new StreamResult(new File(path));
        transformer.transform(source, result);

    }

    public StraightToBankBatchResponseDto convertEntityToResponseDto(StraightToBankBatch batch){

        return StraightToBankBatchResponseDto.builder()
                .id(batch.getId())
                .messageId(batch.getMessageId())
                .messageIdSeq(batch.getMessageIdSeq())
                .schemeId(batch.getSchemeId())
                .schemeName(batch.getSchemeName())
                .schemeCode(batch.getSchemeCode())
                .batchDate(DateUtil.convertDateToGridShort(batch.getBatchDate()))
                .preparedById(batch.getPreparedById())
                .preparedByName(batch.getPreparedByName())
                .cashbookId(batch.getCashbookId())
                .cashbookName(batch.getCashbookName())
                .cashbookAccountName(batch.getDebitAccountName())
                .cashbookAccountNo(batch.getDebitAccountNo())
                .countLines(batch.getCountTransactions())
                .totalSourceCurrency(batch.getTotal())
                .totalBaseCurrency(batch.getTotalBc())
                .consolidatePaymentLines(batch.getConsolidatedPosting())
                .instructionPriority(batch.getInstructionPriority())
                .valueDate(DateUtil.convertDateToGridShort(batch.getValueDate()))
                .chargeBearer(batch.getChargeBearer())
                .batchTitle(batch.getBatchTitle())
                .build();

    }

    public StraightToBankLineResponseDto convertLineEntityToResponseDto(StraightToBankBatchLine line){

        return StraightToBankLineResponseDto.builder()
                .id(line.getId())
                .paymentId(line.getPaymentId())
                .batchPaymentScheduleId(line.getBatchPaymentScheduleId())
                .customerRef(line.getCustomerRef())
                .amountSc(line.getAmountSc())
                .amountBc(line.getAmountBc())
                .spotRate(line.getSpotRate())
                .currencyId(line.getCurrencyId())
                .paymentCurrencyCode(line.getPaymentCurrencyCode())
                .purposeOfPayment(line.getPurposeOfPayment())
                .forexType(line.getForexType())
                .forexDealNo(line.getForexDealNo())
                .forexDealerName(line.getForexDealerName())
                .forexDirectInverse(line.getForexDirectInverse())
                .maturityDate(line.getMaturityDate() != null ? DateUtil.convertDateToGridShort(line.getMaturityDate()) : null)
                .intermediaryBankCode(line.getIntermediaryBankCode())
                .localBankCode(line.getLocalBankCode())
                .bankCode(line.getBankCode())
                .branchCode(line.getBranchCode())
                .bankName(line.getBankName())
                .swiftCode(line.getSwiftCode())
                .bankCountryCode(line.getBankCountryCode())
                .accountName(line.getAccountName())
                .accountNo(line.getAccountNo())
                .beneficiaryName(line.getBeneficiaryName())
                .email(line.getEmail())
                .particulars(line.getParticulars())
                .debitAccountNo(line.getDebitAccountNo())
                .debitAccountName(line.getDebitAccountName())
                .paymentType(line.getPaymentType())
                .refCode(line.getRefCode())
                .transactionCurrency(line.getTransactionCurrency())
                .beneficiaryAddress(line.getBeneficiaryAddress())
                .beneficiaryCountryCode(line.getBeneficiaryCountryCode())
                .beneficiaryCountryName(line.getBeneficiaryCountryName())
                .jsonRequest(line.getJsonRequest())
                .recallInitiatedById(line.getRecallInitiatedById())
                .recallInitiatedByName(line.getRecallInitiatedByName())
                .recallInitiatedDate(line.getRecallInitiatedDate() != null ? DateUtil.convertDateToGridShort(line.getRecallInitiatedDate()) : null)
                .recallCertifiedById(line.getRecallCertifiedById())
                .recallCertifiedByName(line.getRecallCertifiedByName())
                .recallCertifiedDate(line.getRecallCertifiedDate() != null ? DateUtil.convertDateToGridShort(line.getRecallCertifiedDate()) : null)
                .recallApprovedById(line.getRecallApprovedById())
                .recallApprovedByName(line.getRecallApprovedByName())
                .recallApprovedDate(line.getRecallApprovedDate() != null ? DateUtil.convertDateToGridShort(line.getRecallApprovedDate()) : null)
                .comments(line.getComments())
                .oldPaymentId(line.getOldPaymentId())
                .instructionGroup(line.getInstructionGroup())
                .build();

    }

    public StraightToBankBatch convertPaymentDtoToEntity(StraightToBankPayloadDto batch){

        YesNo consolidateLines = YesNo.YES.name().equalsIgnoreCase(batch.getConsolidatePaymentLines())?YesNo.YES:YesNo.NO;
        StraightToBankInstructionPriority instructionPriority = StraightToBankInstructionPriority.fromString(batch.getInstructionPriority());
        StraightToBankChargeBearer chargeBearer = StraightToBankChargeBearer.fromString(batch.getChargeBearer());

        return StraightToBankBatch.builder()
                .messageId(batch.getMessageId())
                .messageIdSeq(batch.getMessageIdSeq())
                .schemeId(batch.getSchemeId())
                .schemeName(batch.getSchemeName())
                .schemeCode(batch.getSchemeCode())
                .countryCode(batch.getCountryCode())
                .batchDate(new Date())
                .preparedById(batch.getPreparedById())
                .preparedByName(batch.getPreparedByName())
                .cashbookId(batch.getCashbookId())
                .cashbookName(batch.getCashbookName())
                .debitAccountName(batch.getCashbookAccountName())
                .debitAccountNo(batch.getCashbookAccountNo())
                .countTransactions(batch.getCountLines())
                .total(batch.getTotalSourceCurrency())
                .totalBc(batch.getTotalBaseCurrency())
                .consolidatedPosting(consolidateLines)
                .instructionPriority(instructionPriority)
                .valueDate(DateUtil.convertStringToDate(batch.getValueDate()))
                .chargeBearer(chargeBearer)
                .batchTitle(batch.getBatchTitle())
                .build();

    }

    public StraightToBankBatchLine convertLineDtoToEntity(StraightToBankPayloadLineDto line){

        return StraightToBankBatchLine.builder()
                .paymentId(line.getPaymentId())
                .batchPaymentScheduleId(line.getBatchPaymentScheduleId())
                .customerRef(line.getCustomerRef())
                .amountSc(line.getAmountSc())
                .amountBc(line.getAmountBc())
                .spotRate(line.getSpotRate())
                .currencyId(line.getCurrencyId())
                .paymentCurrencyCode(line.getPaymentCurrencyCode())
                .purposeOfPayment(line.getPurposeOfPayment())
                .forexType(line.getForexType())
                .forexDealNo(line.getForexDealNo())
                .forexDealerName(line.getForexDealerName())
                .forexDirectInverse(line.getForexDirectInverse())
                .maturityDate(line.getMaturityDate()!=null? DateUtil.convertStringToDate(line.getMaturityDate()):null)
                .intermediaryBankCode(line.getIntermediaryBankCode())
                .localBankCode(line.getLocalBankCode())
                .bankCode(line.getBankCode())
                .branchCode(line.getBranchCode())
                .bankName(line.getBankName())
                .swiftCode(line.getSwiftCode())
                .bankCountryCode(line.getBankCountryCode())
                .accountName(line.getAccountName())
                .accountNo(line.getAccountNo())
                .beneficiaryName(line.getBeneficiaryName())
                .email(line.getEmail())
                .particulars(line.getParticulars())
                .paymentType(line.getPaymentType())
                .refCode(line.getRefCode())
                .transactionCurrency(line.getTransactionCurrency())
                .beneficiaryAddress(line.getBeneficiaryAddress())
                .beneficiaryCountryCode(line.getBeneficiaryCountryCode())
                .beneficiaryCountryName(line.getBeneficiaryCountryName())
                .instructionGroup(line.getInstructionGroup())
                .build();

    }

    public String deleteByIds(ProcessIDsDto request, HttpServletRequest servletRequest){

        if(request==null){
            throw new CustomException(CustomErrorCode.REQ_404);
        }

        List<Long> ids = request.getIds();

        BasicValidationUtil.validateIdentifiersAndThrowCustomEx(ids);

        List<StraightToBankBatch> list = batchRepository.findAllById(ids);

        if(list.isEmpty()){
            throw new CustomException(CustomErrorCode.LIST_404, "No Batches Found");
        }

        String ipAddress = HttpServletRequestUtil.getClientIp(servletRequest);

        int count = 0;

        for(StraightToBankBatch batch : list){

          /*  auditTrailService.trail(JsonUtil.convertToPrettyJsonString(user), JsonUtil.convertToPrettyJsonString(user), null, ipAddress,
                    CrudOperationType.DELETE, user.getId(), "User Details Deleted", true);
*/
            batchRepository.deleteById(batch.getId());

            count++;

        }

        return count+" Batch(es) Deleted Successfully";

    }

    public List<StraightToBankBatchResponseDto> getList(String dateFromStr, String dateToStr, String searchKey, String cashbookCode){

        Date dateFrom = DateUtil.convertStringToDate(dateFromStr);
        Date dateTo = DateUtil.convertStringToDate(dateToStr);

        List<StraightToBankBatch> batches = batchRepository.findAll(
                StraightToBankBatchSpecifications.filter(dateFrom, dateTo, searchKey, cashbookCode),
                Sort.by(Sort.Direction.DESC, "batchDate")
        );

        List<StraightToBankBatchResponseDto> list =  batches.stream()
                .map(this::convertEntityToResponseDto)
                .toList();

        return list;

    }

    public List<StraightToBankLineResponseDto> getBatchLines(Long batchId){

        List<StraightToBankBatchLine> lines = bankBatchLineRepository.getStraightToBankBatchLineByBatchIdOrderByCustomerRefAsc(batchId);

        List<StraightToBankLineResponseDto> list =  lines.stream()
                .map(this::convertLineEntityToResponseDto)
                .toList();

        return list;

    }

}
