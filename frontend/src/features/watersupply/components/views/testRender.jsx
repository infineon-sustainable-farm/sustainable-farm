import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'

/**
 * Rendu d'une vue du module avec un client React Query neuf.
 *
 * Pourquoi un client par test : le client partage de l'application conserve son cache entre les
 * tests (meme cle = meme donnee), ce qui masquerait les appels API que les tests verifient
 * (« la liste est rechargee apres une creation »). Ici chaque rendu part d'un cache vide.
 *
 * Les tests importent cette fonction sous le nom `render` : `import { renderView as render }`.
 */
export function renderView(ui) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: 0, gcTime: 0 } },
  })
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>)
}
