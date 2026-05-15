package com.yali.mactav.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.yali.mactav")
public class MacTavApplication {

    public static void main(String[] args) {
        SpringApplication.run(MacTavApplication.class, args);
    }
}
