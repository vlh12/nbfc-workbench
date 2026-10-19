package com.nbfc.exposureanalytics;
import com.nbfc.shared.ExposureEvent.Row;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity
@Table(name="exposure_record")
public class ExposureRecord {
    @Id @Column(length=50) public String id;
    @Column(name="batch_id",nullable=false,length=36) public String batchId;
    @Column(name="bank_id",nullable=false,length=44) public String bankId;
    @Column(name="nbfc_id",nullable=false,length=44) public String nbfcId;
    @Column(name="exposure_amount",nullable=false,precision=18,scale=2) public BigDecimal exposureAmount;
    protected ExposureRecord() {}
    public ExposureRecord(String id,String batchId,Row row) {
        this.id=id; this.batchId=batchId; bankId=row.bankId(); nbfcId=row.nbfcId(); exposureAmount=row.exposureAmount();
    }
}

