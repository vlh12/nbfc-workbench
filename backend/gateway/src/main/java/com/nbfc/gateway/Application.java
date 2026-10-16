package com.nbfc.gateway;
import com.nbfc.shared.ApiSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
@SpringBootApplication
@Import(ApiSecurity.class)
public class Application {
	public static void main(String[] args) {
	    var applicationContext =
	        SpringApplication.run(Application.class, args);

	    System.out.println(
	        "GATEWAY_DEMO_ENABLED=" +
	        applicationContext.getEnvironment().getProperty(
	            "WORKBENCH_DEMO_ENABLED",
	            "false"
	        )
	    );
	}
}

