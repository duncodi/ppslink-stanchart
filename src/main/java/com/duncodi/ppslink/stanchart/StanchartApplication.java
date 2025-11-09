package com.duncodi.ppslink.stanchart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class StanchartApplication {

	public static void main(String[] args) {
		SpringApplication.run(StanchartApplication.class, args);
	}

}
