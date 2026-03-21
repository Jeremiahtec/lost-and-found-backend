package com.sqi.lostandfound;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing  // Required for @CreatedDate to auto-populate timestamp
public class LostAndFoundApplication {

	public static void main(String[] args) {
		SpringApplication.run(LostAndFoundApplication.class, args);
	}
}