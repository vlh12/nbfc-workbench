package com.nbfc.accessdata.data;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class CsvValidatorTest {
    private final CsvValidator validator=new CsvValidator();
    private byte[] csv(String rows) { return ("bank_id,nbfc_id,exposure_amount\n"+rows).getBytes(StandardCharsets.UTF_8); }
    @Test void acceptsSyntheticDecimalValues() {
        var rows=validator.validate(csv("SYN-BANK-1,SYN-NBFC-1,10.10\nSYN-BANK-2,SYN-NBFC-1,0.00\n"));
        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst().exposureAmount()).isEqualByComparingTo("10.10");
    }
    @Test void rejectsWholeFileWhenOneRowIsInvalid() {
        
        assertThatThrownBy(() -> validator.validate(csv("SYN-BANK-1,SYN-NBFC-1,10.10\nREAL-BANK,SYN-NBFC-1,20\n")))
            .isInstanceOf(CsvValidator.InvalidCsv.class)
            .satisfies(e -> assertThat(((CsvValidator.InvalidCsv)e).issues.getFirst().line()).isEqualTo(3));
    }
    @Test void rejectsNegativeAndScientificAmounts() {
        for(String amount:new String[]{"-1.00","1e5","0.001","10000000000000000"}) {
            assertThatThrownBy(() -> validator.validate(csv("SYN-BANK-1,SYN-NBFC-1,"+amount+"\n")))
                .isInstanceOf(CsvValidator.InvalidCsv.class);
        }
    }
    @Test void rejectsEmptyAndMalformedUtf8Files() {
        assertThatThrownBy(() -> validator.validate(csv(""))).isInstanceOf(CsvValidator.InvalidCsv.class);
        assertThatThrownBy(() -> validator.validate(new byte[]{(byte)0xc3,(byte)0x28})).isInstanceOf(CsvValidator.InvalidCsv.class);
    }
    @Test void ignoresBlankLinesIncludingWindowsTrailingLines() {
        var rows=validator.validate(csv("SYN-BANK-1,SYN-NBFC-1,10.10\r\n\r\n \t\r\nSYN-BANK-2,SYN-NBFC-2,20.20\r\n\r\n"));
        assertThat(rows).hasSize(2);
        assertThat(rows.getLast().exposureAmount()).isEqualByComparingTo("20.20");
    }
    @Test void blankLinesDoNotHideMalformedRecordsOrChangeLineNumbers() {
        assertThatThrownBy(() -> validator.validate(csv("SYN-BANK-1,SYN-NBFC-1,10.10\n\nSYN-BANK-2\n\n")))
            .isInstanceOf(CsvValidator.InvalidCsv.class)
            .satisfies(e -> assertThat(((CsvValidator.InvalidCsv)e).issues.getFirst().line()).isEqualTo(4));
    }
}
