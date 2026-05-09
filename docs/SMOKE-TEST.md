# Smoke test — Joseph · Yusuf

Validation bout-en-bout de tous les jalons (0 → 9b) avec la stack lancée
localement via Docker Compose. À exécuter **dans l'ordre** : chaque section
réutilise des données créées plus haut (JWT, IDs, plans).

> ⏱️ Durée estimée : ~30 min (build inclus, ~10 min sans).

---

## 0. Prérequis et démarrage

### 0.1 Prérequis machine

- Docker ≥ 24, Docker Compose v2
- Java 17 + Maven (pour rebuild local optionnel)
- `jq`, `curl` installés
- ~6 Go de RAM libre (postgres + kafka + 8 microservices + 2 frontends)
- Comptes Stripe test (clés `sk_test_…` / `pk_test_…`) pour la section 6

### 0.2 Variables d'environnement

```bash
cp .env.example .env
# Éditer .env :
#   - DB_PASSWORD : nouveau mot de passe
#   - JWT_SECRET  : 64+ caractères aléatoires
#   - SUPER_ADMIN_EMAILS=admin@josephyusuf.com
#   - STRIPE_SECRET_KEY=sk_test_...
#   - STRIPE_WEBHOOK_SECRET=whsec_test_...
```

### 0.3 Build et démarrage

```bash
cd joseph-yusuf

# Build complet (sans tests pour aller plus vite ; les tests ont déjà
# passé via mvn verify et le pipeline CI)
mvn -DskipTests clean package

# Démarrer la stack complète + MailHog (override dev)
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build

# Suivre la santé des services
watch -n 2 'docker compose ps --format "table {{.Service}}\t{{.Status}}"'
```

✅ **Succès** : tous les services en `Up (healthy)` après ~90 s.
❌ **Échec** : `docker compose logs <service>` pour diagnostiquer. Postgres
doit démarrer en premier, puis Kafka, puis discovery-server, puis les
microservices.

### 0.4 URLs de référence

| Service              | URL                                  | Notes                          |
|----------------------|--------------------------------------|--------------------------------|
| Frontend utilisateur | http://localhost:4200                | App Angular principale         |
| Admin Frontend       | http://localhost:4201                | Dashboard admin (rôle ADMIN)   |
| Gateway              | http://localhost:8080                | Toutes les APIs `/api/**`      |
| Eureka Discovery     | http://localhost:8761                | UI de découverte de services   |
| MailHog              | http://localhost:8025                | Capture des emails dev         |
| auth-service         | http://localhost:8081/actuator/health|                                |
| income-service       | http://localhost:8082/actuator/health|                                |
| rule-engine-service  | http://localhost:8083/actuator/health|                                |
| alert-service        | http://localhost:8084/actuator/health|                                |
| report-service       | http://localhost:8085/actuator/health|                                |
| subscription-service | http://localhost:8086/actuator/health|                                |
| admin-service        | http://localhost:8087/actuator/health|                                |
| Postgres             | localhost:5432 (joseph / DB_PASSWORD)| 8 schemas `joseph_*`           |
| Kafka                | localhost:9092                       | Topics `income.classified`, …  |

### 0.5 Healthcheck global

```bash
for port in 8080 8081 8082 8083 8084 8085 8086 8087 8761; do
  printf "Port %s : " "$port"
  curl -sf "http://localhost:$port/actuator/health" | jq -r '.status' || echo "DOWN"
done
```

✅ **Succès** : tous renvoient `UP`.
❌ **Échec** : un service `DOWN` ou en erreur → consulter `docker compose
logs <service>` et vérifier la connexion postgres (`SPRING_DATASOURCE_URL`).

---

## 1. Auth (auth-service)

### 1.1 Register d'un utilisateur

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "MotDePasse123!",
    "firstName": "Joseph",
    "lastName": "Test"
  }' | jq
```

✅ **Succès** : `201 Created`, JSON contenant `accessToken`, `refreshToken`
et `user.plan = "FREE"`, `user.role = "USER"`.
❌ **Échec attendu si re-run** : `409 Conflict — Cet email est déjà utilisé`.

### 1.2 Login → récupérer le JWT

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"MotDePasse123!"}' \
  | jq -r '.accessToken')

echo "JWT longueur : ${#TOKEN}"   # ~ 200+ caractères
```

✅ **Succès** : `TOKEN` non vide.
❌ **Échec** : `401 — Identifiants invalides`.

### 1.3 Inspecter le JWT (claims)

```bash
echo "$TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | jq
```

✅ **Succès** : payload contient `sub` (email), `userId` (UUID),
`plan: "FREE"`, `role: "USER"`, `exp` (timestamp futur).

### 1.4 GET /me

```bash
curl -s http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN" | jq
```

✅ **Succès** : profil complet, `plan: "FREE"`, `role: "USER"`,
`enabled: true`.

### 1.5 Reset mot de passe (avec MailHog)

```bash
# Demander un reset (réponse silencieuse 200 même si email inconnu)
curl -i -X POST http://localhost:8080/api/auth/password/forgot \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'

# → 200 OK
```

Ouvrir MailHog : http://localhost:8025

✅ **Succès** : un email est apparu, sujet « Réinitialisation de votre
mot de passe Joseph · Yusuf », contenant un lien
`http://localhost:4200/reset-password?token=<UUID>`.
❌ **Échec** : pas d'email → vérifier `docker compose logs auth-service
| grep -i mail` (config SMTP, MailHog joignable).

```bash
# Récupérer le token depuis l'API MailHog
RESET_TOKEN=$(curl -s http://localhost:8025/api/v2/messages \
  | jq -r '.items[0].Content.Body' \
  | grep -oE 'token=[a-f0-9-]+' | head -1 | cut -d= -f2)

# Reset effectif
curl -i -X POST http://localhost:8080/api/auth/password/reset \
  -H "Content-Type: application/json" \
  -d "{\"token\":\"$RESET_TOKEN\",\"newPassword\":\"NouveauPass123!\"}"
```

✅ **Succès** : `200 OK`, login fonctionne avec le nouveau mot de passe.
❌ **Échec attendu après 15 min** : `400 — Token expiré ou invalide`.

> Pour la suite des tests, on remet le mot de passe initial via un nouveau
> reset, ou on relance directement un login avec `NouveauPass123!`.

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"NouveauPass123!"}' \
  | jq -r '.accessToken')
USER_ID=$(echo "$TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | jq -r '.userId')
echo "USER_ID=$USER_ID"
```

---

## 2. Income (income-service)

### 2.1 Créer une source de revenu (FREE → 1 max)

```bash
SOURCE_ID=$(curl -s -X POST http://localhost:8080/api/incomes/sources \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Salaire principal","type":"SALARY","currency":"XOF"}' \
  | jq -r '.id')

echo "SOURCE_ID=$SOURCE_ID"
```

✅ **Succès** : `201 Created`, UUID renvoyé.

### 2.2 Limite plan FREE : 2e source → 403

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X POST \
  http://localhost:8080/api/incomes/sources \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Freelance","type":"FREELANCE","currency":"XOF"}'
```

✅ **Succès** : `403`.
❌ **Échec** : `201` → la `PlanLimitException` n'est pas appliquée.

### 2.3 Saisir 4 mois de revenus pour générer ABUNDANCE

> Référence : 3 mois précédents = 1 000 XOF chacun → moyenne 1 000.
> Mois courant = 1 500 → +50 % → ABUNDANCE.

```bash
for m in 2 3 4; do
  curl -s -X POST http://localhost:8080/api/incomes/entries \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"incomeSourceId\":\"$SOURCE_ID\",\"amount\":1000,\"month\":$m,\"year\":2026}" \
    > /dev/null
done

# Mois courant : ABUNDANCE
curl -s -X POST http://localhost:8080/api/incomes/entries \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"incomeSourceId\":\"$SOURCE_ID\",\"amount\":1500,\"month\":5,\"year\":2026}" | jq
```

✅ **Succès** : 4 entrées créées, statut HTTP 201 à chaque fois.

### 2.4 Vérifier la classification ABUNDANCE/LEAN/NORMAL

```bash
curl -s "http://localhost:8080/api/incomes/summary?month=5&year=2026" \
  -H "Authorization: Bearer $TOKEN" | jq
```

✅ **Succès** : JSON avec `classification: "ABUNDANCE"`, `total: 1500`,
`average3Months: 1000`, `ratio` ≈ `1.5`.

```bash
# Mois NORMAL (sans données → pas de classification, ou NORMAL si
# le service projette à partir de l'historique)
curl -s "http://localhost:8080/api/incomes/summary?month=4&year=2026" \
  -H "Authorization: Bearer $TOKEN" | jq -r '.classification'
```

✅ **Succès** : `"NORMAL"` ou `null` (pas assez d'historique).

> Pour tester LEAN, créer un mois 6 à 700 XOF (-30 % vs moyenne 1 000) :
> classification = `LEAN`.

---

## 3. Rule Engine (rule-engine-service)

### 3.1 Règle 50/30/20 — autorisée FREE

```bash
curl -s -X POST http://localhost:8080/api/rules/calculate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"rule":"RULE_50_30_20","totalIncome":1500,"month":5,"year":2026}' | jq
```

✅ **Succès** : `{ "needs": 750, "wants": 450, "savings": 300 }` (50/30/20).
Statut HTTP 200.

### 3.2 Règle avancée — interdite FREE → 403

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X POST \
  http://localhost:8080/api/rules/calculate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"rule":"RULE_JOSEPH_DYNAMIC","totalIncome":1500,"month":5,"year":2026}'
```

✅ **Succès** : `403`.
❌ **Échec** : `200` → `RuleNotAccessibleException` non levée.

### 3.3 Lister les règles disponibles selon le plan

```bash
curl -s http://localhost:8080/api/rules/available \
  -H "Authorization: Bearer $TOKEN" | jq
```

✅ **Succès** : pour FREE, seul `RULE_50_30_20` est marqué `accessible:true`.

---

## 4. Alerts (alert-service)

> alert-service consomme deux topics Kafka : `income.classified` (publié
> par income-service à chaque entrée) et `rule.applied` (publié par
> rule-engine après un `/calculate`). Les sections 2 et 3 ont déjà
> déclenché plusieurs événements.

### 4.1 Vérifier la consommation Kafka

```bash
docker compose logs alert-service | grep -E "Réception|alerte créée" | tail -10
```

✅ **Succès** : plusieurs lignes
`Réception IncomeClassifiedEvent type=ABUNDANCE` et
`alerte créée pour user=$USER_ID`.

### 4.2 Lister les alertes

```bash
curl -s http://localhost:8080/api/alerts \
  -H "Authorization: Bearer $TOKEN" | jq '.[] | {type, message, read, createdAt}'
```

✅ **Succès** : au moins 2 alertes (`ABUNDANCE_DETECTED`, `RULE_APPLIED`).

### 4.3 Compteur non lues

```bash
curl -s http://localhost:8080/api/alerts/unread-count \
  -H "Authorization: Bearer $TOKEN" | jq
```

✅ **Succès** : `{ "count": <n> }` avec `n ≥ 1`.

### 4.4 Marquer une alerte comme lue

```bash
ALERT_ID=$(curl -s http://localhost:8080/api/alerts \
  -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')

curl -s -X PUT http://localhost:8080/api/alerts/$ALERT_ID/read \
  -H "Authorization: Bearer $TOKEN" | jq

# Vérifier que le compteur a baissé
curl -s http://localhost:8080/api/alerts/unread-count \
  -H "Authorization: Bearer $TOKEN" | jq
```

✅ **Succès** : alerte `read: true`, compteur décrémenté.

---

## 5. Reports (report-service)

### 5.1 Génération PDF avec plan FREE → 403

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X POST \
  http://localhost:8080/api/reports/monthly \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"month":5,"year":2026}'
```

✅ **Succès** : `403`.

### 5.2 Upgrade plan via admin dashboard (préparation section 7)

> ⚠️ Cette étape suppose que le super-admin a été créé.
> Voir section 7.1 si ce n'est pas encore le cas.

```bash
# Login admin (cf. 7.1)
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"admin@josephyusuf.com\",\"password\":\"$SUPER_ADMIN_DEFAULT_PASSWORD\"}" \
  | jq -r '.accessToken')

# Promouvoir le user test en PREMIUM
curl -s -X PUT http://localhost:8080/api/admin/users/$USER_ID/plan \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"plan":"PREMIUM"}' | jq

# Re-login user pour récupérer un JWT avec plan=PREMIUM
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"NouveauPass123!"}' \
  | jq -r '.accessToken')

echo "$TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | jq -r '.plan'
# → PREMIUM
```

✅ **Succès** : nouveau JWT contient `plan: "PREMIUM"`.

### 5.3 Génération PDF mensuel — PREMIUM

```bash
REPORT_ID=$(curl -s -X POST http://localhost:8080/api/reports/monthly \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"month":5,"year":2026}' | jq -r '.id')

echo "REPORT_ID=$REPORT_ID"
```

✅ **Succès** : `201 Created`, ID renvoyé. Logs report-service indiquent
« Rapport mensuel généré ».

### 5.4 Téléchargement PDF

```bash
curl -s -o /tmp/rapport-mai-2026.pdf \
  http://localhost:8080/api/reports/$REPORT_ID \
  -H "Authorization: Bearer $TOKEN"

file /tmp/rapport-mai-2026.pdf
# → /tmp/rapport-mai-2026.pdf: PDF document, version 1.x
```

✅ **Succès** : fichier PDF non vide (> 10 ko), ouvrable. Contient les
données du mois 5/2026, classification ABUNDANCE et la règle 50/30/20.

---

## 6. Subscriptions (subscription-service)

### 6.1 Créer un PaymentIntent Stripe (test)

```bash
PI=$(curl -s -X POST http://localhost:8080/api/subscriptions/stripe/create-payment-intent \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"plan":"PREMIUM","currency":"EUR"}')

echo "$PI" | jq
CLIENT_SECRET=$(echo "$PI" | jq -r '.clientSecret')
```

✅ **Succès** : JSON avec `clientSecret` (format `pi_..._secret_...`),
`amount` correct (3000 ou 6000 centimes selon le plan).
❌ **Échec** : `500` ou `clientSecret: null` → vérifier
`STRIPE_SECRET_KEY` dans `.env`.

### 6.2 Créer un code promo (admin)

```bash
PROMO_CODE_ID=$(curl -s -X POST http://localhost:8080/api/admin/promo-codes \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code":"JOSEPH20",
    "description":"20% de réduction premier paiement",
    "discountPercent":20,
    "maxUses":100
  }' | jq -r '.id')

echo "PROMO_CODE_ID=$PROMO_CODE_ID"
```

✅ **Succès** : `201 Created`, code `JOSEPH20` actif.

### 6.3 PaymentIntent avec code promo

```bash
PI_PROMO=$(curl -s -X POST http://localhost:8080/api/subscriptions/stripe/create-payment-intent \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"plan":"PREMIUM","currency":"EUR","promoCode":"JOSEPH20"}')

echo "$PI_PROMO" | jq '{amount, discountApplied, promoCode}'
```

✅ **Succès** : `amount` = 2400 (au lieu de 3000), `discountApplied: 600`,
`promoCode: "JOSEPH20"`. Une entrée est créée dans `joseph_admin.promo_code_usage`.

### 6.4 Vérifier la mise à jour du plan après webhook

> En vrai paiement, Stripe envoie un webhook `payment_intent.succeeded`.
> En local on simule via Stripe CLI ou un curl direct sur le webhook
> (signature requise — voir `docs/PAYMENT-INTEGRATION.md`).

```bash
# Simulation directe : confirmer le PaymentIntent côté Stripe (clés test)
stripe payment_intents confirm $(echo "$PI" | jq -r '.id') \
  --payment-method=pm_card_visa
# (nécessite la CLI Stripe : https://stripe.com/docs/stripe-cli)
```

✅ **Succès** : webhook reçu, `subscription` mise à jour en base
(`plan = PREMIUM`, `status = ACTIVE`), un nouveau JWT contient
`plan: "PREMIUM"`.

```bash
# Vérifier l'historique des paiements
curl -s http://localhost:8080/api/subscriptions/history \
  -H "Authorization: Bearer $TOKEN" | jq '.[] | {plan, amount, currency, status, createdAt}'
```

---

## 7. Admin Dashboard (admin-frontend)

### 7.1 Bootstrap super-admin

> Le `SuperAdminBootstrap` (auth-service) tourne au démarrage : il
> promeut tout email de `SUPER_ADMIN_EMAILS` en `ADMIN`. Si le compte
> n'existe pas, il est créé avec `SUPER_ADMIN_DEFAULT_PASSWORD` (ou
> un mot de passe temporaire imprimé dans les logs si non fourni).

```bash
# Vérifier le bootstrap dans les logs
docker compose logs auth-service | grep -i "super.?admin"
```

✅ **Succès** : ligne du type
`Super-admin promu : admin@josephyusuf.com (ADMIN)` ou
`Super-admin créé : admin@josephyusuf.com (mot de passe temporaire : XXXX)`.

### 7.2 Login depuis l'UI

1. Ouvrir http://localhost:4201
2. Renseigner `admin@josephyusuf.com` / mot de passe (cf. logs ou
   `SUPER_ADMIN_DEFAULT_PASSWORD`)
3. Cliquer **Se connecter**

✅ **Succès** : redirection vers `/dashboard`. La nav latérale affiche
Users, Transactions, Codes promo, Audit log.
❌ **Échec — rôle non ADMIN** : redirection vers `/login` + toast « Accès
refusé ». L'`adminGuard` lit le claim `role` du JWT.

### 7.3 Changer le plan d'un utilisateur

1. Aller dans **Users**
2. Chercher `test@example.com`
3. Cliquer la ligne → vue détail
4. Bouton **Changer plan** → choisir `PREMIUM_PLUS` → confirmer

✅ **Succès** : toast « Plan mis à jour », table rafraîchie. Une entrée
`USER_UPDATE_PLAN` apparaît dans Audit log.

### 7.4 Créer un code promo depuis l'UI

1. **Codes promo** → bouton **Nouveau code**
2. Remplir : `WELCOME10`, description, `discountPercent: 10`,
   `maxUses: 1000`, expiration optionnelle
3. **Créer**

✅ **Succès** : code listé, statut `Actif`, créé par l'admin courant.
❌ **Échec — code dupliqué** : toast `Code promo déjà existant` (409).

### 7.5 KPIs

1. Aller dans **Dashboard**

✅ **Succès** : 4 cartes affichées et non vides :
- Utilisateurs totaux / actifs / bloqués
- Répartition par plan (FREE / PREMIUM / PREMIUM_PLUS)
- MRR (€)
- Codes promo actifs / utilisés

### 7.6 Audit log

1. Aller dans **Audit log**
2. Vérifier les entrées : `USER_UPDATE_PLAN`, `PROMO_CODE_CREATE`, etc.
3. Filtrer par `adminId` (super-admin)

✅ **Succès** : table paginée triée par `createdAt DESC`. Chaque ligne
contient action, target, IP, détails.

---

## 8. Frontend utilisateur

### 8.1 Login utilisateur

1. Ouvrir http://localhost:4200
2. Email `test@example.com` + mot de passe → **Se connecter**

✅ **Succès** : redirection vers `/dashboard`, header avec prénom
« Joseph », badge plan `PREMIUM_PLUS` (suite à 7.3).

### 8.2 Dashboard principal

✅ **Succès** : la page affiche
- Le total revenus du mois courant (mai 2026 → 1 500 XOF)
- La classification visuelle (badge ABUNDANCE en vert/or)
- Le graphique des 6 derniers mois (chart PrimeNG)
- La carte de la règle 50/30/20 avec les montants 750 / 450 / 300

### 8.3 Saisie d'un revenu

1. Bouton **+ Ajouter un revenu** dans la nav
2. Sélectionner la source `Salaire principal`, montant `2000`,
   mois `6`, année `2026`
3. **Enregistrer**

✅ **Succès** : toast « Revenu ajouté », dashboard rafraîchi, mois 6
visible dans le graphique. Une nouvelle alerte `INCOME_RECORDED` apparaît.

### 8.4 Cloche de notifications

1. Cloche dans la navbar — un badge affiche le nombre d'alertes non lues
2. Clic → drawer s'ouvre, liste les alertes (ABUNDANCE, RULE_APPLIED,
   INCOME_RECORDED)
3. Clic sur une alerte → marquée lue, badge décrémente
4. Bouton **Tout marquer comme lu**

✅ **Succès** : drawer animé, badge passe à 0 après « Tout marquer ».

### 8.5 Téléchargement rapport PDF

1. Menu **Rapports** → liste des rapports générés (cf. 5.3)
2. Cliquer **Télécharger** sur le rapport mai 2026

✅ **Succès** : un PDF est téléchargé par le navigateur, ouvrable, contenu
identique à `/tmp/rapport-mai-2026.pdf` (section 5.4).

---

## Diagnostic en cas de problème

### Logs ciblés

```bash
docker compose logs --tail=50 -f <service>
docker compose logs auth-service | grep -i "error\|warn"
```

### Postgres

```bash
docker exec -it joseph_postgres psql -U joseph -d joseph_db -c "\dn"
# → 8 schemas : joseph_admin, joseph_alerts, joseph_auth, joseph_income,
#   joseph_reports, joseph_rules, joseph_subscriptions

docker exec -it joseph_postgres psql -U joseph -d joseph_db \
  -c "SELECT id, email, plan, role, enabled FROM joseph_auth.users;"
```

### Kafka

```bash
docker exec joseph_kafka kafka-topics --list --bootstrap-server localhost:9092

docker exec joseph_kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic income.classified \
  --from-beginning --max-messages 5
```

### Reset complet de la stack

```bash
docker compose down -v          # ⚠️ supprime aussi les volumes (DB, Kafka)
docker compose up -d --build
```

---

## Critères globaux de validation

- [ ] Tous les services `Up (healthy)` (section 0.5).
- [ ] Auth : register / login / reset password fonctionnels (1.x).
- [ ] Income : création source + entries, classification ABUNDANCE
      visible (2.x). Limite plan FREE respectée.
- [ ] Rule engine : 50/30/20 OK pour FREE, règle avancée bloquée (3.x).
- [ ] Alerts : événements Kafka consommés, alertes listées,
      mark-as-read fonctionnel (4.x).
- [ ] Reports : 403 pour FREE, PDF généré et téléchargé pour PREMIUM (5.x).
- [ ] Subscriptions : PaymentIntent Stripe OK, code promo applique la
      remise, webhook met à jour le plan (6.x).
- [ ] Admin : login super-admin, modification plan, création code promo,
      KPIs renseignés, audit log peuplé (7.x).
- [ ] Frontend user : dashboard, saisie, cloche, téléchargement PDF (8.x).

> Si tous les critères sont cochés, la stack est prête pour le
> déploiement VPS Hetzner (cf. `ansible/`).
