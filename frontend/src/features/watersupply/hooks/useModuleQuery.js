import { useQuery } from '@tanstack/react-query'

/**
 * Enveloppe locale de React Query pour les donnees du module.
 *
 * Pourquoi : chaque vue appelait auparavant son propre `useEffect` + `useState` de chargement,
 * avec la meme logique recopiee une dizaine de fois. React Query est deja la bibliotheque de
 * donnees du projet (machinery, plants) : elle apporte la deduplication des requetes
 * simultanees, la mise en cache par cle et le rafraichissement, sans reinventer un etat local.
 *
 * Le contrat expose aux vues reste volontairement celui d'avant — `{ data, loading, error,
 * refetch }` — afin de ne pas modifier les vues ni leurs tests. Les donnees sont considerees
 * comme fraiches (`staleTime: 0`) et les echecs ne sont pas reessayes automatiquement :
 * un ecran de supervision doit afficher l'erreur, pas masquer une panne de backend.
 */
export function useModuleQuery(queryKey, queryFn, { enabled = true } = {}) {
  const { data, isPending, error, refetch } = useQuery({
    queryKey,
    queryFn: () => queryFn(),
    enabled,
    retry: false,
    refetchOnWindowFocus: false,
    staleTime: 0,
  })

  return {
    data: data ?? null,
    // Une requete desactivee (identifiant absent) n'est pas « en chargement » : comportement
    // identique aux hooks precedents, qui laissaient `loading` a false dans ce cas.
    loading: enabled ? isPending : false,
    error: error ?? null,
    refetch,
  }
}
