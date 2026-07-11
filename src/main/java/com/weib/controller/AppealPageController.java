package com.weib.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AppealPageController {
    @GetMapping("/appeal")
    public String page() { return "appeal"; }
}