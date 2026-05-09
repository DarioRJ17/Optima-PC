import { useEffect, useState, type FormEvent } from 'react'
import { BrowserRouter, Routes, Route, useNavigate } from 'react-router-dom'
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
  mode: AuthMode
  catalogItems: CatalogPremontado[]
  catalogLoading: boolean
  catalogError: string
  selectedFilters: SelectedFilters
  setSelectedFilters: React.Dispatch<React.SetStateAction<SelectedFilters>>
  switchMode: (newMode: AuthMode) => void
  loginData: LoginData
  setLoginData: React.Dispatch<React.SetStateAction<LoginData>>
  registerData: RegisterData
  setRegisterData: React.Dispatch<React.SetStateAction<RegisterData>>
  passwordStrength: string | null
  fieldErrors: Record<string, string>
  globalError: string
  successMessage: string
  loading: boolean
  onLoginSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
  onRegisterSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
}

function AppShell({
  mode,
  catalogItems,
  catalogLoading,
  catalogError,
  selectedFilters,
  setSelectedFilters,
  switchMode,
  loginData,
  setLoginData,
  registerData,
  setRegisterData,
  passwordStrength,
  fieldErrors,
  globalError,
  successMessage,
  loading,
  onLoginSubmit,
  onRegisterSubmit,
}: AppShellProps) {
  const navigate = useNavigate()

  const openAuth = (nextMode: AuthMode) => {
    switchMode(nextMode)
    navigate('/auth')
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
              openAuth={(nextMode) => openAuth(nextMode)}
            />
          }
        />
        <Route path="/productos/:id" element={<ProductDetailPage onBack={() => navigate('/')} />} />
        <Route
          path="/auth"
          element={
            <AuthPage
              mode={mode}
              switchMode={switchMode}
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
      </Routes>
    </div>
  )
}

function App() {
  const [mode, setMode] = useState<AuthMode>('login')

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

  const switchMode = (newMode: AuthMode) => {
    setMode(newMode)
    setFieldErrors({})
    setGlobalError('')
    setSuccessMessage('')
    setPasswordStrength(null)
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
      setSuccessMessage(`Bienvenido, ${data.nombre} ${data.apellidos}`)
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
      setSuccessMessage(`Cuenta creada para ${data.nombre} ${data.apellidos}`)
      setLoginData({ email: data.email, password: '' })
      setRegisterData({
        nombre: '',
        apellidos: '',
        email: '',
        password: '',
        confirmPassword: '',
      })
      setMode('login')
    } catch {
      setGlobalError('No se pudo conectar con el backend')
    } finally {
      setLoading(false)
    }
  }

  return (
    <BrowserRouter>
      <AppShell
        mode={mode}
        catalogItems={catalogItems}
        catalogLoading={catalogLoading}
        catalogError={catalogError}
        selectedFilters={selectedFilters}
        setSelectedFilters={setSelectedFilters}
        switchMode={switchMode}
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
      />
    </BrowserRouter>
  )
}

export default App
