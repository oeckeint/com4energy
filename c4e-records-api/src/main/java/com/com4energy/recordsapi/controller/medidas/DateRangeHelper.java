package com.com4energy.recordsapi.controller.medidas;

import com.com4energy.recordsapi.common.MessageKey;
import com.com4energy.recordsapi.common.Messages;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Helper class for parsing and processing date ranges in Medidas endpoints.
 * Handles various date formats and applies sensible defaults for time windows.
 */
public final class DateRangeHelper {

    private DateRangeHelper() {
        throw new IllegalStateException(Messages.get(MessageKey.UTILITY_CLASS));
    }

    /**
     * Parses a date string in multiple formats:
     * - OffsetDateTime (ISO-8601 with timezone)
     * - LocalDate (yyyy-MM-dd) - converted to start of day
     * - LocalDateTime (ISO-8601 without timezone)
     *
     * @param dateString the date string to parse
     * @param endOfDay if true and date is yyyy-MM-dd format, returns end of day instead of start
     * @return LocalDateTime or null if input is null/blank
     */
    public static LocalDateTime parseDate(String dateString, boolean endOfDay) {
        if (dateString == null || dateString.isBlank()) {
            return null;
        }

        try {
            // Try OffsetDateTime first (most complete format)
            return OffsetDateTime.parse(dateString).toLocalDateTime();
        } catch (Exception e) {
            // If simple date format (yyyy-MM-dd)
            if (dateString.length() == MedidasConstants.DATE_ONLY_LENGTH) {
                LocalDate date = LocalDate.parse(dateString);
                return endOfDay ? date.atTime(
                    MedidasConstants.END_OF_DAY_HOUR,
                    MedidasConstants.END_OF_DAY_MINUTE,
                    MedidasConstants.END_OF_DAY_SECOND,
                    MedidasConstants.END_OF_DAY_NANO
                ) : date.atStartOfDay();
            }
            // Try LocalDateTime
            return LocalDateTime.parse(dateString);
        }
    }

    /**
     * Applies a time window when only one date bound is provided.
     * If start is provided but not end: end = start + windowDays
     * If end is provided but not start: start = end - windowDays
     *
     * @param start the start date (may be null)
     * @param end the end date (may be null)
     * @param windowDays the number of days for the window
     * @return DateRange with adjusted dates
     */
    public static DateRange applyDefaultWindow(LocalDateTime start, LocalDateTime end, int windowDays) {
        LocalDateTime adjustedStart = start;
        LocalDateTime adjustedEnd = end;

        if (start != null && end == null) {
            adjustedEnd = start.plusDays(windowDays);
        } else if (end != null && start == null) {
            adjustedStart = end.minusDays(windowDays);
        }

        return new DateRange(adjustedStart, adjustedEnd);
    }


    /**
     * Creates a DateRange for the last N days from now.
     *
     * @param days number of days to go back
     * @return DateRange from (now - days) to now
     */
    public static DateRange lastNDays(int days) {
        OffsetDateTime now = OffsetDateTime.now();
        LocalDateTime end = now.toLocalDateTime();
        LocalDateTime start = now.minusDays(days).toLocalDateTime();
        return new DateRange(start, end);
    }

    /**
     * Simple container for date range results.
     */
    @Getter
    public static class DateRange {
        private final LocalDateTime start;
        private final LocalDateTime end;

        public DateRange(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
    }
}



