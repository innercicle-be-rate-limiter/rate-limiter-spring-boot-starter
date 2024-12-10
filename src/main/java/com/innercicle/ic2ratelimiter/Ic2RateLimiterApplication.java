package com.innercicle.ic2ratelimiter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Ic2RateLimiterApplication {

	public static void main(String[] args) {
		SpringApplication.run(Ic2RateLimiterApplication.class, args);
	}

}
