
package com.documind.api.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        String tokenType,
        UUID userId,
        String name,
        String email
) {
}
