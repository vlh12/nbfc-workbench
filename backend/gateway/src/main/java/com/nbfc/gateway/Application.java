package com.nbfc.gateway;

import com.nbfc.shared.ApiSecurity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootApplication(exclude = SessionAutoConfiguration.class)
@Import(ApiSecurity.class)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}