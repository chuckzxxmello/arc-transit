package com.transit.arctransit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ArcTransitSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArcTransitSystemApplication.class, args);
	}

}
