package com.chhavi.busbuddy_backend.util;

import com.chhavi.busbuddy_backend.config.AppProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Centralizes application time access so all date/time calculations use the
 * same configured timezone instead of relying on host defaults.
 */
@Component
public class DateTimeProvider {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ZoneId zoneId;

    public DateTimeProvider(AppProperties appProperties) {
        this.zoneId = ZoneId.of(appProperties.getTimezone());
    }

    public String currentDate() {
        return LocalDate.now(zoneId).format(DATE_FORMATTER);
    }

    public String currentTime() {
        return LocalTime.now(zoneId).format(TIME_FORMATTER);
    }
}
