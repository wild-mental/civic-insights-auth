package com.makersworld.civic_insights_auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CivicInsightsAuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(CivicInsightsAuthApplication.class, args);
	}

}
