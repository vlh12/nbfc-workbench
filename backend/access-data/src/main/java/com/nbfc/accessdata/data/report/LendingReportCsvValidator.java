package com.nbfc.accessdata.data.report;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class LendingReportCsvValidator {

    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final int MAX_ROWS = 10000;
    private static final int MAX_ISSUES = 100;

    private static final String HEADER =
            "bank_id,nbfc_id,loan_id,reporting_date,"
            + "amount_lent,interest_amount,total_payable,amount_repaid,"
            + "outstanding_amount,revenue_period_start,revenue_period_end,"
            + "revenue_amount,currency,unit";

    private static final Pattern SYNTHETIC_ID =
            Pattern.compile("SYN-[A-Z0-9-]+");

    private static final Pattern MONEY =
            Pattern.compile("[0-9]{1,16}(\\.[0-9]{1,2})?");

    private static final Pattern DATE =
            Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");

    public record Row(
            int line,
            String bankId,
            String nbfcId,
            String loanId,
            LocalDate reportingDate,
            BigDecimal amountLent,
            BigDecimal interestAmount,
            BigDecimal totalPayable,
            BigDecimal amountRepaid,
            BigDecimal outstandingAmount,
            LocalDate revenuePeriodStart,
            LocalDate revenuePeriodEnd,
            BigDecimal revenueAmount,
            String currency,
            String unit) {
    }

    public record Issue(int line, String message) {
    }

    public static final class InvalidCsv extends RuntimeException {

        public final List<Issue> issues;

        public InvalidCsv(List<Issue> issues) {
            super("Report validation failed. Check the listed CSV rows.");
            this.issues = List.copyOf(issues);
        }
    }

    public List<Row> validate(byte[] bytes) {
        if (bytes.length == 0) {
            throw invalid(1, "The CSV file is empty.");
        }

        if (bytes.length > MAX_BYTES) {
            throw invalid(1, "Maximum file size is 2 MB.");
        }

        String csv;

        try {
            csv = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw invalid(1, "Save the CSV file using UTF-8 encoding.");
        }

        if (csv.startsWith("\uFEFF")) {
            csv = csv.substring(1);
        }

        String[] lines = csv.split("\\R", -1);

        if (!lines[0].equals(HEADER)) {
            throw invalid(1, "Expected header: " + HEADER);
        }

        List<Row> rows = new ArrayList<>();
        List<Issue> issues = new ArrayList<>();
        int recordCount = 0;

        for (int index = 1; index < lines.length; index++) {
            if (lines[index].isBlank()) {
                continue;
            }

            int lineNumber = index + 1;
            recordCount++;

            if (recordCount > MAX_ROWS) {
                throw invalid(
                        lineNumber,
                        "Maximum 10,000 loan records per file."
                );
            }

            if (issues.size() >= MAX_ISSUES) {
                continue;
            }

            try {
                rows.add(parseRow(lines[index], lineNumber));
            } catch (IllegalArgumentException exception) {
                issues.add(new Issue(lineNumber, exception.getMessage()));
            }
        }

        if (!issues.isEmpty()) {
            throw new InvalidCsv(issues);
        }

        if (rows.isEmpty()) {
            throw invalid(2, "Include at least one loan record.");
        }

        return List.copyOf(rows);
    }

    private Row parseRow(String line, int lineNumber) {
        String[] fields = line.split(",", -1);

        require(
                fields.length == 14,
                "Expected 14 columns. Use the lending report sample."
        );

        for (int index = 0; index < fields.length; index++) {
            require(
                    !fields[index].contains("\""),
                    "Use unquoted values, as shown in the sample CSV."
            );
            fields[index] = fields[index].trim();
        }

        String bankId = identifier(fields[0], "bank_id", 44);
        String nbfcId = identifier(fields[1], "nbfc_id", 44);
        String loanId = identifier(fields[2], "loan_id", 64);

        LocalDate reportingDate = date(fields[3], "reporting_date");

        BigDecimal amountLent = money(fields[4], "amount_lent");
        BigDecimal interestAmount = money(fields[5], "interest_amount");
        BigDecimal totalPayable = money(fields[6], "total_payable");
        BigDecimal amountRepaid = money(fields[7], "amount_repaid");
        BigDecimal outstandingAmount = money(
                fields[8], "outstanding_amount"
        );

        LocalDate revenuePeriodStart = date(
                fields[9], "revenue_period_start"
        );
        LocalDate revenuePeriodEnd = date(
                fields[10], "revenue_period_end"
        );

        BigDecimal revenueAmount = money(fields[11], "revenue_amount");

        String currency = fields[12];
        String unit = fields[13];

        require(
                amountLent.signum() > 0,
                "amount_lent must be greater than zero."
        );

        require(
                totalPayable.compareTo(
                        amountLent.add(interestAmount)
                ) == 0,
                "total_payable must equal amount_lent + interest_amount."
        );

        require(
                amountRepaid.compareTo(totalPayable) <= 0,
                "amount_repaid cannot exceed total_payable."
        );

        require(
                outstandingAmount.compareTo(
                        totalPayable.subtract(amountRepaid)
                ) == 0,
                "outstanding_amount must equal total_payable - amount_repaid."
        );

        require(
                !revenuePeriodStart.isAfter(revenuePeriodEnd),
                "revenue_period_start cannot be after revenue_period_end."
        );

        require(
                !revenuePeriodEnd.isAfter(reportingDate),
                "revenue_period_end cannot be after reporting_date."
        );

        require(
                "INR".equals(currency),
                "currency must be INR."
        );

        require(
                "RUPEE".equals(unit),
                "unit must be RUPEE."
        );

        return new Row(
                lineNumber,
                bankId,
                nbfcId,
                loanId,
                reportingDate,
                amountLent,
                interestAmount,
                totalPayable,
                amountRepaid,
                outstandingAmount,
                revenuePeriodStart,
                revenuePeriodEnd,
                revenueAmount,
                currency,
                unit
        );
    }

    private static String identifier(
            String value,
            String field,
            int maximumLength) {

        require(
                value.length() <= maximumLength
                        && SYNTHETIC_ID.matcher(value).matches(),
                field + " must be a SYN- ID using uppercase letters, "
                        + "digits or hyphens, with at most "
                        + maximumLength + " characters."
        );

        return value;
    }

    private static BigDecimal money(String value, String field) {
        require(
                MONEY.matcher(value).matches(),
                field + " must be a nonnegative rupee amount with "
                        + "at most 16 integer digits and 2 decimal places."
        );

        return new BigDecimal(value);
    }

    private static LocalDate date(String value, String field) {
        require(
                DATE.matcher(value).matches(),
                field + " must use YYYY-MM-DD."
        );

        try {
            LocalDate parsed = LocalDate.parse(value);

            require(
                    parsed.getYear() >= 1,
                    field + " must have a year between 0001 and 9999."
            );

            return parsed;
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    field + " must contain a valid calendar date."
            );
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static InvalidCsv invalid(int line, String message) {
        return new InvalidCsv(List.of(new Issue(line, message)));
    }
}