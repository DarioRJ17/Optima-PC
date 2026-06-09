import { useEffect, useState, type FormEvent } from 'react'
import { BrowserRouter, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import './App.css'
import { AppLogo } from './components/common'
import { AuthPage } from './pages/AuthPage.tsx'
import { HomePage } from './pages/HomePage'
import { InitialSurveyPage } from './pages/InitialSurveyPage'
import { ProductDetailPage } from './pages/ProductDetailPage'
import { PasswordRecoveryPage } from './pages/PasswordRecoveryPage'
import { PasswordResetPage } from './pages/PasswordResetPage'
import { AuthProvider } from './auth/AuthContext'
import { useAuth } from './auth/useAuth'
import { MontarPCPage } from './pages/MontarPCPage'
import { ReciclajePage } from './pages/ReciclajePage'
import { Recycle, Wrench, Bot, LogIn, UserPlus, ShoppingCart, Menu, Heart, Package } from 'lucide-react'
import { ChatbotPage } from './pages/ChatbotPage'
import { FavoritosPage } from './pages/FavoritosPage'
import { CarritoPage } from './pages/CarritoPage'
import { ComprasPage } from './pages/ComprasPage'
import type {
  ApiError,
  AuthMode,
  AuthResponse,
  CartItem,
  CatalogPremontado,
  LoginData,
  RegisterData,
  SelectedFilters,
} from './types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080'

function AppShell() {
  const navigate = useNavigate()
  const { user, setAuth, logout, token } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const [userMenuOpen, setUserMenuOpen] = useState(false)
  const [recommendationsRefreshKey, setRecommendationsRefreshKey] = useState(0)
  const [favoritosIds, setFavoritosIds] = useState<Set<number>>(new Set())
  const [favoritosLoaded, setFavoritosLoaded] = useState(false)
  const [cartItems, setCartItems] = useState<CartItem[]>([])

  // Estados que antes estaban en App()
  const [catalogItems, setCatalogItems] = useState<CatalogPremontado[]>([])
  const [catalogLoading, setCatalogLoading] = useState(true)
  const [catalogError, setCatalogError] = useState('')
  const [recommendationItems, setRecommendationItems] = useState<CatalogPremontado[]>([])
  const [recommendationsLoading, setRecommendationsLoading] = useState(false)
  const [recommendationsError, setRecommendationsError] = useState('')

  const [selectedFilters, setSelectedFilters] = useState<SelectedFilters>({
    priceRange: null,
    tipos: new Set(),
    marcas: new Set(),
    sistemasOperativos: new Set(),
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

  /**
   * Efecto para cargar el catálogo
   */
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
          headers: token ? { Authorization: `Bearer ${token}` } : undefined,
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
  }, [selectedFilters.priceRange, selectedFilters.tipos, token])

  /**
   * Efecto para cargar recomendaciones personalizadas (solo usuario autenticado)
   */
  useEffect(() => {
    const controller = new AbortController()

    if (!user || !token) {
      setRecommendationItems([])
      setRecommendationsError('')
      setRecommendationsLoading(false)
      return () => controller.abort()
    }

    const loadRecommendations = async () => {
      setRecommendationsLoading(true)
      setRecommendationsError('')

      try {
        const response = await fetch(`${API_BASE_URL}/api/catalogo/premontados/recomendaciones`, {
          signal: controller.signal,
          headers: {
            Authorization: `Bearer ${token}`,
          },
        })

        if (!response.ok) {
          throw new Error('No se pudieron cargar las recomendaciones')
        }

        const data = (await response.json()) as CatalogPremontado[]
        setRecommendationItems(Array.isArray(data) ? data : [])
      } catch (error) {
        if (!(error instanceof DOMException && error.name === 'AbortError')) {
          setRecommendationsError('No se pudieron cargar tus recomendaciones')
        }
      } finally {
        if (!controller.signal.aborted) {
          setRecommendationsLoading(false)
        }
      }
    }

    void loadRecommendations()

    return () => controller.abort()
  }, [user, token, recommendationsRefreshKey])

  const refreshRecommendations = () => {
    setRecommendationsRefreshKey((current) => current + 1)
  }

  useEffect(() => {
    if (!user || !token) {
      setFavoritosIds(new Set())
      setFavoritosLoaded(false)
      return
    }

    const controller = new AbortController()

    const loadFavoritos = async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/api/catalogo/favoritos`, {
          headers: { Authorization: `Bearer ${token}` },
          signal: controller.signal,
        })
        if (response.ok) {
          const data = (await response.json()) as Array<{ premontado: { id: number } }>
          setFavoritosIds(new Set(data.map((f) => f.premontado.id)))
        }
      } catch {
        // silencioso — el estado de favoritos queda vacío
      } finally {
        if (!controller.signal.aborted) setFavoritosLoaded(true)
      }
    }

    void loadFavoritos()
    return () => controller.abort()
  }, [user, token])

  const toggleFavorito = async (premontadoId: number) => {
    if (!user || !token) return

    const esFavorito = favoritosIds.has(premontadoId)

    setFavoritosIds((prev) => {
      const next = new Set(prev)
      if (esFavorito) next.delete(premontadoId)
      else next.add(premontadoId)
      return next
    })

    try {
      await fetch(`${API_BASE_URL}/api/catalogo/premontados/${premontadoId}/favoritos`, {
        method: esFavorito ? 'DELETE' : 'POST',
        headers: { Authorization: `Bearer ${token}` },
      })
      refreshRecommendations()
    } catch {
      setFavoritosIds((prev) => {
        const next = new Set(prev)
        if (esFavorito) next.add(premontadoId)
        else next.delete(premontadoId)
        return next
      })
    }
  }

  /**
   * Efecto para validar fortaleza de contraseña
   */
  useEffect(() => {
    if (!registerData.password) {
      setPasswordStrength(null)
      return
    }

    const timer = setTimeout(async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/api/auth/password-strength`, {
          method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
            },
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
  }, [registerData.password, token])

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
      setAuth(
        { id: data.id, nombre: data.nombre, apellidos: data.apellidos, email: data.email },
        data.token
      )
      setLoginData({ email: '', password: '' })
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
      // auto-login: set user and token, then go home
      setAuth(
        { id: data.id, nombre: data.nombre, apellidos: data.apellidos, email: data.email },
        data.token
      )
      setRegisterData({
        nombre: '',
        apellidos: '',
        email: '',
        password: '',
        confirmPassword: '',
      })
      navigate('/perfil-inicial', { replace: true })
    } catch {
      setGlobalError('No se pudo conectar con el backend')
    } finally {
      setLoading(false)
    }
  }

  const addToCart = (item: CartItem) => {
    setCartItems((prev) => {
      const existing = prev.find((i) => i.configuracionId === item.configuracionId)
      if (existing) {
        return prev.map((i) =>
          i.configuracionId === item.configuracionId ? { ...i, cantidad: i.cantidad + 1 } : i
        )
      }
      return [...prev, item]
    })
  }

  const removeFromCart = (configuracionId: number) => {
    setCartItems((prev) => prev.filter((i) => i.configuracionId !== configuracionId))
  }

  const updateCartQuantity = (configuracionId: number, cantidad: number) => {
    if (cantidad < 1) return
    setCartItems((prev) =>
      prev.map((i) => (i.configuracionId === configuracionId ? { ...i, cantidad } : i))
    )
  }

  const clearCart = () => setCartItems([])

  return (
    <div className="app-root">
      <header className="topbar">
        <button className="brand-button" type="button" onClick={() => navigate('/')}>
          <AppLogo />
        </button>

        <nav className="topbar-actions" aria-label="Navegación principal">
          {user ? (
            <>
              <div className="nav-user">
                <button
                  type="button"
                  className="nav-chip nav-chip--menu"
                  onClick={() => setMenuOpen((s) => !s)}
                  aria-haspopup="true"
                  aria-expanded={menuOpen}
                >
                  <Menu size={16} strokeWidth={1.75} aria-hidden="true" />
                  <span>Menú</span>
                  <span aria-hidden="true">⌄</span>
                </button>
                {menuOpen ? (
                  <div className="nav-user__menu" role="menu">
                    <button
                      type="button"
                      role="menuitem"
                      onClick={() => {
                        navigate('/montar-pc')
                        setMenuOpen(false)
                      }}
                    >
                      <Wrench size={16} strokeWidth={1.75} aria-hidden="true" />
                      <span>Montar tu propio PC</span>
                    </button>
                    <button
                      type="button"
                      role="menuitem"
                      onClick={() => {
                        navigate('/reciclaje')
                        setMenuOpen(false)
                      }}
                    >
                      <Recycle size={16} strokeWidth={1.75} aria-hidden="true" />
                      <span>Reciclaje de componentes</span>
                    </button>
                    <button
                      type="button"
                      role="menuitem"
                      onClick={() => {
                        navigate('/chat')
                        setMenuOpen(false)
                      }}
                    >
                      <Bot size={16} strokeWidth={1.75} aria-hidden="true" />
                      <span>Asistente IA</span>
                    </button>
                  </div>
                ) : null}
              </div>
              <div className="nav-user">
                <button
                  type="button"
                  className="nav-chip nav-chip--menu"
                  onClick={() => setUserMenuOpen((s) => !s)}
                  aria-haspopup="true"
                  aria-expanded={userMenuOpen}
                >
                  <span aria-hidden="true">👤</span>
                  <span>{user.nombre}</span>
                  <span aria-hidden="true">⌄</span>
                </button>
                {userMenuOpen ? (
                  <div className="nav-user__menu" role="menu">
                    <button
                      type="button"
                      role="menuitem"
                      onClick={() => {
                        navigate('/compras')
                        setUserMenuOpen(false)
                      }}
                    >
                      <Package size={16} strokeWidth={1.75} aria-hidden="true" />
                      <span>Mis compras</span>
                    </button>
                    <button
                      type="button"
                      role="menuitem"
                      onClick={() => {
                        navigate('/favoritos')
                        setUserMenuOpen(false)
                      }}
                    >
                      <Heart size={16} strokeWidth={1.75} aria-hidden="true" />
                      <span>Mis favoritos</span>
                    </button>
                    <div className="nav-user__menu-divider" aria-hidden="true" />
                    <button
                      type="button"
                      role="menuitem"
                      onClick={() => {
                        logout()
                        navigate('/')
                        setUserMenuOpen(false)
                      }}
                    >
                      Cerrar sesión
                    </button>
                  </div>
                ) : null}
              </div>
            </>
          ) : (
            <>
              <div className="nav-user">
                <button 
                  type="button"
                  className="nav-chip nav-chip--menu"
                  onClick={() => setMenuOpen((s) => !s)}
                  aria-haspopup="true"
                  aria-expanded={menuOpen}
                >
                  <Menu size={16} strokeWidth={1.75} aria-hidden="true" />
                  <span>Menú</span>
                  <span aria-hidden="true">⌄</span>
                </button>
                {menuOpen ? (
                  <div className="nav-user__menu" role="menu">
                    <button
                      type="button"
                      role="menuitem"
                      onClick={() => {
                        navigate('/montar-pc')
                        setMenuOpen(false)
                      }}
                    >
                      <Wrench size={16} strokeWidth={1.75} aria-hidden="true" />
                      <span>Montar tu propio PC</span>
                    </button>
                    <button
                      type="button"
                      role="menuitem"
                      onClick={() => {
                        navigate('/reciclaje')
                        setMenuOpen(false)
                      }}
                    >
                      <Recycle size={16} strokeWidth={1.75} aria-hidden="true" />
                      <span>Reciclaje de componentes</span>
                    </button>
                    <button
                      type="button"
                      role="menuitem"
                      onClick={() => {
                        navigate('/chat')
                        setMenuOpen(false)
                      }}
                    >
                      <Bot size={16} strokeWidth={1.75} aria-hidden="true" />
                      <span>Asistente IA</span>
                    </button>
                  </div>
                ) : null}
              </div>
              <button type="button" className="nav-link" onClick={() => openAuth('login')}>
                <LogIn size={16} strokeWidth={1.75} aria-hidden="true" />
                <span>Iniciar sesión</span>
              </button>
              <button type="button" className="nav-primary" onClick={() => openAuth('register')}>
                <UserPlus size={16} strokeWidth={1.75} aria-hidden="true" />
                <span>Registrarse</span>
              </button>
            </>
          )}

          <button type="button" className="nav-chip nav-chip--cart" onClick={() => navigate('/carrito')}>
            <ShoppingCart size={16} strokeWidth={1.75} aria-hidden="true" />
            <span>Carrito</span>
            {cartItems.length > 0 && (
              <span className="cart-badge" aria-label={`${cartItems.length} productos en el carrito`}>
                {cartItems.length}
              </span>
            )}
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
              recommendationItems={recommendationItems}
              recommendationsLoading={recommendationsLoading}
              recommendationsError={recommendationsError}
              showRecommendations={Boolean(user)}
              isAuthenticated={Boolean(user)}
              selectedFilters={selectedFilters}
              setSelectedFilters={setSelectedFilters}
              openAuth={openAuth}
              favoritosIds={favoritosLoaded ? favoritosIds : undefined}
              toggleFavorito={favoritosLoaded ? toggleFavorito : undefined}
            />
          }
        />
        <Route
          path="/productos/:id"
          element={
            <ProductDetailPage
              onBack={() => navigate('/')}
              onReviewSubmitted={refreshRecommendations}
              onAddToCart={addToCart}
              cartItems={cartItems}
            />
          }
        />
        <Route
          path="/login"
          element={
            <AuthPage
              mode="login"
              switchMode={() => openAuth('register')}
              onForgotPassword={() => navigate('/forgot-password')}
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
              onForgotPassword={() => navigate('/forgot-password')}
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
        <Route path="/forgot-password" element={<PasswordRecoveryPage onBack={() => navigate('/login')} />} />
        <Route path="/reset-password" element={<PasswordResetPage onBack={() => navigate('/login')} />} />
        <Route
          path="/perfil-inicial"
          element={<InitialSurveyPage onBack={() => navigate('/')} onSurveySaved={refreshRecommendations} />}
        />
        <Route path="/montar-pc" element={<MontarPCPage onBack={() => navigate('/')} onAddToCart={addToCart} />} />
        <Route path="/reciclaje" element={<ReciclajePage onBack={() => navigate('/')} />} />
        <Route path="/chat" element={<ChatbotPage />} />
        <Route
          path="/favoritos"
          element={<FavoritosPage favoritosIds={favoritosIds} toggleFavorito={toggleFavorito} />}
        />
        <Route
          path="/carrito"
          element={
            <CarritoPage
              cartItems={cartItems}
              onRemoveFromCart={removeFromCart}
              onUpdateQuantity={updateCartQuantity}
              onClearCart={clearCart}
            />
          }
        />
        <Route path="/compras" element={<ComprasPage />} />
        <Route path="/auth" element={<Navigate to="/login" replace />} />
      </Routes>
    </div>
  )
}

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppShell />
      </BrowserRouter>
    </AuthProvider>
  )
}

export default App
