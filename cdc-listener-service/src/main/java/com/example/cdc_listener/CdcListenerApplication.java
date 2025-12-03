package com.example.cdc_listener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CdcListenerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CdcListenerApplication.class, args);
	}

}
