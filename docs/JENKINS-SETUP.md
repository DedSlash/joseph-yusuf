# Jenkins & SonarQube — Installation locale

> Jenkins et SonarQube tournent sur la machine de développement (local).
> Le VPS Hetzner n'héberge que l'application en production.
> À terme, Jenkins/SonarQube pourront migrer vers un VPS dédié séparé.

---

## Architecture CI/CD

```
┌─────────────────────────────────────────────────────┐
│  MACHINE LOCALE (dev)                               │
│                                                     │
│  Jenkins (port 8090) ──► Build, Test, Analyse       │
│  SonarQube (port 9000) ──► Quality Gate             │
│                                                     │
│  Tunnel (ngrok/Cloudflare) ◄── Webhooks GitHub      │
└──────────────────────────┬──────────────────────────┘
                           │ SSH + Ansible
                           ▼
┌─────────────────────────────────────────────────────┐
│  VPS HETZNER (production)                           │
│                                                     │
│  docker-compose.prod.yml                            │
│  PostgreSQL, Redis, Kafka, microservices, frontend  │
│  Nginx reverse proxy + Let's Encrypt               │
└─────────────────────────────────────────────────────┘
```

---

## 0. Variables d'environnement requises

Copier `.env.example` → `.env` et renseigner les valeurs :

| Variable | Description | Exemple local |
|----------|-------------|---------------|
| `JENKINS_URL` | URL d'accès Jenkins | `http://localhost:8090` |
| `JENKINS_WEBHOOK_URL` | URL publique pour les webhooks GitHub | `https://xxx.ngrok-free.app/github-webhook/` |
| `SONARQUBE_URL` | URL d'accès SonarQube | `http://localhost:9000` |
| `SONARQUBE_TOKEN` | Token d'analyse SonarQube | `sqa_xxx...` |
| `SONAR_DB_PASSWORD` | Mot de passe PostgreSQL de SonarQube | *(secret)* |
| `VPS_HOST` | IP ou domaine du VPS prod | *(variable d'env, jamais en dur)* |
| `VPS_USER` | User SSH de déploiement | `deploy` |
| `SSH_KEY_PATH` | Chemin vers la clé SSH privée | `~/.ssh/id_rsa` |

---

## 1. Installer Jenkins en local

### Option A — Docker (recommandé)

```bash
docker run -d --name jenkins \
  -p 8090:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts
```

### Option B — Package système

```bash
# Ubuntu/Debian
sudo apt install openjdk-17-jre
wget -q -O - https://pkg.jenkins.io/debian/jenkins.io-2023.key | sudo tee /usr/share/keyrings/jenkins-keyring.asc
echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian binary/" | sudo tee /etc/apt/sources.list.d/jenkins.list
sudo apt update && sudo apt install jenkins
```

### Premiers pas

1. Récupérer le mot de passe initial : `docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword`
2. Ouvrir `http://localhost:8090`
3. Installer les plugins suggérés + ceux listés en §2

---

## 2. Plugins Jenkins à installer

| Plugin | Rôle |
|--------|------|
| workflow-aggregator | Pipeline (Declarative + Scripted) |
| git | Clone des repos |
| github-branch-source | Multibranch Pipeline + webhooks GitHub |
| sonar | SonarQube Scanner for Jenkins |
| jacoco | Rapports de couverture JaCoCo |
| docker-workflow | Docker Pipeline (build & push images) |
| ansible | Exécution de playbooks Ansible |
| blueocean | Interface moderne Blue Ocean |
| pipeline-utility-steps | Utilitaires pipeline (readJSON, etc.) |
| ssh-agent | SSH Agent pour déploiement |
| timestamper | Timestamps dans les logs |
| ws-cleanup | Nettoyage workspace (cleanWs) |

### Installation en une commande

```bash
docker exec jenkins jenkins-plugin-cli --plugins \
  workflow-aggregator git github-branch-source sonar jacoco \
  docker-workflow ansible blueocean pipeline-utility-steps \
  ssh-agent timestamper ws-cleanup

docker restart jenkins
```

---

## 3. Credentials Jenkins à créer

**Jenkins → Manage Jenkins → Credentials → System → Global credentials**

| ID | Type | Description | Valeur |
|----|------|-------------|--------|
| `sonar-host-url` | Secret text | URL SonarQube | `${SONARQUBE_URL}` |
| `sonar-token` | Secret text | Token d'analyse SonarQube | *(généré par setup-sonar.sh)* |
| `github_token` | Secret text | GitHub Personal Access Token | *(PAT GitHub)* |
| `registry-url` | Secret text | URL du registry Docker | *(ex: ghcr.io/ton-user)* |
| `registry-user` | Secret text | Username registry Docker | *(ton username)* |
| `registry-pass` | Secret text | Password/token registry | *(token d'accès)* |
| `vps-host` | Secret text | IP ou domaine du VPS | *(jamais en dur)* |
| `vps-user` | Secret text | User SSH pour déploiement | `deploy` |
| `vps-ssh-key` | SSH Username with private key | Clé SSH privée | *(clé privée ed25519)* |
| `jwt-secret` | Secret text | Secret JWT | *(chaîne 64+ chars)* |
| `db-password` | Secret text | Mot de passe PostgreSQL prod | *(mot de passe fort)* |

---

## 4. SonarQube — Installation locale

### 4.1 Lancer SonarQube

```bash
docker compose -f docker-compose.sonar.yml up -d
```

Cela démarre :
- `sonar_db` : PostgreSQL dédié à SonarQube
- `sonarqube` : instance SonarQube sur le port 9000

### 4.2 Premier login et changement de mot de passe

1. Ouvrir `http://localhost:9000`
2. Login : `admin` / `admin`
3. **Obligatoire** : changer le mot de passe immédiatement

### 4.3 Exécuter le script de setup

```bash
cd sonar/
chmod +x setup-sonar.sh
./setup-sonar.sh http://localhost:9000 <NOUVEAU_MOT_DE_PASSE_ADMIN>
```

Le script crée le Quality Gate, les projets, et génère le token d'analyse.

### 4.4 Récupérer le token

Le script affiche :

```
=== TOKEN D'ANALYSE ===
sqa_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
========================
```

→ Ajouter ce token dans Jenkins comme credential `sonar-token`
→ Ajouter dans `.env` : `SONARQUBE_TOKEN=sqa_xxx...`

---

## 5. Lien Jenkins ↔ SonarQube

### Configuration dans Jenkins

1. **Manage Jenkins → System → SonarQube servers**
2. Cocher : ☑ Environment variables
3. Ajouter :

| Champ | Valeur |
|-------|--------|
| Name | `SonarQube` |
| Server URL | `http://localhost:9000` *(ou `${SONARQUBE_URL}`)* |
| Server authentication token | *(credential `sonar-token`)* |

> **Note** : Jenkins et SonarQube tournent tous les deux en local.
> Si Jenkins est en Docker, utiliser `http://host.docker.internal:9000`
> ou mettre les deux sur le même réseau Docker.

### Configuration SonarQube Scanner (outil)

1. **Manage Jenkins → Tools → SonarQube Scanner installations**
2. Name : `SonarScanner` — ☑ Install automatically

---

## 6. Webhook GitHub → Jenkins local (tunnel requis)

Jenkins local n'est pas exposé sur Internet. Un tunnel est **obligatoire**
pour recevoir les webhooks GitHub.

### Option A — ngrok

```bash
# Installer ngrok : https://ngrok.com/download
ngrok http 8090

# Copier l'URL publique (ex: https://abc123.ngrok-free.app)
# → Ajouter dans .env : JENKINS_WEBHOOK_URL=https://abc123.ngrok-free.app/github-webhook/
```

### Option B — Cloudflare Tunnel (recommandé pour la stabilité)

```bash
# Installer cloudflared
cloudflared tunnel create jenkins-local
cloudflared tunnel route dns jenkins-local jenkins.your-domain.com
cloudflared tunnel run jenkins-local

# → JENKINS_WEBHOOK_URL=https://jenkins.your-domain.com/github-webhook/
```

### Configurer le webhook sur GitHub

1. GitHub → Repository → **Settings → Webhooks → Add webhook**
2. Remplir :

| Champ | Valeur |
|-------|--------|
| Payload URL | `${JENKINS_WEBHOOK_URL}` *(URL du tunnel)* |
| Content type | `application/json` |
| Secret | *(optionnel mais recommandé)* |
| SSL verification | ☑ Enable |

3. Events : ☑ Pushes, ☑ Pull requests
4. **Add webhook** → vérifier le ping (✓ 200 OK)

### Attention

- L'URL ngrok change à chaque redémarrage (sauf plan payant) → mettre à jour le webhook
- Cloudflare Tunnel fournit une URL stable (recommandé)
- Le tunnel doit être actif pour que les webhooks arrivent

---

## 7. Protection des branches GitHub

### Branche `main`

1. GitHub → Repository → **Settings → Branches → Add branch protection rule**
2. Branch name pattern : `main`
3. Règles :

| Règle | Valeur |
|-------|--------|
| ☑ Require a pull request before merging | |
| → Require approvals | 1 (ou 0 si solo) |
| ☑ Require status checks to pass before merging | |
| → Status checks : | *(nom du check Jenkins)* |
| ☑ Require branches to be up to date before merging | |
| ☑ Do not allow bypassing the above settings | |
| ☐ Allow force pushes | **NON** |
| ☐ Allow deletions | **NON** |

---

## 8. Vérification end-to-end

### Pré-requis

- [ ] Jenkins local démarré et accessible sur `http://localhost:8090`
- [ ] SonarQube local démarré et accessible sur `http://localhost:9000`
- [ ] Tunnel actif (ngrok ou Cloudflare)
- [ ] Webhook GitHub configuré avec l'URL du tunnel
- [ ] Clé SSH locale peut se connecter au VPS : `ssh ${VPS_USER}@${VPS_HOST}`

### Test du pipeline

```bash
# 1. Créer une branche de test
git checkout -b feature/test-webhook
git commit --allow-empty -m "test: verify Jenkins webhook trigger"
git push origin feature/test-webhook

# 2. Créer une PR
gh pr create --title "test: webhook verification" \
  --body "PR de test pour valider le pipeline CI" \
  --base phase/1

# 3. Vérifier
#    → Jenkins détecte la PR via le webhook
#    → Build démarre (< 30 secondes si tunnel actif)
#    → SonarQube reçoit l'analyse
#    → Quality Gate PASSED/FAILED commenté sur la PR

# 4. Nettoyage
git checkout phase/1
git branch -d feature/test-webhook
git push origin --delete feature/test-webhook
```

### Checklist de validation

- [ ] Jenkins local démarre sans erreur
- [ ] SonarQube local est UP (`curl http://localhost:9000/api/system/status`)
- [ ] Tous les plugins Jenkins installés
- [ ] Tous les credentials créés avec les bons IDs
- [ ] Quality Gate custom actif dans SonarQube
- [ ] Token SonarQube configuré dans Jenkins
- [ ] Tunnel actif et webhook GitHub → 200 OK au ping
- [ ] Push déclenche un build Jenkins
- [ ] SonarQube reçoit l'analyse
- [ ] Commentaire Quality Gate apparaît sur la PR
- [ ] Déploiement via SSH/Ansible vers le VPS fonctionne

---

## 9. Déploiement vers le VPS (inchangé)

Le pipeline Jenkins local déploie vers le VPS via SSH + Ansible.
Aucun changement n'est requis côté VPS.

```
Jenkins (local) ──SSH──► VPS Hetzner
                         │
                         ├── docker compose pull (images du registry)
                         ├── docker compose up -d
                         └── healthcheck
```

Le Jenkinsfile utilise `sshagent(credentials: ['vps-ssh-key'])` pour
se connecter au VPS et exécuter le playbook Ansible.

**Pré-requis** :
- La clé SSH publique correspondant à `vps-ssh-key` doit être dans
  `~deploy/.ssh/authorized_keys` sur le VPS
- Le VPS doit accepter les connexions SSH sur le port 22
- Ansible doit être installé sur la machine locale (ou dans le conteneur Jenkins)

---

## Dépannage rapide

| Problème | Cause probable | Solution |
|----------|---------------|----------|
| Webhook timeout | Tunnel inactif | Relancer ngrok/cloudflared |
| Webhook 403 | CSRF Jenkins | Activer "GitHub hook trigger" dans le job |
| SonarQube "Not authorized" | Token invalide | Regénérer dans SonarQube → mettre à jour credential |
| Quality Gate "NONE" | Webhook SonarQube manquant | SonarQube → Administration → Webhooks → `http://localhost:8090/sonarqube-webhook/` |
| Build ne démarre pas | Branch filter | Vérifier le pattern include du multibranch |
| Docker build échoue | Docker socket | Monter `/var/run/docker.sock` dans le conteneur Jenkins |
| Ansible "permission denied" | Clé SSH | Vérifier `vps-ssh-key` et `authorized_keys` sur le VPS |
| Jenkins ne voit pas SonarQube | Réseau Docker | Utiliser `http://host.docker.internal:9000` si Jenkins est en Docker |

---

## Résumé de l'ordre d'exécution

```
1. Copier .env.example → .env et renseigner les valeurs
2. Lancer SonarQube local : docker compose -f docker-compose.sonar.yml up -d
3. Installer/démarrer Jenkins local
4. Installer les plugins Jenkins → redémarrer
5. Créer tous les credentials dans Jenkins
6. Changer mot de passe admin SonarQube
7. Exécuter setup-sonar.sh → récupérer le token
8. Configurer SonarQube dans Jenkins (serveur + outil)
9. Créer les 2 jobs Jenkins (PR + Deploy)
10. Démarrer le tunnel (ngrok ou Cloudflare)
11. Configurer le webhook GitHub avec l'URL du tunnel
12. Configurer la protection des branches
13. Test end-to-end avec feature/test-webhook
```

---

## Migration future vers un VPS dédié

Quand Jenkins/SonarQube migreront vers un VPS séparé :

1. Mettre à jour `JENKINS_URL` et `SONARQUBE_URL` dans `.env`
2. Mettre à jour `JENKINS_WEBHOOK_URL` (plus besoin de tunnel)
3. Mettre à jour les credentials `sonar-host-url` dans Jenkins
4. Le reste (Jenkinsfile, Ansible, docker-compose.prod.yml) reste inchangé
