import { useEffect, useRef, useState } from 'react'
import { Bot } from 'lucide-react'

import { API_BASE_URL } from '@/api/client'

interface MensajeHistorial {
  rol: string
  contenido: string
}

interface Mensaje {
  id: number
  rol: 'user' | 'assistant'
  contenido: string
}

const SUGERENCIAS = [
  '¿Qué premontado me recomiendas para gaming con 800€?',
  '¿Cuál es la diferencia entre los premontados de gaming?',
  '¿Cómo funciona el sistema de valoraciones?',
  '¿Cómo añado un premontado a favoritos?',
]

export function ChatbotPage() {
  const [mensajes, setMensajes] = useState<Mensaje[]>([])
  const [input, setInput] = useState('')
  const [cargando, setCargando] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [mensajes, cargando])

  const enviarMensaje = async (texto: string = input) => {
    const msg = texto.trim()
    if (!msg || cargando) return

    const nuevosMensajes: Mensaje[] = [
      ...mensajes,
      { id: Date.now(), rol: 'user', contenido: msg },
    ]
    setMensajes(nuevosMensajes)
    setInput('')
    setCargando(true)

    const historial: MensajeHistorial[] = mensajes.map((m) => ({
      rol: m.rol,
      contenido: m.contenido,
    }))

    try {
      const response = await fetch(`${API_BASE_URL}/api/catalogo/premontados/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ mensaje: msg, historial }),
      })

      const data = (await response.json()) as { respuesta: string }
      setMensajes((prev) => [
        ...prev,
        {
          id: Date.now() + 1,
          rol: 'assistant',
          contenido: response.ok
            ? data.respuesta
            : 'Lo siento, el asistente no está disponible en este momento.',
        },
      ])
    } catch {
      setMensajes((prev) => [
        ...prev,
        {
          id: Date.now() + 1,
          rol: 'assistant',
          contenido: 'No se pudo conectar con el asistente. Comprueba tu conexión.',
        },
      ])
    } finally {
      setCargando(false)
      inputRef.current?.focus()
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      void enviarMensaje()
    }
  }

  return (
    <div className="chat-page">
      <div className="chat-container">
        <div className="chat-header">
          <div className="chat-header__info">
            <h1 className="chat-header__title">
              <Bot size={24} strokeWidth={1.75} aria-hidden="true" />
              Asistente OptimaPc
            </h1>
            <p className="chat-header__subtitle">
              Pregúntame sobre premontados o cómo usar la web
            </p>
          </div>
          {mensajes.length > 0 && (
            <button
              type="button"
              className="chat-clear"
              onClick={() => setMensajes([])}
            >
              Nueva conversación
            </button>
          )}
        </div>

        <div className="chat-messages" role="log" aria-live="polite">
          {mensajes.length === 0 && !cargando && (
            <div className="chat-empty">
              <p className="chat-empty__title">¿En qué puedo ayudarte?</p>
              <div className="chat-suggestions">
                {SUGERENCIAS.map((s) => (
                  <button
                    key={s}
                    type="button"
                    className="chat-suggestion"
                    onClick={() => void enviarMensaje(s)}
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>
          )}

          {mensajes.map((msg) => (
            <div key={msg.id} className={`chat-message chat-message--${msg.rol}`}>
              {msg.rol === 'assistant' && (
                <span className="chat-avatar" aria-hidden="true"><Bot size={20} strokeWidth={1.75} /></span>
              )}
              <div className="chat-bubble">{msg.contenido}</div>
            </div>
          ))}

          {cargando && (
            <div className="chat-message chat-message--assistant">
              <span className="chat-avatar" aria-hidden="true"><Bot size={20} strokeWidth={1.75} /></span>
              <div className="chat-bubble chat-bubble--typing" aria-label="El asistente está escribiendo">
                <span /><span /><span />
              </div>
            </div>
          )}

          <div ref={bottomRef} />
        </div>

        <div className="chat-input-bar">
          <input
            ref={inputRef}
            type="text"
            className="chat-input"
            placeholder="Escribe tu pregunta..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            disabled={cargando}
            aria-label="Mensaje para el asistente"
          />
          <button
            type="button"
            className="chat-send"
            onClick={() => void enviarMensaje()}
            disabled={!input.trim() || cargando}
          >
            Enviar
          </button>
        </div>
      </div>
    </div>
  )
}
