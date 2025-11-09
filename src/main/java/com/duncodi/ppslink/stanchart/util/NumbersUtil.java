package com.duncodi.ppslink.stanchart.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Random;

public class NumbersUtil {

    public static int generateRandomNumber(Integer characters) {

        characters = (characters == null) ? 4 : characters;

        if (characters < 1 || characters > 9) {
            throw new IllegalArgumentException("Number of characters must be between 1 and 9");
        }

        Random rand = new Random();

        int min = (int) Math.pow(10, characters - 1);
        int max = (int) Math.pow(10, characters) - 1;

        return rand.nextInt(max - min + 1) + min;

    }

    public static BigDecimal sanitizeBigdecimal(BigDecimal amount){

        if(amount==null){
            return BigDecimal.ZERO;
        }

        return amount;

    }

    public static String formatDecimalToGrid(BigDecimal val) {

        String text = "0.00";
        NumberFormat nf = new DecimalFormat("#,###,###.00");

        if (val != null && BigDecimal.ZERO. compareTo(val) != 0) text = nf.format(val);

        return text;
    }

    public static String formatDoubleToGrid(Double val) {

        String text = "0.00";
        NumberFormat nf = new DecimalFormat("#,###,###.00");

        if (val != null && val != 0) {
            text = nf.format(val);
        }

        return text;

    }

    public static String leftPadNumber(int number, int paddingValue){

        StringBuilder zeros = new StringBuilder();

        while (paddingValue>0){
            zeros.append("0");
            paddingValue--;
        }

        DecimalFormat df = new DecimalFormat(zeros.toString());

        String paddedNumber = df.format(number);

        return paddedNumber;

    }

    public static String leftPadNumber(Long number, int paddingValue){

        StringBuilder zeros = new StringBuilder();

        while (paddingValue>0){
            zeros.append("0");
            paddingValue--;
        }

        DecimalFormat df = new DecimalFormat(zeros.toString());

        String paddedNumber = df.format(number);

        return paddedNumber;

    }

    public static BigDecimal convertTextToBigDecimal(String text){

        text = text==null?"0":text;
        text = text.replaceAll(",", "");

        BigDecimal num = BigDecimal.ZERO;

        try{
            num = new BigDecimal(text);
        }catch (Exception e){
            e.printStackTrace(System.err);
        }

        return num;

    }

    public static Long convertTextToLong(String text){

        text = text==null?"0":text;
        text = text.replaceAll(",", "");

        Long num = 0L;

        try{
            num = Long.parseLong(text);
        }catch (Exception e){
            e.printStackTrace(System.err);
        }

        return num;

    }

}
