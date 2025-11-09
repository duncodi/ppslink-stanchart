package com.duncodi.ppslink.stanchart.controller;

import com.duncodi.ppslink.stanchart.dto.*;
import com.duncodi.ppslink.stanchart.exceptions.CustomException;
import com.duncodi.ppslink.stanchart.service.StraightToBankService;
import com.duncodi.ppslink.stanchart.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/s2b")
public class StraightToBankController{

    private final StraightToBankService service;

    @PostMapping
    public ResponseEntity<ApiResponse<StraightToBankResultsDto>> add(@RequestBody StraightToBankPayloadDto request, HttpServletRequest servletRequest){

        try{

            StraightToBankResultsDto res = service.process(request, servletRequest);

            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Straight to Bank Payment File Submitted for " +
                    "Processing", res));

        }catch (CustomException e){
            e.printStackTrace(System.err);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(e.getMessage()));
        }

    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<StraightToBankConfigDto>> delete(@RequestBody ProcessIDsDto request, HttpServletRequest servletRequest){

        try{

            String res = service.deleteByIds(request, servletRequest);

            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(res));

        }catch (CustomException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(e.getMessage()));
        }

    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StraightToBankBatchResponseDto>>> getList(
            @RequestParam(name = "dateFrom", defaultValue = "") String dateFrom,
            @RequestParam(name = "dateTo", defaultValue = "") String dateTo,
            @RequestParam(name = "searchKey", defaultValue = "") String searchKey,
            @RequestParam(name = "cashbookCode", defaultValue = "") String cashbookCode){

        try{

            List<StraightToBankBatchResponseDto> list = service.getList(dateFrom, dateTo, searchKey, cashbookCode);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(list));

        }catch (CustomException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(e.getMessage()));
        }

    }

    @GetMapping("/breakdown/{batchId}")
    public ResponseEntity<ApiResponse<List<StraightToBankLineResponseDto>>> getList(@PathVariable(name = "batchId") Long batchId){

        try{

            List<StraightToBankLineResponseDto> list = service.getBatchLines(batchId);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(list));

        }catch (CustomException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(e.getMessage()));
        }

    }

}
