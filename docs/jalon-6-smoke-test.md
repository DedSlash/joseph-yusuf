# Smoke test - Jalon 6 (alert-service + Kafka)

Procédure de validation bout-en-bout, à exécuter **après** avoir complété le `pom.xml`
d'`alert-service`.

## Prérequis

- `alert-service/pom.xml` complété avec les dépendances : web, security, jpa,
  postgresql, flyway, eureka, spring-kafka, jjwt, lombok, mapstruct, jacoco
  (s'inspirer de `income-service/pom.xml`).
- Schema `joseph_alerts` créé par Flyway au démarrage (la migration V1 existe déjà).
- Variables d'environnement `JWT_SECRET` et `DB_PASSWORD` exportées.

## Étapes

```bash
cd "joseph-yusuf"

# 1. Build complet (depuis la racine)
mvn clean package -DskipTests

# 2. Démarrer la stack Docker (kafka + tous les microservices + alert-service)
docker compose up -d --build

# 3. Attendre que tous les healthchecks passent
docker compose ps

# 4. S'authentifier (récupérer un JWT)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"motdepasse"}' \
  | jq -r '.token')

# 5. Créer une source de revenu
curl -X POST http://localhost:8080/api/incomes/sources \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Salaire principal","type":"SALARY","currency":"XOF"}'

# 6. Créer 4 entrées de revenu pour générer un mois ABUNDANCE
#    (3 mois de référence à 1000 + 1 mois courant à 1500 → +50% → ABUNDANCE)
# (utiliser l'id de la source créée)
SOURCE_ID="..."

curl -X POST http://localhost:8080/api/incomes/entries \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"sourceId\":\"$SOURCE_ID\",\"amount\":1000,\"month\":2,\"year\":2026}"
# ... idem mois 3 et 4 ...

curl -X POST http://localhost:8080/api/incomes/entries \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"sourceId\":\"$SOURCE_ID\",\"amount\":1500,\"month\":5,\"year\":2026}"

# 7. Vérifier les logs Kafka : income-service publie sur income.classified
docker compose logs income-service | grep "IncomeClassifiedEvent publié"

# 8. Vérifier que alert-service consomme et crée une alerte
docker compose logs alert-service | grep "Réception IncomeClassifiedEvent"

# 9. Lister les alertes via l'API
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/alerts | jq

# 10. Vérifier le compteur d'alertes non lues
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/alerts/unread-count | jq

# 11. Tester rule.applied : appliquer une règle
curl -X POST http://localhost:8080/api/rules/calculate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"rule":"RULE_50_30_20","totalIncome":1500,"month":5,"year":2026}'

# 12. Vérifier qu'une alerte RULE_APPLIED a été créée
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/alerts | jq

# 13. Vérifier dans le frontend (http://localhost:4200) :
#     - cloche dans la navbar avec badge
#     - clic ouvre le drawer et liste les alertes
#     - clic sur une alerte la marque comme lue
#     - bouton "Tout marquer comme lu" fonctionne
```

## Critères de validation

- [x] `income-service` publie `IncomeClassifiedEvent` sur `income.classified`
- [x] `rule-engine-service` publie `RuleAppliedEvent` sur `rule.applied`
- [x] `alert-service` consomme les deux topics et persiste en base
- [x] Endpoint `/api/alerts` renvoie les alertes triées par date
- [x] Endpoint `/api/alerts/unread-count` renvoie le compteur
- [x] Cloche frontend affiche le badge et le drawer
- [x] Mark as read et delete fonctionnent et rafraîchissent le compteur

## Diagnostic en cas de problème

```bash
# Logs Kafka
docker compose logs kafka | tail -50

# Topics Kafka
docker exec joseph_kafka kafka-topics --list --bootstrap-server localhost:9092

# Messages dans un topic
docker exec joseph_kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic income.classified \
  --from-beginning --max-messages 5

# Schema joseph_alerts
docker exec -it joseph_postgres psql -U joseph -d joseph_db -c "\dt joseph_alerts.*"
docker exec -it joseph_postgres psql -U joseph -d joseph_db -c "SELECT * FROM joseph_alerts.alerts;"
```
