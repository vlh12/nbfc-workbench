package com.nbfc.accessdata.data;
import com.nbfc.shared.ExposureEvent.Row;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
@Component
public class CsvValidator {
    public record Issue(int line, String message) {}
    public static class InvalidCsv extends RuntimeException {
        public final List<Issue> issues;
        public InvalidCsv(List<Issue> issues) { super("Upload rejected; no records saved."); this.issues = List.copyOf(issues); }
    }
    public List<Row> validate(byte[] bytes) {
        String csv;
        try {
            csv = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) { throw invalid(1, "Use a UTF-8 CSV file."); }
        csv = csv.replaceFirst("^\\uFEFF", "");
        String[] lines = csv.split("\\R", -1);
        if (!lines[0].equals("bank_id,nbfc_id,exposure_amount"))
            throw invalid(1, "Expected header: bank_id,nbfc_id,exposure_amount");
        var rows = new ArrayList<Row>();
        var issues = new ArrayList<Issue>();
        int recordCount = 0;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) continue;
            if (++recordCount > 10000) throw invalid(i + 1, "Maximum 10,000 records per file.");
            String[] fields = lines[i].split(",", -1);
            if (fields.length != 3) { issues.add(new Issue(i + 1, "Expected three unquoted fields.")); }
            else {
                String bank = fields[0].trim(), nbfc = fields[1].trim(), amount = fields[2].trim();
                if (!bank.matches("SYN-[A-Z0-9-]{1,40}") || !nbfc.matches("SYN-[A-Z0-9-]{1,40}"))
                    issues.add(new Issue(i + 1, "Use synthetic IDs: SYN- followed by uppercase letters, digits or hyphens."));
                else if (!amount.matches("[0-9]{1,16}(\\.[0-9]{1,2})?"))
                    issues.add(new Issue(i + 1, "Amount must be nonnegative with at most 16 integer digits and 2 decimal places."));
                else rows.add(new Row(bank, nbfc, new BigDecimal(amount)));
            }
            if (issues.size() >= 100) break;
        }
        if (!issues.isEmpty()) throw new InvalidCsv(issues);
        if (rows.isEmpty()) throw invalid(2, "Include at least one exposure record.");
        return List.copyOf(rows);
    }
    private InvalidCsv invalid(int line, String message) { return new InvalidCsv(List.of(new Issue(line, message))); }
}
