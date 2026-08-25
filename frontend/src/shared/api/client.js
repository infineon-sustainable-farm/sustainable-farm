/**
 * Client API partagé - wrapper autour de fetch pour communiquer avec le backend Spring Boot.
 * Gère automatiquement le token JWT dans le localStorage.
 */

const API_BASE = '/api'

function getToken() {
  return localStorage.getItem('access_token')
}

function setToken(token) {
  if (token) {
    localStorage.setItem('access_token', token)
  } else {
    localStorage.removeItem('access_token')
  }
}

function getHeaders() {
  const headers = {
    'Content-Type': 'application/json',
  }
  const token = getToken()
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  return headers
}

async function request(method, path, body = null) {
  const options = {
    method,
    headers: getHeaders(),
  }
  if (body) {
    options.body = JSON.stringify(body)
  }

  const response = await fetch(`${API_BASE}${path}`, options)

  if (!response.ok) {
    let errorData
    try {
      errorData = await response.json()
    } catch {
      errorData = { message: response.statusText }
    }
    const error = new Error(errorData.message || `HTTP ${response.status}`)
    error.status = response.status
    error.data = errorData
    throw error
  }

  // Handle empty responses (e.g., 204 No Content)
  const text = await response.text()
  if (!text) {
    return null
  }
  return JSON.parse(text)
}

export const apiClient = {
  get: (path) => request('GET', path),
  post: (path, body) => request('POST', path, body),
  put: (path, body) => request('PUT', path, body),
  delete: (path) => request('DELETE', path),
  patch: (path, body) => request('PATCH', path, body),
}

export { getToken, setToken }
