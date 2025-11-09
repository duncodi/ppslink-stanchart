package com.duncodi.ppslink.stanchart.controller;

import com.duncodi.ppslink.stanchart.dto.ProcessIDsDto;
import com.duncodi.ppslink.stanchart.dto.StraightToBankConfigDto;
import com.duncodi.ppslink.stanchart.exceptions.CustomException;
import com.duncodi.ppslink.stanchart.service.StraightToBankConfigService;
import com.duncodi.ppslink.stanchart.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/config")
public class StraightToBankConfigController{

    private final StraightToBankConfigService service;

    @PostMapping
    public ResponseEntity<ApiResponse<StraightToBankConfigDto>> add(@RequestBody StraightToBankConfigDto request, HttpServletRequest servletRequest){

        try{

            StraightToBankConfigDto config = service.process(request, servletRequest);

            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Configuration Created Successfully", config));

        }catch (CustomException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(e.getMessage()));
        }

    }

    @PutMapping
    public ResponseEntity<ApiResponse<StraightToBankConfigDto>> update(@RequestBody StraightToBankConfigDto request, HttpServletRequest servletRequest){

        try{

            StraightToBankConfigDto config = service.update(request, servletRequest);

            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Configuration Updated Successfully", config));

        }catch (CustomException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(e.getMessage()));
        }

    }

    @GetMapping("/getById/{id}")
    public ResponseEntity<ApiResponse<StraightToBankConfigDto>> getById(@PathVariable("id") Long id){

        try{

            StraightToBankConfigDto config = service.findById(id);

            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(config));

        }catch (CustomException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(e.getMessage()));
        }

    }

    @GetMapping("/get-default")
    public ResponseEntity<ApiResponse<StraightToBankConfigDto>> getDefaultConfig(){

        try{

            StraightToBankConfigDto config = service.findOne(false);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(config));

        }catch (CustomException e){
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
    public ResponseEntity<ApiResponse<List<StraightToBankConfigDto>>> getList(@RequestParam(name = "searchKey", defaultValue = "") String searchKey){

        try{

            List<StraightToBankConfigDto> list = service.getList();

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(list));

        }catch (CustomException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(e.getMessage()));
        }

    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<StraightToBankConfigDto>> activate(@RequestBody StraightToBankConfigDto request,
                                                                                HttpServletRequest servletRequest){

        try{

            StraightToBankConfigDto config = service.processUrlActivation(request, servletRequest);

            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("API URL Activation Done Successfully!", config));

        }catch (CustomException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(e.getMessage()));
        }

    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<String>> test(@RequestBody StraightToBankConfigDto request,
                                                    HttpServletRequest servletRequest){

        try{

            String test = service.performTest(request, servletRequest);

            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(test));

        }catch (CustomException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(e.getMessage()));
        }

    }

}
