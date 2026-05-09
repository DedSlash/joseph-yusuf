# Payment integration — subscription-service

État au Jalon 8 :
- **Stripe** : opérationnel (PaymentIntent + webhooks signés)
- **Wave** : mode SIMULATION (`WaveService`)
- **Orange Money** : mode SIMULATION (`OrangeMoneyService`)

Ce document décrit comment brancher les vraies APIs Wave et Orange Money quand
les comptes marchand seront ouverts.

## Variables d'environnement

| Variable                | Service              | Description                                  |
|-------------------------|----------------------|----------------------------------------------|
| `STRIPE_SECRET_KEY`     | subscription-service | `sk_live_...` ou `sk_test_...`               |
| `STRIPE_WEBHOOK_SECRET` | subscription-service | `whsec_...` issu du dashboard Stripe         |
| `WAVE_API_KEY`          | (à ajouter)          | clé API Wave (à demander à Wave Sénégal)     |
| `WAVE_API_BASE_URL`     | (à ajouter)          | `https://api.wave.com/v1` (ou sandbox)       |
| `ORANGE_API_CLIENT_ID`  | (à ajouter)          | client ID Orange Developer                   |
| `ORANGE_API_CLIENT_SECRET` | (à ajouter)       | client secret Orange Developer                |
| `ORANGE_API_BASE_URL`   | (à ajouter)          | `https://api.orange.com/orange-money-webpay` |

Tous les secrets doivent passer par `docker-compose` env vars + Ansible vault
en production. **Jamais en dur dans `application.yml`**.

## Stripe — déjà opérationnel

### Endpoints
- `POST /api/subscriptions/stripe/create-payment-intent` — crée un PaymentIntent et persiste une transaction PENDING
- `POST /api/webhooks/stripe` — reçoit les événements Stripe (signé)

### Webhook Stripe
1. Sur le dashboard Stripe, créer un endpoint pointant vers
   `https://<votre-domaine>/api/webhooks/stripe`
2. Sélectionner les événements :
   - `payment_intent.succeeded`
   - `payment_intent.payment_failed`
   - `charge.refunded`
3. Copier le `whsec_...` dans `STRIPE_WEBHOOK_SECRET`

### Idempotence
Deux niveaux :
- **côté Stripe API** : `RequestOptions.setIdempotencyKey(...)` sur la création de PaymentIntent (évite les doubles charges si le client retry)
- **côté webhook** : table `processed_webhook_events` indexée sur `event_id` ; `WebhookService.processStripeWebhook()` vérifie l'existence avant de processer puis insère après

## Wave — branchement réel

`WaveService.initiate(...)` retourne actuellement une réponse simulée. Pour brancher la vraie API :

1. Ouvrir un compte marchand sur https://business.wave.com/
2. Récupérer la clé API
3. Remplacer le contenu de `WaveService` :

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class WaveService {

    private final RestClient waveClient; // bean configuré avec base URL + auth header

    public PaymentProviderResponse initiate(UUID userId, WavePaymentRequest request) {
        BigDecimal amount = amountFor(request.getPlan());

        WaveCheckoutRequest payload = WaveCheckoutRequest.builder()
                .amount(amount)
                .currency("XOF")
                .clientReference(userId + ":" + request.getPlan().name())
                .successUrl(...)
                .errorUrl(...)
                .build();

        WaveCheckoutResponse response = waveClient.post()
                .uri("/checkout/sessions")
                .body(payload)
                .retrieve()
                .body(WaveCheckoutResponse.class);

        return PaymentProviderResponse.builder()
                .provider(PaymentProvider.WAVE)
                .transactionId(response.getId())
                .status(TransactionStatus.PENDING)
                .amount(amount)
                .currency("XOF")
                .redirectUrl(response.getWaveLaunchUrl())
                .build();
    }
}
```

4. Ajouter un endpoint webhook `POST /api/webhooks/wave` qui dispatche vers
   `subscriptionService.activateAfterPayment(...)` après vérification de la
   signature HMAC fournie par Wave.

## Orange Money — branchement réel

Pareil pour Orange Money (`OrangeMoneyService`). L'API utilise OAuth2 (client_credentials) pour obtenir un token avant chaque appel. Référence : https://developer.orange.com/apis/om-webpay

Étapes :
1. Créer un compte sur https://developer.orange.com/
2. Activer l'API Orange Money WebPay
3. Récupérer `client_id` + `client_secret`
4. Implémenter un `OrangeTokenService` qui cache le token (durée typique : 1h)
5. Remplacer la simulation par les vrais appels :
   - `POST /oauth/v3/token` (auth)
   - `POST /orange-money-webpay/dev/v1/webpayment` (init paiement)
6. Ajouter `POST /api/webhooks/orange` pour les notifications de paiement

## Synchronisation avec auth-service

Après confirmation d'un paiement (webhook Stripe ou retour Wave/Orange), le
service appelle `AuthClient.updatePlan(userId, plan)` via Feign. Si l'appel
échoue, l'erreur est loggée mais l'activation locale est conservée — un job
de retry manuel ou cron sera ajouté au Jalon ultérieur si nécessaire.

## Tests

```bash
mvn -pl subscription-service verify
```

Coverage cible ≥ 70 % (exclusions : `config/`, `dto/`, `entity/`, `enums/`,
`exception/`).
