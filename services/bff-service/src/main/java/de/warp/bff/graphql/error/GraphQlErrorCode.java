// [KI-Log Nr. KI-033] (7bdabfa) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add graphql to backend and frontend. Initialize dict for SPA (WARP-003)
// [KI-Log Nr. KI-063] (cde9f97) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: expose activity profiles through BFF (WARP-033)
// [KI-Log Nr. KI-099] (21f31de) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: harden BFF session management
// [KI-Log Nr. KI-101] (c1e1a5c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: implement MapTiler place geocoding (WARP-010)
// [KI-Log Nr. KI-133] (943d006) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: implement MapTiler place geocoding (WARP-010)
// [KI-Log Nr. KI-163] (41862d2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose route calculations via GraphQL
// [KI-Log Nr. KI-179] (5bc6d66) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose route calculations via GraphQL
// [KI-Log Nr. KI-182] (0927937) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: some minor changes to security, rebasing (WARP-033)
// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.graphql.error;

public enum GraphQlErrorCode {

    BAD_REQUEST,
    VALIDATION_FAILED,
    NOT_AUTHENTICATED,
    SESSION_INVALID,
    ACCESS_DENIED,
    RESOURCE_NOT_FOUND,
    GEOCODING_SERVICE_UNAVAILABLE,
    DOWNSTREAM_SERVICE_UNAVAILABLE,
    DOWNSTREAM_SERVICE_ERROR,
    GRAPHQL_EXECUTION_ERROR,
    INTERNAL_SERVER_ERROR,
    ACTIVITY_PROFILE_NOT_FOUND,
    INVALID_ROUTE_REQUEST,
    ROUTE_CALCULATION_NOT_FOUND,
    ROUTING_SERVICE_UNAVAILABLE,
    EMAIL_NOT_VERIFIED,
    SAVED_ROUTE_NOT_FOUND
}