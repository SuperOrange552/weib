package com.weib.identity;

public record ActiveIdentity(Long userId, String role, String nickname, String avatar) { }
