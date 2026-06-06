package com.weib;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WeibApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeibApplication.class, args);
        System.out.println("微招启动成功！http://localhost:8888/");
    }
}
