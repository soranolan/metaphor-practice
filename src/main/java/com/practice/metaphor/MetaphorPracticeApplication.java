package com.practice.metaphor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.practice.metaphor.*.mapper")
@EnableScheduling
public class MetaphorPracticeApplication {

	public static void main(String[] args) {
		SpringApplication.run(MetaphorPracticeApplication.class, args);
	}

}
