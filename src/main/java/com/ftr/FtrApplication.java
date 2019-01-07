package com.ftr;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.ftr.dao")
public class FtrApplication {

	public static void main(String[] args) {
		SpringApplication.run(FtrApplication.class, args);
	}
}
