package com.musicmatch.musicmatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = "com.musicmatch")
@EnableMongoRepositories(basePackages = "com.musicmatch.repository")
public class MusicmatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(MusicmatchApplication.class, args);
	}

}
