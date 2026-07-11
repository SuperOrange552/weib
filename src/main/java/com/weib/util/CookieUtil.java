package com.weib.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtil {

    public static final int REMEMBER_TOKEN_MAX_AGE = 7200; // 2 hours

    public static void addRememberTokenCookie(HttpServletResponse response, String token) {
        addRememberTokenCookie(response, token, true);
    }

    public static void addRememberTokenCookie(HttpServletResponse response, String token, HttpServletRequest request) {
        addRememberTokenCookie(response, token, isSecureRequest(request));
    }

    private static void addRememberTokenCookie(HttpServletResponse response, String token, boolean secure) {
        Cookie cookie = new Cookie("remember_token", token);
        cookie.setMaxAge(REMEMBER_TOKEN_MAX_AGE);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    public static void deleteRememberTokenCookie(HttpServletResponse response) {
        deleteRememberTokenCookie(response, true);
    }

    public static void deleteRememberTokenCookie(HttpServletResponse response, HttpServletRequest request) {
        deleteRememberTokenCookie(response, isSecureRequest(request));
    }

    private static void deleteRememberTokenCookie(HttpServletResponse response, boolean secure) {
        Cookie cookie = new Cookie("remember_token", "");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    public static void addJwtCookie(HttpServletResponse response, String token) {
        addJwtCookie(response, token, true);
    }

    public static void addJwtCookie(HttpServletResponse response, String token, HttpServletRequest request) {
        addJwtCookie(response, token, isSecureRequest(request));
    }

    private static void addJwtCookie(HttpServletResponse response, String token, boolean secure) {
        Cookie cookie = new Cookie("jwt_token", token);
        cookie.setMaxAge(REMEMBER_TOKEN_MAX_AGE);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    public static void deleteJwtCookie(HttpServletResponse response) {
        deleteJwtCookie(response, true);
    }

    public static void deleteJwtCookie(HttpServletResponse response, HttpServletRequest request) {
        deleteJwtCookie(response, isSecureRequest(request));
    }

    private static void deleteJwtCookie(HttpServletResponse response, boolean secure) {
        Cookie cookie = new Cookie("jwt_token", "");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    public static boolean isSecureRequest(HttpServletRequest request) {
        if (request == null) return false;
        if (request.isSecure()) return true;
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return forwardedProto != null && forwardedProto.trim().equalsIgnoreCase("https");
    }
}
