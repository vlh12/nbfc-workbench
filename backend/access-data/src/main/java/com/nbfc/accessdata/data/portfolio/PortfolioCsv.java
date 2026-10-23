package com.nbfc.accessdata.data.portfolio;

import com.nbfc.shared.PortfolioData.*;
import com.nbfc.shared.PortfolioData;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.time.LocalDate;
import java.util.*;

@Component
public class PortfolioCsv {
    public static final String HEADER = "bank_id,nbfc_id,loan_id,reporting_date,instrument_type,amount_lent,interest_amount,total_payable,amount_repaid,outstanding_amount,dpd_days,loan_status,maturity_date,collateral_type,revenue_period_start,revenue_period_end,revenue_amount,currency,unit";
    public Validation validate(byte[] bytes) {
        if (bytes.length == 0 || bytes.length > 2 * 1024 * 1024)
            return invalid(1, "file", "Upload a nonempty UTF-8 file no larger than 2 MB.");
        String csv;
        try { csv = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString(); }
        catch (CharacterCodingException e) { return invalid(1, "file", "Use UTF-8 encoding."); }
        if (csv.startsWith("\uFEFF")) csv = csv.substring(1);
        String[] lines = csv.split("\\R", -1);
        boolean ead = lines[0].equals(HEADER + ",ead_amount");
        if (!ead && !lines[0].equals(HEADER)) return invalid(1, "header",
            "Expected the 19-column portfolio header, optionally followed by ead_amount. Download the portfolio template.");
        List<Loan> rows = new ArrayList<>(); List<Issue> warnings = new ArrayList<>(); List<Issue> errors = new ArrayList<>();
        Set<String> keys = new HashSet<>(); LocalDate period = null; int count = 0;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) continue;
            if (++count > 10000) { errors.add(new Issue(i+1,"file","Maximum 10,000 records.")); break; }
            if (errors.size() >= 100) continue;
            String field = "row";
            try {
                String[] v = lines[i].split(",", -1);
                require(v.length == (ead ? 20 : 19), "Incorrect number of fields.");
                for (int k=0;k<v.length;k++) { require(!v[k].contains("\""), "Use unquoted fields."); v[k]=v[k].trim(); }
                for(int k=0;k<3;k++) { field = new String[]{"bank_id","nbfc_id","loan_id"}[k]; require(v[k].matches("SYN-[A-Z0-9-]{1,40}"), "Use a synthetic SYN- identifier (maximum 44 characters)."); }
                field="nbfc_id"; require(v[1].startsWith("SYN-NBFC-"),"Counterparty must be a synthetic NBFC.");
                field="loan_id"; require(v[2].startsWith("SYN-LOAN-"),"Use a synthetic loan ID.");
                field="bank_id"; require(PortfolioData.BANK.equals(v[0]), "This demo is scoped to SYN-BANK-001.");
                require(!v[0].equals(v[1]), "Self exposure is not allowed.");
                field="reporting_date"; LocalDate date=date(v[3]); require(period==null || period.equals(date), "All rows must have one reporting date."); period=date;
                field="loan_id"; require(keys.add(v[0]+"/"+v[2]), "Duplicate loan ID in the bank snapshot.");
                String[] names={"amount_lent","interest_amount","total_payable","amount_repaid","outstanding_amount"};
                for(int k=5;k<=9;k++){field=names[k-5];money(v[k]);}
                field="amount_lent"; require(money(v[5]).signum()>0, "Amount lent must be positive.");
                field="total_payable"; require(money(v[7]).compareTo(money(v[5]).add(money(v[6])))==0, "Total payable must equal amount lent plus recorded interest.");
                field="amount_repaid"; require(money(v[8]).compareTo(money(v[7]))<=0,"Repaid cannot exceed total payable.");
                if(money(v[9]).compareTo(money(v[7]).subtract(money(v[8])))!=0)
                    warnings.add(new Issue(i+1,"outstanding_amount","Reported outstanding differs from total payable minus repaid. Value preserved; correct before stress approval."));
                field="dpd_days"; require(v[10].matches("[0-9]{1,6}"), "DPD must be a nonnegative whole number.");
                field="loan_status"; require(Set.of("CURRENT","SMA_0","SMA_1","SMA_2","NPA","RESTRUCTURED","CURED","WRITTEN_OFF","RECOVERY").contains(v[11]),"Unsupported loan status.");
                field="instrument_type"; require(v[4].matches("[A-Z_]{1,40}"), "Use an uppercase instrument type.");
                field="collateral_type"; require(v[13].matches("[A-Z_]{1,40}"), "Use an uppercase collateral type.");
                field="maturity_date"; LocalDate maturity=date(v[12]);
                field="revenue_period_start"; LocalDate start=date(v[14]);
                field="revenue_period_end"; LocalDate end=date(v[15]);
                require(!start.isAfter(end)&&!end.isAfter(date),"Revenue period must end on or before reporting date.");
                field="revenue_amount"; money(v[16]);
                field="currency"; require("INR".equals(v[17]), "Use INR.");
                field="unit"; require("RUPEE".equals(v[18]),"Use RUPEE.");
                String eadAmount=null;
                if(ead){field="ead_amount"; eadAmount=money(v[19]).toPlainString();}
                rows.add(new Loan(v[0],v[1],v[2],date,v[4],v[5],v[6],v[7],v[8],v[9],
                    Integer.parseInt(v[10]),v[11],maturity,v[13],start,end,v[16],v[17],v[18],eadAmount));
            } catch(IllegalArgumentException e){ errors.add(new Issue(i+1,field,e.getMessage())); }
        }
        if(count==0) errors.add(new Issue(2,"file","Include at least one loan."));
        if(!ead) warnings.add(new Issue(1,"ead_amount","No explicit EAD supplied. Review is available; stress calculations are blocked."));
        return new Validation(errors.isEmpty()?List.copyOf(rows):List.of(),List.copyOf(warnings),List.copyOf(errors));
    }
    private Validation invalid(int line,String field,String message){return new Validation(List.of(),List.of(),List.of(new Issue(line,field,message)));}
    static BigDecimal money(String value){require(value!=null && value.matches("[0-9]{1,16}(\\.[0-9]{1,2})?"),"Use a nonnegative INR amount with at most two decimals.");return new BigDecimal(value);}
    static LocalDate date(String value){require(value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"),"Use YYYY-MM-DD.");try { LocalDate d=LocalDate.parse(value);require(d.getYear()>0,"Use a positive year.");return d; } catch(java.time.DateTimeException e){throw new IllegalArgumentException("Use a valid calendar date.");}}
    static void require(boolean valid,String message){if(!valid)throw new IllegalArgumentException(message);}
}
