import { createContext } from 'react'

export type AuthUser = {
  id: number
  nombre: string
  apellidos: string
  email: string
}

export type AuthContextValue = {
  user: AuthUser | null
  token: string | null
  setAuth: (user: AuthUser | null, token: string | null) => void
  logout: () => void
  isAuthenticated: boolean
}

export const STORAGE_KEY_USER = 'optimapc.auth.user'
export const STORAGE_KEY_TOKEN = 'optimapc.auth.token'

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function readStoredAuth(): { user: AuthUser | null; token: string | null } {
  try {
    const rawUser = localStorage.getItem(STORAGE_KEY_USER)
    const rawToken = localStorage.getItem(STORAGE_KEY_TOKEN)

    const user = rawUser ? (JSON.parse(rawUser) as AuthUser) : null
    const token = rawToken || null

    return { user, token }
  } catch {
    return { user: null, token: null }
  }
}