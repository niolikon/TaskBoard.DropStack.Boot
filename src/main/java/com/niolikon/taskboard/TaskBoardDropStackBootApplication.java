package com.niolikon.taskboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableConfigurationProperties
public class TaskBoardDropStackBootApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskBoardDropStackBootApplication.class, args);
	}

}
