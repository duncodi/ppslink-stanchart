package com.duncodi.ppslink.stanchart.service;

import com.duncodi.ppslink.stanchart.dto.StraightToBankConfigDto;
import com.duncodi.ppslink.stanchart.dto.StraightToBankPayloadDto;
import com.duncodi.ppslink.stanchart.exceptions.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StraightToBankXMLGeneratorService {

    public String generateXMLFile(StraightToBankPayloadDto payload, StraightToBankConfigDto config) throws CustomException {

        StringBuilder sb = new StringBuilder();

        return sb.toString();

    }

}
