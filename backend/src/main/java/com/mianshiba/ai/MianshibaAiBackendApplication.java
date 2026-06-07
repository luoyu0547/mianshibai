package com.mianshiba.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MianshibaAiBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MianshibaAiBackendApplication.class, args);
	}

}
