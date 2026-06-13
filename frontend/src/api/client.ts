// URL base de la API. Se lee de VITE_API_BASE_URL en build/dev y, si no existe,
// cae a localhost:8080. Antes estaba duplicada en ~12 ficheros; ahora vive aquí.
export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080'

// Cabecera de autorización JWT. El token vive en el contexto de auth (useAuth),
// así que se pasa como argumento. Devuelve {} si no hay token, para poder
// hacer spread sin condicionales: { ...authHeader(token) }.
export function authHeader(token: string | null | undefined): Record<string, string> {
  return token ? { Authorization: `Bearer ${token}` } : {}
}
