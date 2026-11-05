// [KI-Log Nr. KI-02] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Monorepo-Setup, Gradle & Docker (WARP-001)
/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly VITE_BFF_BASE_URL?: string
    readonly VITE_MAPTILER_PUBLIC_API_KEY?: string
}

interface ImportMeta {
    readonly env: ImportMetaEnv
}