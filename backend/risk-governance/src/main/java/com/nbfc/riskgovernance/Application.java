package com.nbfc.riskgovernance;
import com.nbfc.shared.ApiSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Import(ApiSecurity.class)
@EnableScheduling

public class Application {
    public static void main(String[] args) { SpringApplication.run(Application.class, args); }
}
