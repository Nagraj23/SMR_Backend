package com.spring.smr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SmrApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmrApplication.class, args);
	}

}
