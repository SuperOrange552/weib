package com.weib.controller;
import org.springframework.stereotype.Controller; import org.springframework.web.bind.annotation.GetMapping;
@Controller public class ForumPageController { @GetMapping("/forum") public String forum(){return "forum";} @GetMapping("/forum/post/new") public String compose(){return "forum-compose";} @GetMapping("/forum/post/{id}") public String detail(){return "forum-detail";} }
