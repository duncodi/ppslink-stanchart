package com.duncodi.ppslink.stanchart.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Random;

public class StringUtil {

    public static String tameString(String text, Integer trimLength) {

        trimLength = trimLength==null?255:trimLength;

        if (text == null) {
            return "";
        }

        if (trimLength < 0) {
            throw new IllegalArgumentException("Trim length must be non-negative");
        }

        if (text.length() <= trimLength) {
            return text;
        }

        return text.substring(0, trimLength) + "...";

    }

    public static String formatDecimalToGrid(BigDecimal val) {

        val = val == null ? BigDecimal.ZERO : val;

        String text = "0.00";

        NumberFormat nf = new DecimalFormat("#,###,###.00");

        if (val.compareTo(BigDecimal.ZERO) != 0) {
            text = nf.format(val);
        }

        return text;

    }

    public static String tameStringCustom(String text, int characters) {

        if (text == null) {
            return "-";
        }

        if (text.length() > (characters)) {
            text = text.substring(0, characters - 1);
        }

        text = text.replaceAll("\\s+", " ");

        return text;

    }

    public static String replaceSpecialCharactersLeave(String text) {

        text = text.replace(",", "");
        text = text.replace("(", "");
        text = text.replace(")", "");
        text = text.replace("`", "");
        text = text.replace("~", "");
        text = text.replace("!", "");
        text = text.replace("@", "");
        text = text.replace("#", "");
        text = text.replace("%", "");
        text = text.replace("^", "");
        text = text.replace("&", "");
        text = text.replace("*", "");
        text = text.replace("=", "");
        text = text.replace("[", "");
        text = text.replace("]", "");
        text = text.replace("{", "");
        text = text.replace("}", "");
        text = text.replace(";", "");
        text = text.replace("'", "");
        text = text.replace("\"", "");
        text = text.replace("/", "");
        text = text.replace("|", "");
        text = text.replace("<", "");
        text = text.replace(">", "");
        text = text.replace("?", "");

        return text;

    }

    public static String generateRandomString(int len) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

}
