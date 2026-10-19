package com.nbfc.shared;
import java.time.Instant;
public final class StressData {
    private StressData(){}
    public record Request(String snapshotId,String nbfcId,String capitalId,String qualityId,
        String thresholdId,String severityPct,String recoveryPct,String rationale,String idempotencyKey){}
    public record Result(String eadAmount,String eligibleCapital,String capitalLabel,String severityPct,
        String recoveryPct,String stressedLoss,String capitalImpactPct,String concentrationCapitalPct,
        String alertSeverity,String thresholdVersion,String mediumPct,String highPct,String formula){}
    public record Run(String id,String bankId,String snapshotId,String nbfcId,String status,String createdBy,
        Instant createdAt,String modelVersion,Request inputs,Result result,String failureReason){}
}
