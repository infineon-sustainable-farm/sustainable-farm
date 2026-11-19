import { createBrowserRouter } from 'react-router-dom'
import { collectModules } from './modules'
import { buildAppRoutes } from './routes'

const files = import.meta.glob('../features/*/routes.jsx', { eager: true })

export const router = createBrowserRouter(buildAppRoutes(collectModules(files)))
