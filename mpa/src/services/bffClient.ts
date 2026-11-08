// [KI-Log Nr. KI-228] (7235e0b) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(mpa): implement basic multi-page application with auth, profile, and GraphQL integration (WARP-034)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
import { config } from '../config.js'
import type {
  ActivityProfile,
  GraphQLResponse,
  LoginResult,
  OverviewQueryData,
  ProfileQueryData,
  RegisterResult,
  User,
  CurrentSession,
} from './types.js'

export class BffClient {
  private readonly bffUrl: string

  constructor(bffUrl: string = config.bffUrl) {
    this.bffUrl = bffUrl
  }

  /**
   * Führt eine GraphQL-Operation gegen das BFF aus.
   */
  async executeGraphQL<T>(
    query: string,
    variables?: Record<string, unknown>,
    sessionToken?: string
  ): Promise<GraphQLResponse<T>> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    }

    if (sessionToken) {
      headers['Cookie'] = `session=${sessionToken}`
    }

    try {
      const response = await fetch(`${this.bffUrl}/graphql`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ query, variables }),
      })

      if (!response.ok && response.status !== 200) {
        // Spring GraphQL may return 200 with errors or error status
        try {
          const json = await response.json()
          return json as GraphQLResponse<T>
        } catch {
          return {
            errors: [
              {
                message: `GraphQL-Anfrage fehlgeschlagen mit Status ${response.status}`,
              },
            ],
          }
        }
      }

      return (await response.json()) as GraphQLResponse<T>
    } catch (error) {
      return {
        errors: [
          {
            message:
              error instanceof Error
                ? error.message
                : 'Verbindung zum GraphQL-Endpunkt fehlgeschlagen',
          },
        ],
      }
    }
  }

  /**
   * Lädt Übersichtsdaten per GraphQL: Benutzerinfo (falls angemeldet) und Aktivitätsprofile.
   */
  async fetchOverviewData(sessionToken?: string): Promise<{
    user: User | null
    activityProfiles: ActivityProfile[]
  }> {
    const query = `
      query OverviewData {
        currentUser {
          id
          name
          email
          emailVerified
          createdAt
        }
        activityProfiles {
          code
          displayName
        }
      }
    `

    const response = await this.executeGraphQL<OverviewQueryData>(
      query,
      undefined,
      sessionToken
    )

    const user = response.data?.currentUser ?? null
    const activityProfiles = response.data?.activityProfiles ?? [
      { code: 'BICYCLE', displayName: 'Fahrrad' },
      { code: 'MOTORCYCLE', displayName: 'Motorrad' },
      { code: 'HIKING', displayName: 'Wandern' },
    ]

    return { user, activityProfiles }
  }

  /**
   * Lädt Profil- und Sitzungsdaten des aktuellen Benutzers per GraphQL.
   */
  async fetchUserProfile(sessionToken: string): Promise<{
    user: User | null
    session: CurrentSession | null
  }> {
    const query = `
      query CurrentUserProfile {
        currentUser {
          id
          name
          email
          emailVerified
          createdAt
        }
        currentSession {
          userId
          createdAt
          expiresAt
        }
      }
    `

    const response = await this.executeGraphQL<ProfileQueryData>(
      query,
      undefined,
      sessionToken
    )

    // Falls die Sitzung ungültig ist, ist currentUser null
    const hasSessionInvalidError = response.errors?.some(
      (err) =>
        err.extensions?.code === 'SESSION_INVALID' ||
        err.message?.includes('SESSION_INVALID')
    )

    if (hasSessionInvalidError) {
      return { user: null, session: null }
    }

    const user = response.data?.currentUser ?? null
    const session = response.data?.currentSession ?? null

    return { user, session }
  }

  /**
   * Meldet einen Benutzer an (REST: POST /api/v1/sessions).
   */
  async login(email: string, password: string): Promise<LoginResult> {
    try {
      const response = await fetch(`${this.bffUrl}/api/v1/sessions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json',
        },
        body: JSON.stringify({ email, password }),
      })

      if (response.ok) {
        let sessionToken: string | undefined

        // Extrahiere Cookie aus Header
        const setCookieHeaders =
          typeof response.headers.getSetCookie === 'function'
            ? response.headers.getSetCookie()
            : [response.headers.get('set-cookie') ?? '']

        for (const cookieStr of setCookieHeaders) {
          const match = cookieStr.match(/session=([^;]+)/)
          if (match) {
            sessionToken = match[1]
            break
          }
        }

        const data = (await response.json().catch(() => ({}))) as {
          userId?: string
          expiresAt?: string
        }

        return {
          success: true,
          sessionToken,
          userId: data.userId,
          expiresAt: data.expiresAt,
        }
      }

      if (response.status === 401) {
        return {
          success: false,
          errorMessage:
            'Die angegebene E-Mail-Adresse oder das Passwort ist falsch.',
        }
      }

      if (response.status === 429) {
        return {
          success: false,
          errorMessage:
            'Zu viele fehlgeschlagene Anmeldeversuche. Bitte warte einige Minuten.',
        }
      }

      const errorBody = (await response.json().catch(() => ({}))) as {
        detail?: string
        message?: string
      }

      return {
        success: false,
        errorMessage:
          errorBody.detail ||
          errorBody.message ||
          'Anmeldung fehlgeschlagen. Bitte prüfe deine Angaben.',
      }
    } catch (error) {
      return {
        success: false,
        errorMessage:
          'Der Anmeldedienst ist momentan nicht erreichbar. Bitte versuche es später erneut.',
      }
    }
  }

  /**
   * Registriert einen neuen Benutzer (REST: POST /api/v1/users).
   */
  async register(
    name: string,
    email: string,
    password: string
  ): Promise<RegisterResult> {
    try {
      const response = await fetch(`${this.bffUrl}/api/v1/users`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json',
        },
        body: JSON.stringify({ name, email, password }),
      })

      if (response.status === 201 || response.ok) {
        const data = (await response.json().catch(() => ({}))) as {
          userId?: string
          id?: string
        }
        return {
          success: true,
          userId: data.userId || data.id,
        }
      }

      const body = (await response.json().catch(() => ({}))) as {
        code?: string
        detail?: string
        message?: string
        fieldErrors?: Array<{ field: string; message: string }>
      }

      if (response.status === 409 || body.code === 'EMAIL_ALREADY_EXISTS') {
        return {
          success: false,
          errorMessage:
            'Für diese E-Mail-Adresse existiert bereits ein Benutzerkonto.',
        }
      }

      const fieldErrors: Record<string, string> = {}
      if (Array.isArray(body.fieldErrors)) {
        for (const fe of body.fieldErrors) {
          fieldErrors[fe.field] = fe.message
        }
      }

      return {
        success: false,
        errorMessage:
          body.detail ||
          body.message ||
          'Die Registrierung konnte nicht durchgeführt werden.',
        fieldErrors: Object.keys(fieldErrors).length > 0 ? fieldErrors : undefined,
      }
    } catch (error) {
      return {
        success: false,
        errorMessage:
          'Der Registrierungsdienst ist momentan nicht erreichbar. Bitte versuche es später erneut.',
      }
    }
  }

  /**
   * Beendet die aktuelle Sitzung (REST: DELETE /api/v1/sessions/current).
   */
  async logout(sessionToken: string): Promise<void> {
    try {
      await fetch(`${this.bffUrl}/api/v1/sessions/current`, {
        method: 'DELETE',
        headers: {
          Cookie: `session=${sessionToken}`,
        },
      })
    } catch {
      // Ignoriere Fehler beim Abmelden im Downstream-Dienst
    }
  }
}

export const bffClient = new BffClient()
