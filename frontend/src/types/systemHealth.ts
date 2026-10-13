export interface ServiceHealth {
    service: string
    status: string
    reachable: boolean
    responseTimeMs: number | null
}

export interface SystemHealth {
    status: string
    checkedAt: string | null
    healthyServices: number
    totalServices: number
    services: ServiceHealth[]
}

export interface SystemHealthQueryData {
    systemHealth: SystemHealth | null
}