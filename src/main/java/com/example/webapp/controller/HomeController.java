package com.example.webapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Welcome to Spring Boot Web Application!";
    }

    @GetMapping("/api/hello")
    public String sayHello() {
        return "Hello from Spring Boot!";
    }

}
