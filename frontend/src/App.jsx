import { useState, useEffect } from 'react'
import { WaterSupplyApp } from './features/watersupply/components/WaterSupplyApp'
import { LoginForm } from './features/watersupply/components/LoginForm'
import { useAuth } from './shared/hooks/useAuth'
import { getToken } from './shared/api/client'

function App() {
  const [loggedIn, setLoggedIn] = useState(false)
  const [checking, setChecking] = useState(true)
  const { user, logout } = useAuth()

  // Check if a token already exists on mount
  useEffect(() => {
    const token = getToken()
    if (token) {
      setLoggedIn(true)
    }
    setChecking(false)
  }, [])

  const handleLogin = (response) => {
    setLoggedIn(true)
  }

  const handleLogout = () => {
    logout()
    setLoggedIn(false)
  }

  if (checking) {
    return <div style={{ display: 'grid', placeItems: 'center', minHeight: '100vh', fontFamily: 'Inter, sans-serif', color: '#6b7a78' }}>Loading...</div>
  }

  return (
    <>
      {loggedIn ? (
        <WaterSupplyApp user={user} onLogout={handleLogout} />
      ) : (
        <LoginForm onLogin={handleLogin} />
      )}
    </>
  )
}

export default App