package com.duncodi.ppslink.stanchart.controller;

import com.duncodi.ppslink.stanchart.dto.FiltersRequestDto;
import com.duncodi.ppslink.stanchart.dto.audittrail.AuditTrailResponseDto;
import com.duncodi.ppslink.stanchart.exceptions.CustomException;
import com.duncodi.ppslink.stanchart.service.otherServices.AdminServiceHelper;
import com.duncodi.ppslink.stanchart.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/logs")
public class StraightToBankLogsController{

    private final AdminServiceHelper adminServiceHelper;

    @PostMapping
    public ResponseEntity<ApiResponse<List<AuditTrailResponseDto>>> getList(@RequestBody FiltersRequestDto request){

        try{

            List<AuditTrailResponseDto> list = adminServiceHelper.getAuditTrails(request);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(list));

        }catch (CustomException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(e.getMessage()));
        }

    }

}
