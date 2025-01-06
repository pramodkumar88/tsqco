package com.tsqco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TsqcoApplication {

	public static void main(String[] args) {
		SpringApplication.run(TsqcoApplication.class, args);
	}

}
