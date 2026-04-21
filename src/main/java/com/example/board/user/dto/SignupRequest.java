package com.example.board.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        String username,

        @NotBlank
        @Size(min = 6, max = 100)
        String password,

        @NotBlank
        @Size(max = 30)
        String nickname
) {
}
