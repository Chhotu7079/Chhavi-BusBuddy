package com.chhavi.busbuddy_backend.util;

import com.chhavi.busbuddy_backend.persistence.model.Schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Hosts schedule-specific transformations and time calculations that are shared
 * across history tracking and delay analysis workflows.
 */
public final class ScheduleUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private ScheduleUtils() {
    }

    public static void replaceNonNullValuesWithPlaceholder(Map<String, List<String>> day) {
        for (Map.Entry<String, List<String>> entry : day.entrySet()) {
            List<String> times = new ArrayList<>();
            for (String time : entry.getValue()) {
                times.add(time != null ? "-" : null);
            }
            day.put(entry.getKey(), times);
        }
    }

    public static void replaceDashWithNull(Map<String, List<String>> times) {
        for (List<String> stopTimes : times.values()) {
            for (int index = 0; index < stopTimes.size(); index++) {
                if ("-".equals(stopTimes.get(index))) {
                    stopTimes.set(index, null);
                }
            }
        }
    }

    public static int calculateDelay(String expectedTime, String actualTime) {
        String[] expected = expectedTime.split(":");
        String[] actual = actualTime.split(":");
        int expectedMinutes = Integer.parseInt(expected[0]) * 60 + Integer.parseInt(expected[1]);
        int actualMinutes = Integer.parseInt(actual[0]) * 60 + Integer.parseInt(actual[1]);
        return actualMinutes - expectedMinutes;
    }

    public static String averageTime(String time1, String time2) {
        LocalTime first = LocalTime.parse(time1, DateTimeFormatter.ofPattern("H:mm"));
        LocalTime second = LocalTime.parse(time2, DateTimeFormatter.ofPattern("H:mm"));
        int avgMinutes = ((first.getHour() * 60 + first.getMinute()) + (second.getHour() * 60 + second.getMinute())) / 2;
        return String.format("%02d:%02d", avgMinutes / 60, avgMinutes % 60);
    }

    public static void sortDatesAscending(List<String> dates) {
        Collections.sort(dates, Comparator.comparing(date -> LocalDate.parse(date, DATE_FORMATTER)));
    }

    public static int firstUpcomingIndex(List<String> historyTimes) {
        if (historyTimes == null) {
            return 0;
        }
        int startIndex = 0;
        for (String time : historyTimes) {
            if (time == null || !time.equals("-")) {
                startIndex += 1;
            }
        }
        return startIndex;
    }

    public static Schedule copyScheduleStructureAsHistory(Schedule timetable) {
        Schedule schedule = new Schedule();
        Map<String, List<String>> forward = timetable.getForward();
        Map<String, List<String>> back = timetable.getBack();
        replaceNonNullValuesWithPlaceholder(forward);
        replaceNonNullValuesWithPlaceholder(back);
        schedule.setForward(forward);
        schedule.setBack(back);
        return schedule;
    }
}
