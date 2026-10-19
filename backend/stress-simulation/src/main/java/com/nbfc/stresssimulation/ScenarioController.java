package com.nbfc.stresssimulation;
import com.nbfc.shared.StressData.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import java.util.*;
@RestController @RequestMapping("/api/scenarios")
public class ScenarioController {
    private final ScenarioService service;
    public ScenarioController(ScenarioService service){this.service=service;}
    @PostMapping @PreAuthorize("hasRole('ANALYST')")
    public Run run(@RequestBody Request body,Authentication actor){return service.execute(body,actor.getName());}
    @GetMapping @PreAuthorize("hasAnyRole('ANALYST','SUPERVISOR')")
    public List<Run> list(){return service.list();}
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ANALYST','SUPERVISOR')")
    public Run get(@PathVariable String id){return service.get(id);}
}
