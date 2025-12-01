package com.example.cdc_producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CdcProducerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CdcProducerApplication.class, args);
	}

}
