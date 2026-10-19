package com.nbfc.shared;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
@Configuration
@EnableMethodSecurity
public class ApiSecurity {
    @Bean SecurityFilterChain security(HttpSecurity http,
            @Value("${WORKBENCH_DEMO_ENABLED:false}") boolean demoEnabled) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint((request,response,error) -> response.sendError(401)))
            .authorizeHttpRequests(a -> {
                a.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                a.requestMatchers("/actuator/health").permitAll();
                if (demoEnabled) a.anyRequest().authenticated();
                else a.anyRequest().denyAll();
            });
        if (demoEnabled) http.addFilterBefore(new DemoRoleFilter(),AnonymousAuthenticationFilter.class);
        return http.build();
    }
    private static class DemoRoleFilter extends OncePerRequestFilter {
        private static final Set<String> ROLES=Set.of("CONTRIBUTOR","ANALYST","SUPERVISOR");
        @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,
                FilterChain chain) throws ServletException,IOException {
            String role=request.getHeader("X-Demo-Role");
            if(role != null) {
                if(!ROLES.contains(role)) { response.sendError(401,"Unknown demo role."); return; }
                var context=SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new UsernamePasswordAuthenticationToken(
                    "synthetic-"+role.toLowerCase(java.util.Locale.ROOT),null,
                    List.of(new SimpleGrantedAuthority("ROLE_"+role))));
                SecurityContextHolder.setContext(context);
            }
            chain.doFilter(request,response);
        }
    }
}
