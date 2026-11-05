// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Wetter- & Routen-Plattform
package de.warp.bff.infrastructure.client.dto;

public record InternalChangePasswordRequest(
        String currentPassword,
        String newPassword
) {
}