package com.nbfc.riskgovernance;
import com.nbfc.shared.LocalServiceClient;
import com.nbfc.shared.StressData.Run;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
@io.swagger.v3.oas.annotations.tags.Tag(name="Scenario assessments",description="Submit completed scenario evidence for supervisor review.")
@RestController @RequestMapping("/api/governance")
public class ScenarioAssessmentController {
    private final GovernanceService service; private final String stress;
    public ScenarioAssessmentController(GovernanceService service,@Value("${STRESS_SIMULATION_URL:http://localhost:8083}") String stress){this.service=service;this.stress=stress;}
    public record Submission(String scenarioId,String note){}
    @PostMapping("/scenario-assessments") @PreAuthorize("hasRole('ANALYST')")
    public Assessment submit(@RequestBody Submission body,Authentication actor){
        if(body.scenarioId()==null||!body.scenarioId().matches("[a-f0-9-]{36}"))throw new IllegalArgumentException("Select a completed stress run.");
        Run run=LocalServiceClient.get(stress,"/api/scenarios/"+body.scenarioId(),Run.class);
        return service.submitScenario(run,body.note(),actor.getName());
    }
}
