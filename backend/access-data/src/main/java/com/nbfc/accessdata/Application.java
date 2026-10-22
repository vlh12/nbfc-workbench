package com.nbfc.accessdata;
import com.nbfc.shared.ApiSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@Import(ApiSecurity.class)
@EnableScheduling
public class Application {
	public static void main(String[] args) {
	    System.out.println("AUTH_CHECK: Access & Data main started");
	    System.out.println(
	    	    "AUTH_CHECK kafkaSkipArgument=" +
	    	    java.util.Arrays.asList(args).contains(
	    	        "--spring.kafka.admin.auto-create=false"
	    	    )
	    	);
	    var context = SpringApplication.run(Application.class, args);

	    System.out.println(
	        "AUTH_CHECK enabled=" +
	        context.getEnvironment().getProperty(
	            "workbench.bootstrap.enabled",
	            "<missing>"
	        )
	    );

	    System.out.println(
	        "AUTH_CHECK configLoaded=" +
	        context.containsBean("accountSetupConfiguration")
	    );

	    System.out.println(
	        "AUTH_CHECK runnerLoaded=" +
	        context.containsBean("initialSupervisorSetup")
	    );
	}
}

