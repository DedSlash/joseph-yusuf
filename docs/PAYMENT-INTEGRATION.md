# Payment integration — subscription-service

État actuel :
- **Stripe Subscriptions** : opérationnel (paiements récurrents + coupons FOREVER)
- **Wave** : mode SIMULATION (`WaveService`) — `subscriptionType = ONE_TIME`
- **Orange Money** : mode SIMULATION (`OrangeMoneyService`) — `subscriptionType = ONE_TIME`

Ce document décrit le flow Stripe Subscriptions, la création des coupons,
les webhooks à configurer et la roadmap récurrence Wave / Orange Money.

## Variables d'environnement

| Variable                            | Service              | Description                                       |
|-------------------------------------|----------------------|---------------------------------------------------|
| `STRIPE_SECRET_KEY`                 | subscription-service | `sk_live_...` ou `sk_test_...`                    |
| `STRIPE_WEBHOOK_SECRET`             | subscription-service | `whsec_...` issu du dashboard Stripe              |
| `STRIPE_PRICE_PREMIUM_EUR`          | subscription-service | Price ID Stripe (PREMIUM mensuel EUR)             |
| `STRIPE_PRICE_PREMIUM_XOF`          | subscription-service | Price ID Stripe (PREMIUM mensuel XOF)             |
| `STRIPE_PRICE_PREMIUM_PLUS_EUR`     | subscription-service | Price ID Stripe (PREMIUM_PLUS mensuel EUR)        |
| `STRIPE_PRICE_PREMIUM_PLUS_XOF`     | subscription-service | Price ID Stripe (PREMIUM_PLUS mensuel XOF)        |
| `WAVE_API_KEY`                      | (à ajouter)          | clé API Wave (à demander à Wave Sénégal)          |
| `WAVE_API_BASE_URL`                 | (à ajouter)          | `https://api.wave.com/v1` (ou sandbox)            |
| `ORANGE_API_CLIENT_ID`              | (à ajouter)          | client ID Orange Developer                        |
| `ORANGE_API_CLIENT_SECRET`          | (à ajouter)          | client secret Orange Developer                    |
| `ORANGE_API_BASE_URL`               | (à ajouter)          | `https://api.orange.com/orange-money-webpay`      |

Tous les secrets doivent passer par `docker-compose` env vars + Ansible vault
en production. **Jamais en dur dans `application.yml`**.

## Stripe Subscriptions — flow complet

### 1. Création des Products + Prices côté Stripe Dashboard

Pour chaque plan + devise (4 combinaisons), créer un **Product** avec un **Price** récurrent :

| Product           | Currency | Recurring  | Price ID            |
|-------------------|----------|------------|---------------------|
| PREMIUM           | EUR      | Monthly    | `price_xxx`         |
| PREMIUM           | XOF      | Monthly    | `price_xxx`         |
| PREMIUM_PLUS      | EUR      | Monthly    | `price_xxx`         |
| PREMIUM_PLUS      | XOF      | Monthly    | `price_xxx`         |

Stripe Dashboard → **Products → Add product** :
1. Name : `Joseph·Yusuf PREMIUM`
2. Pricing → **Recurring**, billing period **Monthly**
3. Currency : EUR (ou XOF — créer un Price additionnel par devise)
4. Save → noter le `price_xxx`

Reporter les Price IDs dans les variables d'environnement (cf. `.env.example`).

### 2. Endpoints exposés

| Méthode | Path                                          | Description                                     |
|---------|-----------------------------------------------|-------------------------------------------------|
| POST    | `/api/subscriptions/stripe/create`            | Créer subscription (clientSecret à confirmer)   |
| POST    | `/api/subscriptions/stripe/confirm/{subId}`   | Confirmer côté serveur (fallback du webhook)    |
| DELETE  | `/api/subscriptions/stripe/cancel`            | Annuler (par défaut : fin de période)           |
| GET     | `/api/subscriptions/current`                  | Subscription courante + couponApplied + nextInvoiceAmount |
| PUT     | `/api/subscriptions/auto-renew?enabled=true`  | Toggle auto-renew (cancel_at_period_end)        |
| GET     | `/api/subscriptions/history`                  | Historique transactions paginé                  |
| POST    | `/api/webhooks/stripe`                        | Endpoint webhook (signé)                        |

### 3. Body `POST /stripe/create`

```json
{
  "planTier": "PREMIUM",
  "currency": "XOF",
  "paymentMethodId": "pm_xxx",
  "couponCode": "EARLY50"
}
```

Réponse (HTTP 200) :
```json
{
  "subscriptionId": "sub_xxx",
  "clientSecret": "pi_xxx_secret_xxx",
  "status": "incomplete"
}
```

Le frontend confirme ensuite côté Stripe.js avec le `clientSecret` ; le statut
réel devient `ACTIVE` quand le webhook `invoice.payment_succeeded` arrive.

### 4. Annulation

`DELETE /api/subscriptions/stripe/cancel` accepte un body optionnel :
```json
{ "immediately": false }
```
- `immediately=false` (défaut) → `cancel_at_period_end = true` côté Stripe
  (le client garde l'accès jusqu'à la fin de la période payée)
- `immediately=true` → annulation immédiate (à éviter sauf cas particulier)

Le toggle `auto-renew` agit symétriquement : `enabled=true` réactive
(`cancel_at_period_end = false`), `enabled=false` programme l'annulation
en fin de période.

## Coupons durables (Stripe Coupons + promo codes internes)

### Architecture en coexistence

| Couche                                       | Rôle                                                              |
|----------------------------------------------|-------------------------------------------------------------------|
| `joseph_admin.promo_codes` (admin-service)   | Validation métier (limites/user, dates, statut admin)             |
| Stripe Coupon (Dashboard)                    | Application de la réduction sur **chaque** invoice (FOREVER)      |

Pour qu'un code (ex. `EARLY50`) fonctionne, il doit exister **dans les deux** :
1. Une ligne `joseph_admin.promo_codes` avec `code = 'EARLY50'`,
   `discount_percent = 50`, `enabled = true`, `valid_from/valid_until` cohérents
2. Un **Stripe Coupon** d'ID `EARLY50` (case sensitive)

### Création du coupon EARLY50 côté Stripe

Stripe Dashboard → **Products → Coupons → Create coupon** :
1. **ID** : `EARLY50` (doit correspondre exactement au `code` interne)
2. **Type** : Percentage discount → 50% off
3. **Duration** : **Forever** (le client garde la réduction à vie)
4. **Currency** : peut être laissé vide (s'applique à toutes les devises)
5. Save

### Comportement runtime

Quand l'utilisateur saisit `EARLY50` à l'inscription :
1. `StripeService.createSubscription(...)` appelle `AdminClient.validate("EARLY50", userId)`
   pour valider en interne (limites par user, expiration, etc.)
2. Si valide → `SubscriptionCreateParams.builder().setCoupon("EARLY50")` côté Stripe
3. Stripe applique la réduction automatiquement sur **chaque invoice** future
   (durée = FOREVER), sans intervention supplémentaire du client
4. Le webhook `invoice.payment_succeeded` appelle `AdminClient.apply(...)` une
   fois pour enregistrer l'usage côté admin (compteur d'utilisations)

Le client **n'a JAMAIS à ressaisir** son code : la page "Mon abonnement"
affiche simplement `couponApplied = "EARLY50"` + le `nextInvoiceAmount`
(montant de base ; pour le montant réel après réduction, interroger Stripe
`Invoice.upcoming` côté front si nécessaire).

### Coupons ONCE / MONTHS

Stripe gère automatiquement les durées `once` (1 seule invoice) et `repeating`
(N mois) — le champ `coupon_duration` côté DB (`ONCE | FOREVER | MONTHS`) est
purement informationnel (récupéré au moment de la création).

## Webhooks à configurer

Stripe Dashboard → **Developers → Webhooks → Add endpoint** :
- URL : `https://<votre-domaine>/api/webhooks/stripe`
- Events à sélectionner :
  - `invoice.payment_succeeded` → activate subscription, record transaction
  - `invoice.payment_failed` → record failed transaction (Stripe Smart Retries automatiques)
  - `customer.subscription.deleted` → downgrade FREE (final, après tous les retries)
  - `customer.subscription.updated` → mise à jour currentPeriodEnd / status / cancelAtPeriodEnd
- Copier le `whsec_...` dans `STRIPE_WEBHOOK_SECRET`

### Idempotence

Deux niveaux conservés :
- **côté Stripe API** : si nécessaire, ajouter `RequestOptions.setIdempotencyKey(...)` sur les appels
- **côté webhook** : table `processed_webhook_events` indexée sur `event_id` ;
  `WebhookService.processStripeWebhook()` vérifie l'existence avant de processer puis insère après

### Smart Retries Stripe

En cas d'`invoice.payment_failed`, Stripe relance automatiquement le paiement
selon une politique configurable (Dashboard → Subscriptions → Retry settings).
Après épuisement des retries (typiquement 4 tentatives sur 3 semaines),
Stripe émet `customer.subscription.deleted` → downgrade vers FREE côté Joseph·Yusuf.

## Wave / Orange Money — récurrence (post-lancement)

Pour le lancement v1, Wave et Orange Money restent en **mode `ONE_TIME`** :
- l'utilisateur effectue un paiement unique mensuel manuel
- aucun renouvellement automatique

### Champ `subscriptionType` sur les transactions

Pour préparer la future migration récurrente, les transactions Wave/Orange
sont logiquement marquées `ONE_TIME` (à exposer dans le DTO à terme).
Le passage à `RECURRING` nécessitera :

#### Wave
Wave Business propose un mode "abonnement" via leur API.
Étapes (post-lancement) :
1. Compte Wave Business activé en mode merchant
2. Créer un "Plan" Wave (équivalent Stripe Product)
3. `WaveService.initiate(...)` doit créer un mandate Wave puis facturer mensuellement
4. Endpoint webhook `POST /api/webhooks/wave` pour les notifications de renouvellement

#### Orange Money
Orange Money WebPay ne supporte **pas nativement** la récurrence aujourd'hui.
Workaround :
1. Stocker le `payer_id` Orange après le premier paiement
2. Cron mensuel `subscription-service` qui appelle l'API de pré-autorisation pour
   débiter ce `payer_id` (avec ré-acceptation client via SMS)

Documenter le statut dans la prochaine itération.

## Synchronisation avec auth-service

Après confirmation d'un paiement (webhook Stripe `invoice.payment_succeeded`
ou activation manuelle Wave/Orange), le service appelle
`AuthClient.updatePlan(userId, plan)` via Feign. Si l'appel échoue, l'erreur
est loggée mais l'activation locale est conservée — un job de retry manuel
ou cron sera ajouté au Jalon ultérieur si nécessaire.

Le downgrade `customer.subscription.deleted → FREE` appelle aussi
`AuthClient.updatePlan(userId, FREE)` de la même manière.

## Tests

```bash
mvn -pl subscription-service verify
```

Coverage cible ≥ 70 % (exclusions : `config/`, `dto/`, `entity/`, `enums/`,
`exception/`).
