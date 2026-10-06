import { describe, expect, it } from 'vitest'
import routes from './routes'
import { VIEW_PATHS, WATERSUPPLY_BASE } from './paths'

/**
 * Le branchement du module dans l'application est verifie ici : c'est exactement ce qui
 * manquait (routes.jsx vide), le module etait donc absent du bundle de production.
 */
describe('routes du module watersupply', () => {
  const [moduleRoute] = routes

  it('monte le module sous /watersupply, comme machinery et plants', () => {
    expect(routes).toHaveLength(1)
    expect(WATERSUPPLY_BASE).toBe('/watersupply')
    expect(moduleRoute.path).toBe('watersupply')
  })

  it('declare un ecran par entree du menu, avec une seule route par defaut', () => {
    const declared = moduleRoute.children.filter((child) => child.path !== '*')
    const indexRoutes = declared.filter((child) => child.index === true)
    const paths = declared
      .filter((child) => child.path)
      .map((child) => child.path)
      .sort()
    const menuPaths = Object.values(VIEW_PATHS)
      .filter((relativePath) => relativePath !== '')
      .sort()

    expect(declared).toHaveLength(Object.keys(VIEW_PATHS).length)
    expect(indexRoutes).toHaveLength(1)
    expect(paths).toEqual(menuPaths)
  })

  it('encapsule chaque ecran pour lui transmettre notify et onNavigate', () => {
    // Les vues attendent ces deux proprietes : elles viennent du contexte de l'Outlet du
    // module, transmis par l'adaptateur (voir withModuleUi dans routes.jsx).
    const declared = moduleRoute.children.filter((child) => child.path !== '*')

    declared.forEach((child) => {
      expect(typeof child.element.type).toBe('function')
      expect(child.element.type.name).toBe('ModuleScreen')
    })
  })

  it('affiche un ecran "introuvable" sur une URL inconnue, sans redirection silencieuse', () => {
    const catchAll = moduleRoute.children.at(-1)

    expect(catchAll.path).toBe('*')
    expect(catchAll.element).toBeTruthy()
  })
})
