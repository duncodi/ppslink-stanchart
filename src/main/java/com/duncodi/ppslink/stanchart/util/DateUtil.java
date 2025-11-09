package com.duncodi.ppslink.stanchart.util;

import com.duncodi.ppslink.stanchart.enums.CustomMonth;
import com.duncodi.ppslink.stanchart.exceptions.CustomException;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Slf4j
public class DateUtil implements java.io.Serializable {

    public static Date addMinutesToDateTime(Date dateTimeNow, int minutes) {

        if (dateTimeNow == null) {
            throw new CustomException("Date/Time cannot be null");
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateTimeNow);
        calendar.add(Calendar.MINUTE, minutes);

        return calendar.getTime();

    }

    public static String convertDateToGridLong(Date date) {

        if (date == null) {
            return "-";
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int day = cal.get(Calendar.DAY_OF_MONTH);
        String daySuffix = getDaySuffix(day);

        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy");
        String monthYear = monthYearFormat.format(date);

        return day + daySuffix + " " + monthYear;

    }

    private static String getDaySuffix(int day) {
        if (day >= 11 && day <= 13) {
            return "th";
        }
        return switch (day % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    public static String convertDateToGridShort(Date date) {

        if (date == null) {
            return "-";
        }

        return new SimpleDateFormat("dd-MMM-yyyy").format(date);

    }

    public static CustomMonth convertDateToMonth(Date date) {
        if (date == null) return null;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        int month = calendar.get(Calendar.MONTH) + 1;

        for (CustomMonth customMonth : CustomMonth.values()) {
            if (customMonth.getMonthInt() != null && customMonth.getMonthInt() == month) {
                return customMonth;
            }
        }

        return null;

    }

    public static Integer convertDateToYear(Date date) {
        if (date == null) return null;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    public static Date convertStringToDate(String dateStr) {

        if (dateStr == null || dateStr.trim().equalsIgnoreCase("")) {
            return null;
        }

        try{

            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");

            Date date = df.parse(dateStr);

            return date;

        }catch (Exception e){
            throw new CustomException("Invalid Date Provided "+dateStr);
        }

    }

    public static Date constructDate(int day, int month, int year) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month - 1); // Month is 0-based in Calendar
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar.getTime();
        } catch (Exception e) {
            throw new CustomException("Unable to construct date");
        }
    }

    public static Date getLastDateOfTheMonth(Date date) {
        try {

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            // Set to last day of the month
            int lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

            calendar.set(Calendar.DAY_OF_MONTH, lastDay);

            // Optional: reset time to midnight
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            return calendar.getTime();

        } catch (Exception e) {
            throw new CustomException("Unable to get Last date of the month");
        }
    }

    public static CustomMonth getMonthFromDate(Date date) {

        try {

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int mon = calendar.get(Calendar.MONTH) + 1;
            return CustomMonth.getMonthByMonthInt(mon);

        } catch (Exception e) {
            throw new CustomException("Unable to get month");
        }


    }

    public static int getYearFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

}
