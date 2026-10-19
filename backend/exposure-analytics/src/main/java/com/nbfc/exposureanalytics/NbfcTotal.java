package com.nbfc.exposureanalytics;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nbfc.shared.DecimalStringSerializer;
public record NbfcTotal(String nbfcId,@JsonSerialize(using=DecimalStringSerializer.class) BigDecimal amount,long records) {}
