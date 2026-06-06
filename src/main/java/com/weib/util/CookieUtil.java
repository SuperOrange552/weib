package com.weib.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtil {

    public static final int REMEMBER_TOKEN_MAX_AGE = 7200; // 2 hours

    public static void addRememberTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("remember_token", token);
        cookie.setMaxAge(REMEMBER_TOKEN_MAX_AGE);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    public static void deleteRememberTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("remember_token", "");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    public static void addJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("jwt_token", token);
        cookie.setMaxAge(REMEMBER_TOKEN_MAX_AGE);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    public static void deleteJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt_token", "");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }
}
