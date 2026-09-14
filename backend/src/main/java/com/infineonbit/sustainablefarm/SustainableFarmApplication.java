package com.infineonbit.sustainablefarm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SustainableFarmApplication {

	public static void main(String[] args) {
		SpringApplication.run(SustainableFarmApplication.class, args);
	}

}
