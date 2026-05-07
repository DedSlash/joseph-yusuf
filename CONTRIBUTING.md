# Stratégie de branches Git

## Branches permanentes

| Branche | Rôle | Protection |
|---------|------|------------|
| `main` | Production | PR requise + Quality Gate + review manuelle |
| `develop` | Intégration continue | PR requise + Quality Gate |

## Branches temporaires

| Pattern | Usage | Durée |
|---------|-------|-------|
| `phase/2-alerts` | Jalon complet | Jusqu'à merge dans develop |
| `phase/2-reports` | Jalon complet | Jusqu'à merge dans develop |
| `phase/2-payments` | Jalon complet | Jusqu'à merge dans develop |
| `feature/xxx` | Feature isolée | < 1 semaine |
| `hotfix/xxx` | Correction urgente prod | < 1 jour |

## Flow

```
feature/xxx → develop → PR → Quality Gate → merge
phase/2-xxx → develop → PR → Quality Gate → merge
develop → main → PR → Quality Gate + review manuelle → merge → deploy
```

## Pipeline CI/CD

Chaque PR déclenche automatiquement :
1. Build + Tests unitaires
2. Analyse SonarQube (coverage, bugs, vulnérabilités)
3. Quality Gate : la PR est bloquée si le gate échoue
4. Commentaire automatique sur la PR avec liens vers les rapports

## Règles

- Ne pas push directement sur `main` ou `develop`
- Les tests doivent passer avant le merge
- Coverage minimum : 70% (backend), 60% (frontend)
- Aucune vulnérabilité de sécurité tolérée (Security rating A)
