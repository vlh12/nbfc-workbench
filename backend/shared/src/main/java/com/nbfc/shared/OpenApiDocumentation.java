package com.nbfc.shared;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import java.util.regex.Pattern;

@Configuration(proxyBeanMethods=false)
public class OpenApiDocumentation {
    @Bean OpenAPI workbenchOpenApi(Environment environment) {
        String title=environment.getProperty("workbench.openapi.title","NBFC Workbench API");
        return new OpenAPI().info(new Info().title(title).version("v1")
            .description("Synthetic NBFC workbench. Click Authorize and enter CONTRIBUTOR, ANALYST or SUPERVISOR as X-Demo-Role. These are selectable local demo roles, not production authentication. Try it out executes real API calls; POST/PATCH can save or change records."))
            .servers(List.of(new Server().url("/").description("Current service or API gateway")))
            .components(new Components().addSecuritySchemes("DemoRole",new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-Demo-Role")
                .description("Enter CONTRIBUTOR, ANALYST or SUPERVISOR. Requires WORKBENCH_DEMO_ENABLED=true.")))
            .addSecurityItem(new SecurityRequirement().addList("DemoRole"));
    }
    @Bean OperationCustomizer demoRoleDocumentation() {
        return (operation,handler) -> {
            PreAuthorize rule=AnnotatedElementUtils.findMergedAnnotation(handler.getMethod(),PreAuthorize.class);
            if(rule==null)rule=AnnotatedElementUtils.findMergedAnnotation(handler.getBeanType(),PreAuthorize.class);
            if(rule!=null) {
                var matcher=Pattern.compile("'([A-Z_]+)'").matcher(rule.value());
                var roles=new java.util.ArrayList<String>();while(matcher.find())roles.add(matcher.group(1));
                if(!roles.isEmpty())operation.setDescription((operation.getDescription()==null?"":operation.getDescription()+"\n\n")+"Allowed demo roles: "+String.join(", ",roles)+".");
            }
            return operation;
        };
    }
}
