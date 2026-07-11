package com.weib.dto.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MobileLoginRequest(
        @NotBlank @Size(max = 32) String username,
        @NotBlank @Size(max = 64) String password,
        @NotBlank @Size(max = 8) String captcha,
        @Size(max = 20) String selectedRole
) {
}
