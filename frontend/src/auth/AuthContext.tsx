import { useEffect, useMemo, useState, type ReactNode } from 'react'

import {
  AuthContext,
  STORAGE_KEY_TOKEN,
  STORAGE_KEY_USER,
  type AuthContextValue,
  type AuthUser,
  readStoredAuth,
} from './auth-context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUserState] = useState<AuthUser | null>(() => readStoredAuth().user)
  const [token, setTokenState] = useState<string | null>(() => readStoredAuth().token)

  useEffect(() => {
    try {
      if (user) {
        localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(user))
      } else {
        localStorage.removeItem(STORAGE_KEY_USER)
      }

      if (token) {
        localStorage.setItem(STORAGE_KEY_TOKEN, token)
      } else {
        localStorage.removeItem(STORAGE_KEY_TOKEN)
      }
    } catch {
      // silencioso: si localStorage falla, la app sigue funcionando en memoria
    }
  }, [user, token])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      token,
      setAuth: (newUser, newToken) => {
        setUserState(newUser)
        setTokenState(newToken)
      },
      logout: () => {
        setUserState(null)
        setTokenState(null)
      },
      isAuthenticated: Boolean(user && token),
    }),
    [user, token]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
