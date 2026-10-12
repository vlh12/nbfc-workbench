package com.nbfc.shared;
import java.math.BigDecimal;
import java.util.List;
public record ExposureEvent(String eventId, String batchId, List<Row> rows) {
    public static final String TOPIC = "exposure-batch-validated";
    public record Row(String bankId, String nbfcId, BigDecimal exposureAmount) {}
}

