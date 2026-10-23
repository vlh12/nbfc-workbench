package com.nbfc.accessdata.data.portfolio;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
class PortfolioCsvTest {
    private final PortfolioCsv validator=new PortfolioCsv();
    private byte[] sample()throws Exception{return getClass().getResourceAsStream("/portfolio-16.csv").readAllBytes();}
    @Test void suppliedFilePreservesAllSixteenLoansAcrossEightNbfcs()throws Exception{
        var result=validator.validate(sample());
        assertThat(result.errors()).isEmpty();assertThat(result.rows()).hasSize(16);
        assertThat(result.rows().stream().map(r->r.nbfcId()).distinct()).hasSize(8);
        assertThat(result.rows().get(2).outstandingAmount()).isEqualTo("150000000.00");
        assertThat(result.warnings()).anyMatch(w->w.field().equals("outstanding_amount"));
        assertThat(result.rows()).allMatch(r->r.eadAmount()==null);
    }
    @Test void explicitEadIsNotDerivedFromOutstanding()throws Exception{
        String first=new String(sample(),StandardCharsets.UTF_8).split("\\R")[1];
        var result=validator.validate((PortfolioCsv.HEADER+",ead_amount\n"+first+",123456789.12\n").getBytes(StandardCharsets.UTF_8));
        assertThat(result.errors()).isEmpty();assertThat(result.warnings()).isEmpty();
        assertThat(result.rows().getFirst().eadAmount()).isEqualTo("123456789.12");
    }
    @Test void duplicateAndCrossBankFilesAreRejectedAtomically()throws Exception{
        String first=new String(sample(),StandardCharsets.UTF_8).split("\\R")[1];
        var duplicate=validator.validate((PortfolioCsv.HEADER+"\n"+first+"\n"+first).getBytes(StandardCharsets.UTF_8));
        assertThat(duplicate.errors()).anyMatch(i->i.field().equals("loan_id"));assertThat(duplicate.rows()).isEmpty();
        var other=validator.validate((PortfolioCsv.HEADER+"\n"+first.replace("SYN-BANK-001","SYN-BANK-002")).getBytes(StandardCharsets.UTF_8));
        assertThat(other.errors()).anyMatch(i->i.field().equals("bank_id"));
    }
    @Test void invalidUtf8AndMissingEadAreNeverCoerced()throws Exception{
        assertThat(validator.validate(new byte[]{(byte)0xff}).errors()).isNotEmpty();
        String first=new String(sample(),StandardCharsets.UTF_8).split("\\R")[1];
        assertThat(validator.validate((PortfolioCsv.HEADER+",ead_amount\n"+first+",").getBytes(StandardCharsets.UTF_8)).errors()).anyMatch(i->i.field().equals("ead_amount"));
    }
}
