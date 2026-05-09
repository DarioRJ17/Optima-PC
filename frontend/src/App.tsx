import { useEffect, useState, type Dispatch, type FormEvent, type SetStateAction } from 'react'
import { BrowserRouter, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import './App.css'
import { AppLogo } from './components/common'
import { AuthPage } from './pages/AuthPage.tsx'
import { HomePage } from './pages/HomePage'
import { ProductDetailPage } from './pages/ProductDetailPage'
import type {
  ApiError,
  AuthMode,
  AuthResponse,
  CatalogPremontado,
  LoginData,
  RegisterData,
  SelectedFilters,
} from './types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080'

type AppShellProps = {
  catalogItems: CatalogPremontado[]
  catalogLoading: boolean
  catalogError: string
  selectedFilters: SelectedFilters
  setSelectedFilters: Dispatch<SetStateAction<SelectedFilters>>
  loginData: LoginData
  setLoginData: Dispatch<SetStateAction<LoginData>>
  registerData: RegisterData
  setRegisterData: Dispatch<SetStateAction<RegisterData>>
  passwordStrength: string | null
  fieldErrors: Record<string, string>
  globalError: string
  successMessage: string
  loading: boolean
  setFieldErrors: Dispatch<SetStateAction<Record<string, string>>>
  setGlobalError: Dispatch<SetStateAction<string>>
  setSuccessMessage: Dispatch<SetStateAction<string>>
  setLoading: Dispatch<SetStateAction<boolean>>
  setPasswordStrength: Dispatch<SetStateAction<string | null>>
  setUser: Dispatch<SetStateAction<{ nombre: string; apellidos: string; email: string } | null>>
}

function AppShell({
  catalogItems,
  catalogLoading,
  catalogError,
  selectedFilters,
  setSelectedFilters,
  loginData,
  setLoginData,
  registerData,
  setRegisterData,
  passwordStrength,
  fieldErrors,
  globalError,
  successMessage,
  loading,
  setFieldErrors,
  setGlobalError,
  setSuccessMessage,
  setLoading,
  setPasswordStrength,
  setUser,
}: AppShellProps) {
  const navigate = useNavigate()

  const clearAuthMessages = () => {
    setFieldErrors({})
    setGlobalError('')
    setSuccessMessage('')
    setPasswordStrength(null)
  }

  const openAuth = (nextMode: AuthMode) => {
    clearAuthMessages()
    navigate(nextMode === 'login' ? '/login' : '/signup')
  }

  const parseError = async (response: Response) => {
    try {
      const data = (await response.json()) as ApiError
      const nextFieldErrors = data.fieldErrors ?? {}
      const hasFieldErrors = Object.keys(nextFieldErrors).length > 0

      setFieldErrors(nextFieldErrors)
      setGlobalError(hasFieldErrors ? '' : data.message || 'No se pudo procesar la solicitud')
    } catch {
      setFieldErrors({})
      setGlobalError('Error de conexion con el servidor')
    }
  }

  const onLoginSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoading(true)
    setFieldErrors({})
    setGlobalError('')
    setSuccessMessage('')

    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(loginData),
      })

      if (!response.ok) {
        await parseError(response)
        return
      }

      const data = (await response.json()) as AuthResponse
      setUser({ nombre: data.nombre, apellidos: data.apellidos, email: data.email })
      navigate('/')
    } catch {
      setGlobalError('No se pudo conectar con el backend')
    } finally {
      setLoading(false)
    }
  }

  const onRegisterSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoading(true)
    setFieldErrors({})
    setGlobalError('')
    setSuccessMessage('')

    if (registerData.password !== registerData.confirmPassword) {
      setFieldErrors({ confirmPassword: 'Las contraseñas no coinciden' })
      setLoading(false)
      return
    }

    try {
      const payload = {
        nombre: registerData.nombre,
        apellidos: registerData.apellidos,
        email: registerData.email,
        password: registerData.password,
      }

      const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })

      if (!response.ok) {
        await parseError(response)
        return
      }

      const data = (await response.json()) as AuthResponse
      // auto-login: set user and go home
      setUser({ nombre: data.nombre, apellidos: data.apellidos, email: data.email })
      setLoginData({ email: data.email, password: '' })
      setRegisterData({
        nombre: '',
        apellidos: '',
        email: '',
        password: '',
        confirmPassword: '',
      })
      navigate('/')
    } catch {
      setGlobalError('No se pudo conectar con el backend')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="app-root">
      <header className="topbar">
        <button className="brand-button" type="button" onClick={() => navigate('/')}>
          <AppLogo />
        </button>

        <nav className="topbar-actions" aria-label="Navegación principal">
          <button type="button" className="nav-chip nav-chip--menu">
            <span aria-hidden="true">☰</span>
            <span>Menú</span>
            <span aria-hidden="true">⌄</span>
          </button>
          <button type="button" className="nav-link" onClick={() => openAuth('login')}>
            <span aria-hidden="true">↪</span>
            <span>Iniciar sesión</span>
          </button>
          <button type="button" className="nav-primary" onClick={() => openAuth('register')}>
            <span aria-hidden="true">👤+</span>
            <span>Registrarse</span>
          </button>
          <button type="button" className="nav-chip nav-chip--cart">
            <span aria-hidden="true">🛒</span>
            <span>Carrito</span>
          </button>
        </nav>
      </header>

      <Routes>
        <Route
          path="/"
          element={
            <HomePage
              catalogItems={catalogItems}
              catalogLoading={catalogLoading}
              catalogError={catalogError}
              selectedFilters={selectedFilters}
              setSelectedFilters={setSelectedFilters}
              openAuth={openAuth}
            />
          }
        />
        <Route path="/productos/:id" element={<ProductDetailPage onBack={() => navigate('/')} />} />
        <Route
          path="/login"
          element={
            <AuthPage
              mode="login"
              switchMode={() => openAuth('register')}
              loginData={loginData}
              setLoginData={setLoginData}
              registerData={registerData}
              setRegisterData={setRegisterData}
              passwordStrength={passwordStrength}
              fieldErrors={fieldErrors}
              globalError={globalError}
              successMessage={successMessage}
              loading={loading}
              onLoginSubmit={onLoginSubmit}
              onRegisterSubmit={onRegisterSubmit}
              goHome={() => navigate('/')}
            />
          }
        />
        <Route
          path="/signup"
          element={
            <AuthPage
              mode="register"
              switchMode={() => openAuth('login')}
              loginData={loginData}
              setLoginData={setLoginData}
              registerData={registerData}
              setRegisterData={setRegisterData}
              passwordStrength={passwordStrength}
              fieldErrors={fieldErrors}
              globalError={globalError}
              successMessage={successMessage}
              loading={loading}
              onLoginSubmit={onLoginSubmit}
              onRegisterSubmit={onRegisterSubmit}
              goHome={() => navigate('/')}
            />
          }
        />
        <Route path="/auth" element={<Navigate to="/login" replace />} />
      </Routes>
    </div>
  )
}

function App() {
  const [, setUser] = useState<{ nombre: string; apellidos: string; email: string } | null>(null)
  const [catalogItems, setCatalogItems] = useState<CatalogPremontado[]>([])
  const [catalogLoading, setCatalogLoading] = useState(true)
  const [catalogError, setCatalogError] = useState('')

  const [selectedFilters, setSelectedFilters] = useState<SelectedFilters>({
    priceRange: null,
    tipos: new Set(),
  })

  const [loginData, setLoginData] = useState<LoginData>({ email: '', password: '' })
  const [registerData, setRegisterData] = useState<RegisterData>({
    nombre: '',
    apellidos: '',
    email: '',
    password: '',
    confirmPassword: '',
  })
  const [passwordStrength, setPasswordStrength] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [globalError, setGlobalError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    const controller = new AbortController()

    const loadCatalog = async () => {
      setCatalogLoading(true)
      setCatalogError('')

      try {
        const url = new URL(`${API_BASE_URL}/api/catalogo/premontados`)

        if (selectedFilters.priceRange) {
          url.searchParams.append('minPrice', String(selectedFilters.priceRange.minPrice))
          url.searchParams.append('maxPrice', String(selectedFilters.priceRange.maxPrice))
        }

        const reacondicionadoSelected = selectedFilters.tipos.has('__reacondicionado__')
        const tiposSeleccionados = Array.from(selectedFilters.tipos).filter(
          (tipo) => tipo !== '__reacondicionado__'
        )

        if (reacondicionadoSelected) {
          url.searchParams.append('reacondicionado', 'true')
        }

        tiposSeleccionados.forEach((tipo) => {
          url.searchParams.append('tipos', tipo)
        })

        const response = await fetch(url.toString(), {
          signal: controller.signal,
        })

        if (!response.ok) {
          throw new Error('No se pudo cargar el catálogo')
        }

        const data = (await response.json()) as CatalogPremontado[]
        setCatalogItems(Array.isArray(data) ? data : [])
      } catch (error) {
        if (!(error instanceof DOMException && error.name === 'AbortError')) {
          setCatalogError('No se pudo cargar el catálogo desde el backend')
        }
      } finally {
        if (!controller.signal.aborted) {
          setCatalogLoading(false)
        }
      }
    }

    void loadCatalog()

    return () => controller.abort()
  }, [selectedFilters])

  useEffect(() => {
    if (!registerData.password) {
      setPasswordStrength(null)
      return
    }

    const timer = setTimeout(async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/api/auth/password-strength`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ password: registerData.password }),
        })

        if (response.ok) {
          const data = await response.json()
          setPasswordStrength(data.strength)
        }
      } catch {
        // silencioso, no es crítico
      }
    }, 300)

    return () => clearTimeout(timer)
  }, [registerData.password])

  return (
    <BrowserRouter>
      <AppShell
        catalogItems={catalogItems}
        catalogLoading={catalogLoading}
        catalogError={catalogError}
        selectedFilters={selectedFilters}
        setSelectedFilters={setSelectedFilters}
        loginData={loginData}
        setLoginData={setLoginData}
        registerData={registerData}
        setRegisterData={setRegisterData}
        passwordStrength={passwordStrength}
        fieldErrors={fieldErrors}
        globalError={globalError}
        successMessage={successMessage}
        loading={loading}
        setFieldErrors={setFieldErrors}
        setGlobalError={setGlobalError}
        setSuccessMessage={setSuccessMessage}
        setLoading={setLoading}
        setPasswordStrength={setPasswordStrength}
        setUser={setUser}
      />
    </BrowserRouter>
  )
}

export default App
