import { useEffect, useState, type FormEvent } from 'react'
import './App.css'
import heroImage from './assets/hero.png'

type AuthMode = 'login' | 'register'

type ViewMode = 'home' | 'auth'

type ApiError = {
  message?: string
  fieldErrors?: Record<string, string>
}

type AuthResponse = {
  mensaje: string
  id: number
  nombre: string
  apellidos: string
  email: string
  fechaRegistro: string
}

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080'

// Filtros disponibles basados en datos reales de la BD
const filterGroups = [
  {
    title: 'Precio',
    open: true,
    key: 'price',
    type: 'radio' as const,
    options: [
      { label: 'Menos de 500€', minPrice: 0, maxPrice: 500 },
      { label: '500€ - 1000€', minPrice: 500, maxPrice: 1000 },
      { label: '1000€ - 1500€', minPrice: 1000, maxPrice: 1500 },
      { label: 'Más de 1500€', minPrice: 1500, maxPrice: 999999 },
    ],
  },
  {
    title: 'Tipo de ordenador',
    open: true,
    key: 'tipos',
    type: 'checkbox' as const,
    options: [
      { label: 'Gaming', value: 'GAMING' },
      { label: 'Oficina', value: 'OFIMATICA' },
      { label: 'Programación', value: 'PROGRAMACION' },
      { label: 'Creación de contenido', value: 'EDICION' },
      { label: 'Streaming', value: 'STREAMING' },
      { label: 'Reacondicionado', value: '__reacondicionado__' },
    ],
  },
]

type CatalogPremontado = {
  id: number
  titulo: string
  descripcion: string | null
  marca: string
  descuento: number | null
  sistemaOperativo: string | null
  stock: number
  usosPrevistos: string[]
  imagenUrl: string | null
  esReacondicionado: boolean
  precio: number
  precioReducido: number | null
  valoracionMedia: number
  numeroValoraciones: number
  favorita: boolean | null
  rendimientoPorEuro: number
}

type ProductCard = {
  id: number
  title: string
  badge: string
  ribbon: string
  rating: number
  reviews: number
  performance: number
  performanceLabel: string
  price: string
  oldPrice?: string
  tone: 'gaming' | 'laptop' | 'office' | 'refurb'
  imageUrl?: string
}

function formatEuro(value: number) {
  return new Intl.NumberFormat('es-ES', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}

function normalizarUso(uso: string) {
  return uso.toUpperCase()
}

function buildBadge(item: CatalogPremontado) {
  if (item.esReacondicionado) {
    return 'Reacondicionado'
  }

  const usos = item.usosPrevistos.map(normalizarUso)
  if (usos.includes('GAMING')) return 'Gaming'
  if (usos.includes('OFIMATICA')) return 'Oficina'
  if (usos.includes('PROGRAMACION')) return 'Workstation'
  if (usos.includes('EDICION') || usos.includes('STREAMING')) return 'Creación de contenido'

  return 'Portátil'
}

function buildTone(item: CatalogPremontado): ProductCard['tone'] {
  if (item.esReacondicionado) {
    return 'refurb'
  }

  const usos = item.usosPrevistos.map(normalizarUso)
  if (usos.includes('GAMING')) return 'gaming'
  if (usos.includes('OFIMATICA') || usos.includes('PROGRAMACION')) return 'office'
  return 'laptop'
}

function buildRibbon(item: CatalogPremontado, section: 'featured' | 'offers' | 'refurbished') {
  if (section === 'offers' && item.descuento) {
    return `-${item.descuento}%`
  }

  if (section === 'refurbished' && item.esReacondicionado) {
    return 'Reacondicionado'
  }

  return 'Trending'
}

function buildPerformanceLabel(value: number) {
  if (value >= 90) return 'Excelente'
  if (value >= 75) return 'Muy bueno'
  if (value >= 60) return 'Bueno'
  return 'Regular'
}

function buildPerformance(value: number) {
  return Math.max(0, Math.min(100, Math.round(value)))
}

function toProductCard(item: CatalogPremontado, section: 'featured' | 'offers' | 'refurbished'): ProductCard {
  const rating = Math.max(1, Math.min(5, Math.round(item.valoracionMedia || 0)))
  const performance = buildPerformance((item.rendimientoPorEuro || 0))
  const price = item.precioReducido ?? item.precio

  return {
    id: item.id,
    title: item.titulo,
    badge: buildBadge(item),
    ribbon: buildRibbon(item, section),
    rating,
    reviews: item.numeroValoraciones,
    performance,
    performanceLabel: buildPerformanceLabel(performance),
    price: `${formatEuro(price)}€`,
    oldPrice: item.precioReducido ? `${formatEuro(item.precio)}€` : undefined,
    tone: buildTone(item),
    imageUrl:
      item.imagenUrl && !item.imagenUrl.includes('images.example.com') ? item.imagenUrl : undefined,
  }
}

function LogoMark() {
  return (
    <span className="brand-mark" aria-hidden="true">
      <img src="/optimaPC_icon.png" alt="OptimaPC" />
    </span>
  )
}

function AppLogo() {
  return (
    <span className="brand" aria-label="OptimaPC">
      <LogoMark />
      <span className="brand-name">OptimaPC</span>
    </span>
  )
}

function StarRating({ value }: { value: number }) {
  return (
    <div className="star-rating" aria-label={`${value} de 5 estrellas`}>
      {Array.from({ length: 5 }).map((_, index) => (
        <span key={index} className={index < value ? 'star star--filled' : 'star'}>
          ★
        </span>
      ))}
    </div>
  )
}

function PerformanceMeter({ value }: { value: number }) {
  return (
    <div className="performance-meter" aria-label={`Rendimiento ${value} sobre 100`}>
      <div className="performance-meter__header">
        <span>⚡ Rendimiento/€</span>
        <span>ⓘ</span>
      </div>
      <div className="performance-meter__value">
        <strong>{value}</strong>
        <span>/ 100</span>
      </div>
    </div>
  )
}

function ProductCardView({ product }: { product: ProductCard }) {
  return (
    <article className={`product-card product-card--${product.tone}`}>
      <div className="product-card__media">
        <span className="pill pill--primary">{product.badge}</span>
        <span className="pill pill--accent">{product.ribbon}</span>
        <div className="product-card__shine" aria-hidden="true" />
        <img className="product-card__image" src={product.imageUrl || heroImage} alt="" aria-hidden="true" />
      </div>

      <div className="product-card__body">
        <h3>{product.title}</h3>
        <StarRating value={product.rating} />
        <p className="reviews">({product.reviews})</p>
        <PerformanceMeter value={product.performance} />
        <div className="performance-label-row">
          <span>{product.performanceLabel}</span>
          <div className="performance-track" aria-hidden="true">
            <span style={{ width: `${product.performance}%` }} />
          </div>
        </div>
        <div className="price-row">
          {product.oldPrice ? <span className="old-price">{product.oldPrice}</span> : null}
          <strong className="price">{product.price}</strong>
        </div>
        <button type="button" className="details-button">
          Ver detalles
        </button>
      </div>
    </article>
  )
}

const STRENGTH_CONFIG: Record<string, { label: string; color: string; bars: number }> = {
  VERY_WEAK:   { label: 'Muy débil',  color: '#ef4444', bars: 1 },
  WEAK:        { label: 'Débil',      color: '#f97316', bars: 2 },
  FAIR:        { label: 'Regular',    color: '#eab308', bars: 3 },
  STRONG:      { label: 'Fuerte',     color: '#22c55e', bars: 4 },
  VERY_STRONG: { label: 'Muy fuerte', color: '#16a34a', bars: 5 },
}

function PasswordStrengthMeter({ strength }: { strength: string }) {
  const config = STRENGTH_CONFIG[strength] ?? STRENGTH_CONFIG.VERY_WEAK

  return (
    <div className="password-strength">
      <div className="strength-bars">
        {Array.from({ length: 5 }).map((_, i) => (
          <div
            key={i}
            className="strength-bar"
            style={{ backgroundColor: i < config.bars ? config.color : undefined }}
          />
        ))}
      </div>
      <span className="strength-label" style={{ color: config.color }}>
        {config.label}
      </span>
    </div>
  )
}

function App() {
  const [view, setView] = useState<ViewMode>('home')
  const [mode, setMode] = useState<AuthMode>('login')

  const [catalogItems, setCatalogItems] = useState<CatalogPremontado[]>([])
  const [catalogLoading, setCatalogLoading] = useState(true)
  const [catalogError, setCatalogError] = useState('')

  // Estado de filtros
  const [selectedFilters, setSelectedFilters] = useState<{
    priceRange: { minPrice: number; maxPrice: number } | null
    tipos: Set<string>
  }>({
    priceRange: null,
    tipos: new Set(),
  })

  const [loginData, setLoginData] = useState({ email: '', password: '' })
  const [registerData, setRegisterData] = useState({
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
        // Construir URL con parámetros de filtro
        const url = new URL(`${API_BASE_URL}/api/catalogo/premontados`)

        if (selectedFilters.priceRange) {
          url.searchParams.append('minPrice', String(selectedFilters.priceRange.minPrice))
          url.searchParams.append('maxPrice', String(selectedFilters.priceRange.maxPrice))
        }

        const reacondicionadoSelected = selectedFilters.tipos.has('__reacondicionado__')
        const tiposSeleccionados = Array.from(selectedFilters.tipos).filter(
          (t) => t !== '__reacondicionado__'
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

  const openAuth = (nextMode: AuthMode) => {
    setView('auth')
    switchMode(nextMode)
  }

  const goHome = () => {
    setView('home')
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

  const catalogCards = catalogItems.map((item) => toProductCard(item, 'featured'))
  const bestSellers = [...catalogCards]
    .sort((left, right) => right.rating - left.rating || right.reviews - left.reviews)
    .slice(0, 3)
  const offers = catalogItems
    .filter((item) => (item.descuento ?? 0) > 0)
    .sort((left, right) => (right.descuento ?? 0) - (left.descuento ?? 0) || right.valoracionMedia - left.valoracionMedia)
    .map((item) => toProductCard(item, 'offers'))
    .slice(0, 3)
  const refurbished = catalogItems
    .filter((item) => item.esReacondicionado)
    .sort((left, right) => right.valoracionMedia - left.valoracionMedia || right.numeroValoraciones - left.numeroValoraciones)
    .map((item) => toProductCard(item, 'refurbished'))
    .slice(0, 3)

  const fallbackCards = catalogCards.slice(0, 3)
  const sections = [
    { title: 'Ordenadores más vendidos', icon: '↗', products: bestSellers.length > 0 ? bestSellers : fallbackCards },
    { title: 'Mejores ofertas', icon: '🏷', products: offers.length > 0 ? offers : fallbackCards },
    { title: 'Ordenadores reacondicionados', icon: '⟲', products: refurbished.length > 0 ? refurbished : fallbackCards },
  ]

  return (
    <div className="app-root">
      <header className="topbar">
        <button className="brand-button" type="button" onClick={goHome}>
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

      {view === 'home' ? (
        <main className="home-page">
          <aside className="filters-panel">
            <div className="filters-panel__header">
              <h2>Filtros</h2>
              {(selectedFilters.priceRange || selectedFilters.tipos.size > 0) && (
                <span className="filters-badge">
                  {(selectedFilters.priceRange ? 1 : 0) + selectedFilters.tipos.size}
                </span>
              )}
            </div>

            {filterGroups.map((group) => {
              const isPriceGroup = group.key === 'price'
              const selectedPrice = isPriceGroup ? selectedFilters.priceRange : undefined

              return (
                <details className="filter-group" key={group.title} open={group.open}>
                  <summary>{group.title}</summary>
                  <div className="filter-group__items">
                    {isPriceGroup
                      ? (group.options as unknown as Array<{ label: string; minPrice: number; maxPrice: number }>).map(
                          (option) => (
                            <label key={option.label} className="filter-option">
                              <input
                                type="radio"
                                name={group.key}
                                checked={
                                  selectedPrice?.minPrice === option.minPrice &&
                                  selectedPrice?.maxPrice === option.maxPrice
                                }
                                onChange={() => {
                                  // Cambiar selección de precio
                                  const newSelectedFilters = { ...selectedFilters }
                                  if (
                                    newSelectedFilters.priceRange?.minPrice === option.minPrice &&
                                    newSelectedFilters.priceRange?.maxPrice === option.maxPrice
                                  ) {
                                    newSelectedFilters.priceRange = null
                                  } else {
                                    newSelectedFilters.priceRange = {
                                      minPrice: option.minPrice,
                                      maxPrice: option.maxPrice,
                                    }
                                  }
                                  setSelectedFilters(newSelectedFilters)
                                }}
                              />
                              <span>{option.label}</span>
                            </label>
                          )
                        )
                      : (group.options as unknown as Array<{ label: string; value: string }>).map((option) => (
                          <label key={option.value} className="filter-option">
                            <input
                              type="checkbox"
                              checked={selectedFilters.tipos.has(option.value)}
                              onChange={(e) => {
                                const newTipos = new Set(selectedFilters.tipos)
                                if (e.target.checked) {
                                  newTipos.add(option.value)
                                } else {
                                  newTipos.delete(option.value)
                                }
                                setSelectedFilters({
                                  ...selectedFilters,
                                  tipos: newTipos,
                                })
                              }}
                            />
                            <span>{option.label}</span>
                          </label>
                        ))}
                  </div>
                </details>
              )
            })}

            <button
              type="button"
              className="filters-panel__apply"
              onClick={() => {
                setSelectedFilters({
                  priceRange: null,
                  tipos: new Set(),
                })
              }}
            >
              Limpiar filtros
            </button>
          </aside>

          <section className="catalog-shell">
            <section className="hero-panel">
              <div className="hero-copy">
                <p className="eyebrow">Tu tienda para gaming, oficina y reacondicionados</p>
                <h1>Encuentra el ordenador que encaja con cada uso</h1>
                <p>
                  Compara rendimiento, precio y estado de forma rápida. La home está pensada para
                  que puedas explorar categorías y volver al acceso desde la barra superior.
                </p>
                <div className="hero-actions">
                  <button type="button" className="hero-primary" onClick={() => openAuth('register')}>
                    Crear cuenta
                  </button>
                  <button type="button" className="hero-secondary" onClick={() => openAuth('login')}>
                    Ya tengo cuenta
                  </button>
                </div>
                {catalogLoading ? <p className="catalog-message">Cargando catálogo real...</p> : null}
                {catalogError ? <p className="catalog-message catalog-message--error">{catalogError}</p> : null}
              </div>
              <div className="hero-visual" aria-hidden="true">
                <img src={heroImage} alt="" />
                <span className="hero-visual__tag hero-visual__tag--top">Equipos destacados</span>
                <span className="hero-visual__tag hero-visual__tag--bottom">Listos para comprar</span>
              </div>
            </section>

            <div className="catalog-sections">
              {sections.map((section) => (
                <section className="catalog-section" key={section.title}>
                  <div className="section-title">
                    <span className="section-title__icon" aria-hidden="true">
                      {section.icon}
                    </span>
                    <h2>{section.title}</h2>
                  </div>

                  <div className="product-grid">
                    {section.products.map((product) => (
                      <ProductCardView key={product.id} product={product} />
                    ))}
                  </div>
                </section>
              ))}
            </div>
          </section>
        </main>
      ) : (
        <main className="auth-page">
          <section className="auth-shell">
            <button type="button" className="back-link" onClick={goHome}>
              &larr; Volver al inicio
            </button>

            <section className="auth-card" aria-live="polite">
              <div className="auth-card__brand">
                <AppLogo />
              </div>

              {mode === 'login' ? (
                <>
                  <h1>Iniciar sesión</h1>
                  <p className="subtitle">Accede a tu cuenta de OptimaPC</p>

                  <form className="auth-form" onSubmit={onLoginSubmit} noValidate>
                    <label htmlFor="login-email">Correo electrónico</label>
                    <input
                      id="login-email"
                      type="email"
                      placeholder="ejemplo@correo.com"
                      value={loginData.email}
                      onChange={(event) =>
                        setLoginData((previous) => ({
                          ...previous,
                          email: event.target.value,
                        }))
                      }
                    />
                    {fieldErrors.email ? <p className="field-error">{fieldErrors.email}</p> : null}

                    <label htmlFor="login-password">Contraseña</label>
                    <input
                      id="login-password"
                      type="password"
                      placeholder="********"
                      value={loginData.password}
                      onChange={(event) =>
                        setLoginData((previous) => ({
                          ...previous,
                          password: event.target.value,
                        }))
                      }
                    />
                    {fieldErrors.password ? (
                      <p className="field-error">{fieldErrors.password}</p>
                    ) : null}

                    <a className="inline-link" href="#">
                      ¿Olvidaste tu contraseña?
                    </a>

                    <button type="submit" disabled={loading}>
                      {loading ? 'Validando...' : 'Iniciar sesión'}
                    </button>
                  </form>

                  <p className="mode-link">
                    No tienes cuenta?{' '}
                    <button type="button" onClick={() => switchMode('register')}>
                      Regístrate aquí
                    </button>
                  </p>
                </>
              ) : (
                <>
                  <h1>Crear cuenta</h1>
                  <p className="subtitle">Únete a OptimaPC hoy mismo</p>

                  <form className="auth-form" onSubmit={onRegisterSubmit} noValidate>
                    <label htmlFor="register-nombre">Nombre</label>
                    <input
                      id="register-nombre"
                      type="text"
                      placeholder="Tu nombre"
                      value={registerData.nombre}
                      onChange={(event) =>
                        setRegisterData((previous) => ({
                          ...previous,
                          nombre: event.target.value,
                        }))
                      }
                    />
                    {fieldErrors.nombre ? <p className="field-error">{fieldErrors.nombre}</p> : null}

                    <label htmlFor="register-apellidos">Apellidos</label>
                    <input
                      id="register-apellidos"
                      type="text"
                      placeholder="Tus apellidos"
                      value={registerData.apellidos}
                      onChange={(event) =>
                        setRegisterData((previous) => ({
                          ...previous,
                          apellidos: event.target.value,
                        }))
                      }
                    />
                    {fieldErrors.apellidos ? (
                      <p className="field-error">{fieldErrors.apellidos}</p>
                    ) : null}

                    <label htmlFor="register-email">Correo electrónico</label>
                    <input
                      id="register-email"
                      type="email"
                      placeholder="ejemplo@correo.com"
                      value={registerData.email}
                      onChange={(event) =>
                        setRegisterData((previous) => ({
                          ...previous,
                          email: event.target.value,
                        }))
                      }
                    />
                    {fieldErrors.email ? <p className="field-error">{fieldErrors.email}</p> : null}

                    <label htmlFor="register-password">Contraseña</label>
                    <input
                      id="register-password"
                      type="password"
                      placeholder="Mínimo 8 caracteres"
                      value={registerData.password}
                      onChange={(event) =>
                        setRegisterData((previous) => ({
                          ...previous,
                          password: event.target.value,
                        }))
                      }
                    />
                    {passwordStrength ? <PasswordStrengthMeter strength={passwordStrength} /> : null}
                    {fieldErrors.password ? <p className="field-error">{fieldErrors.password}</p> : null}

                    <label htmlFor="register-confirm-password">Confirmar contraseña</label>
                    <input
                      id="register-confirm-password"
                      type="password"
                      placeholder="Repite tu contraseña"
                      value={registerData.confirmPassword}
                      onChange={(event) =>
                        setRegisterData((previous) => ({
                          ...previous,
                          confirmPassword: event.target.value,
                        }))
                      }
                    />
                    {fieldErrors.confirmPassword ? (
                      <p className="field-error">{fieldErrors.confirmPassword}</p>
                    ) : null}

                    <button type="submit" disabled={loading}>
                      {loading ? 'Creando cuenta...' : 'Crear cuenta'}
                    </button>
                  </form>

                  <p className="mode-link">
                    Ya tienes cuenta?{' '}
                    <button type="button" onClick={() => switchMode('login')}>
                      Inicia sesión
                    </button>
                  </p>
                </>
              )}

              {globalError ? <p className="global-error">{globalError}</p> : null}
              {successMessage ? <p className="success-message">{successMessage}</p> : null}
            </section>
          </section>
        </main>
      )}
    </div>
  )
}

export default App
