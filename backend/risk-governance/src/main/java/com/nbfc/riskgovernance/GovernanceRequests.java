package com.nbfc.riskgovernance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
public final class GovernanceRequests {
    private GovernanceRequests() {}
    public record SubmitAssessment(
        @NotBlank @Size(max=160) String title,
        @NotBlank @Size(max=2000) String summary) {}
    public record ResubmitAssessment(@NotBlank @Size(max=2000) String summary) {}
    public record Decision(@NotBlank @Size(max=2000) String note) {}
    public record CreateAction(
        @NotBlank @Size(max=160) String title,
        @NotBlank @Size(max=160) String ownerName,
        LocalDate dueDate) {}
    public record UpdateAction(@NotNull ActionStatus status) {}
}

