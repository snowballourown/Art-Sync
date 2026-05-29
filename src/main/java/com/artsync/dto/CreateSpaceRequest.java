package com.artsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSpaceRequest(
        @NotBlank(message = "공간 이름은 필수입니다.")
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description
) {}
