# CI intelligente : auto-review + auto-merge des PR `*/init`

Objectif : plus jamais de revue manuelle branche par branche. La CI relit
chaque PR vers `develop`, publie un rapport, et merge seule les PR autorisées.

## Pipeline

```
PR (opened/push/reopen) vers develop
│
├── Workflow "CI"                          (checks requis)
│   ├── Backend tests (PostgreSQL)   ← tests sur VRAI Postgres (plus de H2 muet)
│   ├── Flyway migrations (PostgreSQL) ← migrations appliquées sur base vierge
│   └── Frontend build
│
└── Workflow "Auto-review"
    └── Quality gate (auto-review)    (check requis, verdict du merge)
        ├── scripts/audit-shared-files.sh --cross-check
        │     conflits de merge réels, fichiers partagés divergents/supprimés,
        │     convention endpoints module-local, conflits croisés entre branches
        ├── scripts/ci-guards.sh  (sur le DIFF : la dette existante ne bloque pas)
        └── Rapport publié en commentaire de la PR (mis à jour à chaque push)
            │
            ├── 🔴 violation bloquante → check rouge, le rapport dit QUOI corriger
            └── 🟢/🟡 passée
                └── auto-merge (job) : si PR créée par PANK4SS ET branche */init
                    → gh pr merge --squash --auto --delete-branch
                    → merge dès que TOUS les checks requis sont verts
```

## Garde-fous bloquants (`scripts/ci-guards.sh`)

| ID | Motif | Référence review |
|----|-------|------------------|
| F1 | `@CrossOrigin` ajouté | PR-26/02 |
| F2 | URL/IP en dur (`localhost`, `10.x`, `192.168.x`) dans le code | PR-26/07, PR-42 |
| F3 | hash/secret dans une migration Flyway | PR-33/01 |
| F4 | comptes/données démo en migration | PR-33/01 |
| F5 | starter supprimé ou `java.version` baissé dans `pom.xml` | PR-42/02, PR-29/04 |
| F6 | entité JPA liée au `@RequestBody` (mass assignment) | PR-26/04, PR-27/02 |
| F7 | `@ManyToOne` sans `LAZY` | PR-43/06 |
| F8 | `CommandLineRunner` sans `@Profile` | PR-26/01 |
| F9 | second `@RestControllerAdvice` sans `@Order` | PR-42/06 |
| F10 | `@Id` sans `@GeneratedValue` (PK contrôlée par le client) | PR-43/04 |
| F11 | secret littéral codé en dur | PR-33 |

Avertissements (jaunes, non bloquants) : `findAll()` non borné (W1), entité
sans `@Version` (W2), exception brute → 500 (W3), `fetch()` hors couche API (W4).

## Setup GitHub (une seule fois)

1. **Settings → General → Pull Requests** : cocher **Allow auto-merge** et
   *Delete head branch* (déjà passé par le workflow via `--delete-branch`).
2. **Settings → Branches → Branch protection rule** sur `develop` :
   - Require a pull request before merging
   - Require status checks to pass :
     - `Quality gate (auto-review)`
     - `Backend tests (PostgreSQL)`
     - `Flyway migrations (PostgreSQL)`
     - `Frontend build`
   - Require branches to be up to date before merging
   - Ne **pas** exiger d'approbations pour les PR `*/init` (GitHub interdit à
     l'auteur d'approuver sa propre PR — le rôle de "review" est tenu par le
     check `Quality gate`).
3. Modifier l'identité/suffixe autorisés : `AUTO_REVIEW_LOGIN` et le `if`
   du job `auto-merge` dans `.github/workflows/auto-review.yml` (deux
   endroits, le contexte `env` n'étant pas disponible dans un `if` de job).

## Limites assumées (revue humaine réduite, pas nulle)

Le rapport de la PR rappelle en permanence les points non automatisables :
sémantique métier (invariants, volumes réels vs planifiés, portes QC),
pertinence des tests, idempotence d'ingestion, sémantique PUT. La CI est un
filtre à défauts connus, pas un relecteur fonctionnel.

## Étendre les gardes

Ajouter le motif dans `scripts/ci-guards.sh` (fonction `fail`/`warn`),
documenter dans le tableau ci-dessus. Les guards ne voient que le diff
`merge-base..head` : un défaut préexistant sur la base n'est jamais signalé.
