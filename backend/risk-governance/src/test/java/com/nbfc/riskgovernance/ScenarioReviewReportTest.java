package com.nbfc.riskgovernance;

import com.nbfc.shared.ApiSecurity;
import com.nbfc.shared.auth.AccountSessionVerifier;
import com.nbfc.shared.auth.WorkbenchPrincipal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers=ScenarioReviewReportController.class,properties="WORKBENCH_DEMO_ENABLED=true")
@Import(ApiSecurity.class)
class ScenarioReviewReportTest {
    static final String RUN="e34494b4-b18b-41af-ada5-2a4718fe51b3";
    @Autowired MockMvc mvc;
    @MockitoBean AssessmentRepository assessments;
    @MockitoBean FollowUpRepository followUps;
    @MockitoBean AuditRepository audit;
    @MockitoBean AccountSessionVerifier accountSessions;
    @BeforeEach void currentAccount(){when(accountSessions.isCurrent(any(WorkbenchPrincipal.class))).thenReturn(true);}
    static RequestPostProcessor role(String role) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(new WorkbenchPrincipal(
                "00000000-0000-0000-0000-000000000042","test-user",role,0L),null,List.of(new SimpleGrantedAuthority("ROLE_"+role))));
    }
    @Test void supervisorGetsOnlyThisScenariosHistoryWithoutRawEvidence() throws Exception {
        var item=new Assessment("assessment-a","portfolio-a","analyst-a","Review the synthetic assumptions.");
        item.scenarioId=RUN;item.status="APPROVED";item.decidedBy="supervisor-a";
        item.decidedAt=Instant.parse("2026-10-22T10:10:00Z");item.scenarioEvidence="large raw JSON must not be returned";
        var action=new FollowUpAction("action-a",item.id,"Review inputs","owner-a",LocalDate.of(2026,11,1),"supervisor-a");
        var approved=new AuditEvent("event-2","ASSESSMENT",item.id,"APPROVED","supervisor-a","Reviewed");approved.createdAt=item.decidedAt;
        var submitted=new AuditEvent("event-1","ASSESSMENT",item.id,"SCENARIO_SUBMITTED","analyst-a",RUN);submitted.createdAt=Instant.parse("2026-10-22T10:00:00Z");
        when(assessments.findByScenarioIdOrderBySubmittedAtAscIdAsc(RUN)).thenReturn(List.of(item));
        when(followUps.findByAssessmentIdInOrderByDueDateAscIdAsc(List.of(item.id))).thenReturn(List.of(action));
        when(audit.findByEntityTypeAndEntityIdInOrderByCreatedAtAscIdAsc("ASSESSMENT",List.of(item.id))).thenReturn(List.of(approved,submitted));
        when(audit.findByEntityTypeAndEntityIdInOrderByCreatedAtAscIdAsc("FOLLOW_UP",List.of(action.id))).thenReturn(List.of());
        mvc.perform(get("/api/governance/scenarios/"+RUN+"/review-history").with(role("SUPERVISOR")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.scenarioId").value(RUN))
            .andExpect(jsonPath("$.assessments[0].status").value("APPROVED"))
            .andExpect(jsonPath("$.assessments[0].scenarioEvidence").doesNotExist())
            .andExpect(jsonPath("$.followUps[0].title").value("Review inputs"))
            .andExpect(jsonPath("$.events[0].action").value("SCENARIO_SUBMITTED"));
        verify(assessments,never()).findTop100ByOrderBySubmittedAtDesc();
    }
    @Test void emptyHistoryIsNotAnApproval() throws Exception {
        when(assessments.findByScenarioIdOrderBySubmittedAtAscIdAsc(RUN)).thenReturn(List.of());
        mvc.perform(get("/api/governance/scenarios/"+RUN+"/review-history").with(role("SUPERVISOR")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.assessments").isEmpty()).andExpect(jsonPath("$.events").isEmpty());
        verifyNoInteractions(followUps);verify(audit,never()).findTop200ByOrderByCreatedAtDesc();
    }
    @Test void analystCannotReadSupervisorHistory() throws Exception {
        mvc.perform(get("/api/governance/scenarios/"+RUN+"/review-history").with(role("ANALYST"))).andExpect(status().isForbidden());
        verifyNoInteractions(assessments,followUps);
    }
    @Test void contributorCannotReadSupervisorHistory() throws Exception {
        mvc.perform(get("/api/governance/scenarios/"+RUN+"/review-history").with(role("CONTRIBUTOR"))).andExpect(status().isForbidden());
        verifyNoInteractions(assessments,followUps);
    }
    @Test void invalidReferenceIsRejectedBeforeQueries() throws Exception {
        mvc.perform(get("/api/governance/scenarios/not-a-run/review-history").with(role("SUPERVISOR"))).andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(assessments,followUps);
    }
}
