import { useState } from 'react'
import { useAuth } from '../../../shared/hooks/useAuth'
import '../watersupply.css'

/**
 * Formulaire de connexion - communique avec le backend via /api/auth/login.
 * Styled to match the Water Supply Management design system.
 */
export function LoginForm({ onLogin }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const { login, loading, error } = useAuth()

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      const response = await login(email, password)
      onLogin?.(response)
    } catch (err) {
      // L'erreur est gérée par le hook useAuth
      console.error('Login failed:', err)
    }
  }

  return (
    <div style={{
      minHeight: '100vh',
      display: 'grid',
      placeItems: 'center',
      background: 'var(--ws-bg, #f6f8f8)',
      fontFamily: "'Inter', Arial, sans-serif",
      padding: '20px',
    }}>
      <div style={{
        width: '100%',
        maxWidth: '420px',
        background: '#fff',
        border: '1px solid var(--ws-line, #e2e9e7)',
        borderRadius: '12px',
        padding: '40px 36px',
        boxShadow: '0 8px 30px rgba(10,130,118,.08)',
      }}>
        <div style={{
          width: '80px',
          height: '80px',
          borderRadius: '50%',
          background: 'var(--ws-primary-tint, #e3f2ef)',
          display: 'grid',
          placeItems: 'center',
          fontSize: '36px',
          margin: '0 auto 20px',
        }}>
          💧
        </div>
        <h1 style={{
          fontFamily: "'Montserrat', Arial, sans-serif",
          fontSize: '22px',
          fontWeight: 800,
          color: 'var(--ws-primary, #0a8276)',
          textAlign: 'center',
          margin: '0 0 6px',
        }}>
          Water Supply Management
        </h1>
        <p style={{
          color: 'var(--ws-muted, #6b7a78)',
          fontSize: '13.5px',
          textAlign: 'center',
          margin: '0 0 28px',
          lineHeight: 1.5,
        }}>
          Sign in to access your water supply dashboard
        </p>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <label style={{
              display: 'block',
              fontSize: '11.5px',
              fontWeight: 800,
              letterSpacing: '1px',
              textTransform: 'uppercase',
              color: 'var(--ws-muted, #6b7a78)',
              marginBottom: '6px',
            }}>
              Email
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              style={{
                width: '100%',
                padding: '11px 14px',
                border: '1px solid var(--ws-line, #e2e9e7)',
                borderRadius: '8px',
                fontSize: '14px',
                fontFamily: 'inherit',
                outline: 'none',
                transition: 'border-color .2s',
              }}
              onFocus={(e) => e.target.style.borderColor = 'var(--ws-primary, #0a8276)'}
              onBlur={(e) => e.target.style.borderColor = 'var(--ws-line, #e2e9e7)'}
              required
            />
          </div>
          <div>
            <label style={{
              display: 'block',
              fontSize: '11.5px',
              fontWeight: 800,
              letterSpacing: '1px',
              textTransform: 'uppercase',
              color: 'var(--ws-muted, #6b7a78)',
              marginBottom: '6px',
            }}>
              Password
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              style={{
                width: '100%',
                padding: '11px 14px',
                border: '1px solid var(--ws-line, #e2e9e7)',
                borderRadius: '8px',
                fontSize: '14px',
                fontFamily: 'inherit',
                outline: 'none',
                transition: 'border-color .2s',
              }}
              onFocus={(e) => e.target.style.borderColor = 'var(--ws-primary, #0a8276)'}
              onBlur={(e) => e.target.style.borderColor = 'var(--ws-line, #e2e9e7)'}
              required
            />
          </div>

          {error && (
            <p style={{
              color: 'var(--ws-red, #c62828)',
              fontSize: '12.5px',
              background: 'var(--ws-red-tint, #fbeaea)',
              padding: '9px 12px',
              borderRadius: '8px',
              margin: 0,
            }}>
              {error.message || 'Invalid email or password. Please try again.'}
            </p>
          )}

          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%',
              padding: '12px',
              background: 'var(--ws-primary, #0a8276)',
              color: '#fff',
              border: 'none',
              borderRadius: '8px',
              fontSize: '14px',
              fontWeight: 800,
              fontFamily: 'inherit',
              cursor: 'pointer',
              transition: 'filter .2s',
              marginTop: '4px',
            }}
            onMouseEnter={(e) => e.target.style.filter = 'brightness(.94)'}
            onMouseLeave={(e) => e.target.style.filter = 'brightness(1)'}
          >
            {loading ? 'Signing in...' : 'Sign in'}
          </button>
        </form>

        <p style={{
          color: 'var(--ws-muted, #6b7a78)',
          fontSize: '12px',
          textAlign: 'center',
          margin: '20px 0 0',
        }}>
          Backend: <code style={{ color: 'var(--ws-primary-dark, #07655c)' }}>http://localhost:8080/api</code>
        </p>
      </div>
    </div>
  )
}