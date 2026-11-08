// [KI-Log Nr. KI-228] (7235e0b) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(mpa): implement basic multi-page application with auth, profile, and GraphQL integration (WARP-034)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
export interface User {
  id: string
  name: string
  email: string
  emailVerified: boolean
  createdAt: string
}

export interface CurrentSession {
  userId: string
  createdAt: string
  expiresAt: string
}

export interface ActivityProfile {
  code: string
  displayName: string
}

export interface GraphQLResponse<T> {
  data?: T
  errors?: Array<{
    message: string
    extensions?: {
      code?: string
      classification?: string
      [key: string]: unknown
    }
  }>
}

export interface OverviewQueryData {
  currentUser?: User | null
  activityProfiles: ActivityProfile[]
}

export interface ProfileQueryData {
  currentUser: User | null
  currentSession: CurrentSession
}

export interface LoginResult {
  success: boolean
  sessionToken?: string
  userId?: string
  expiresAt?: string
  errorMessage?: string
}

export interface RegisterResult {
  success: boolean
  userId?: string
  errorMessage?: string
  fieldErrors?: Record<string, string>
}
