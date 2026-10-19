package com.nbfc.stresssimulation;
import com.nbfc.shared.ApiSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(ApiSecurity.class)

public class Application {
    public static void main(String[] args) { SpringApplication.run(Application.class, args); }
}

