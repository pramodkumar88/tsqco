package com.tsqco.helper;

import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

@Slf4j
public class DateHelper {
    public static LocalDateTime convertToIndiaTimeZone(Date date, String type) {
        Date date1 = addTimes(date, type);
        return LocalDateTime.ofInstant(
                date1.toInstant(), ZoneId.of("Asia/Kolkata"));
    }

    public static Date addTimes(Date date, String type){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if(type.equalsIgnoreCase("FROM")) {
            calendar.add(Calendar.HOUR_OF_DAY, 9);
            calendar.add(Calendar.MINUTE, 30);
        } else {
            calendar.add(Calendar.HOUR_OF_DAY, 15);
            calendar.add(Calendar.MINUTE, 30);
        }
        return calendar.getTime();
    }

    public static boolean timeRangeCheck(String timestamp) {
        log.debug("Instrument last loaded {}",timestamp);
        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        ZonedDateTime now = ZonedDateTime.now(istZone);
        ZonedDateTime lastLoadedTime = ZonedDateTime.parse(timestamp);
        // Calculate today at 9 AM in IST
        ZonedDateTime today9AM = now.with(LocalTime.of(9, 0, 0));
        // Calculate yesterday at 4 PM in IST
        ZonedDateTime yesterday4PM = now.minus(1, ChronoUnit.DAYS).with(LocalTime.of(16, 0, 0));
        // Check if the given timestamp is within the range
        return lastLoadedTime.isAfter(yesterday4PM) && now.isBefore(today9AM);

    }



}
