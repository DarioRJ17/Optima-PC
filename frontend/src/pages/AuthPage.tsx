import type { Dispatch, FormEvent, SetStateAction } from 'react'
import { AppLogo, PasswordStrengthMeter } from '../components/common'
import type { AuthMode, LoginData, RegisterData } from '../types'

type AuthPageProps = {
  mode: AuthMode
  switchMode: (newMode: AuthMode) => void
  loginData: LoginData
  setLoginData: Dispatch<SetStateAction<LoginData>>
  registerData: RegisterData
  setRegisterData: Dispatch<SetStateAction<RegisterData>>
  passwordStrength: string | null
  fieldErrors: Record<string, string>
  globalError: string
  successMessage: string
  loading: boolean
  onLoginSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
  onRegisterSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
  goHome: () => void
}

export function AuthPage({
  mode,
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
  goHome,
}: AuthPageProps) {
  return (
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
                {fieldErrors.password ? <p className="field-error">{fieldErrors.password}</p> : null}

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
                {fieldErrors.apellidos ? <p className="field-error">{fieldErrors.apellidos}</p> : null}

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
                {fieldErrors.confirmPassword ? <p className="field-error">{fieldErrors.confirmPassword}</p> : null}

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
  )
}
