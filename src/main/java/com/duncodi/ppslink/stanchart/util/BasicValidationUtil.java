package com.duncodi.ppslink.stanchart.util;

import com.duncodi.ppslink.stanchart.exceptions.CustomErrorCode;
import com.duncodi.ppslink.stanchart.exceptions.CustomException;

import java.util.List;

public class BasicValidationUtil {

    public static boolean validateAgainstNullAndZero(Long id) {
        return id != null && id != 0;
    }

    public static boolean validateIdentifierIsOkay(Long id) {

        if(id==null || id==0L){
            return false;
        }

        return true;

    }

    public static boolean validateIdentifierAndThrowException(Long id) throws CustomException {

        if(!BasicValidationUtil.validateAgainstNullAndZero(id)){
            throw new CustomException(CustomErrorCode.ID_404, "Identifier Not Provided");
        }

        return true;

    }

    public static boolean validateIdentifiersAndThrowCustomEx(List<Long> ids) throws CustomException {

        if(ids==null || ids.isEmpty()){
            throw new CustomException(CustomErrorCode.ID_404, "Identifier(s) Not Provided");
        }

        return true;

    }

    public static boolean validateStringForNullAndEmpty(String text){

        if (text != null && !text.isEmpty()){
            return true;
        }

        return false;

    }

    public static boolean startAndLimitAvailable(int start, int limit){

        if (start!=-1 && limit!=-1){
            return true;
        }

        return false;

    }

}
