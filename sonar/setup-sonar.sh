#!/bin/bash
# Script d'initialisation SonarQube
# Exécuter une seule fois après le premier démarrage de SonarQube
# Usage: ./setup-sonar.sh [SONAR_URL] [ADMIN_TOKEN]

SONAR_URL="${1:-${SONARQUBE_URL:-http://localhost:9000}}"
ADMIN_TOKEN="${2:-admin}"

echo "=== Configuration SonarQube Joseph Yusuf ==="
echo "URL: $SONAR_URL"

# Attendre que SonarQube soit prêt
echo "Attente de SonarQube..."
until curl -s "$SONAR_URL/api/system/status" | grep -q '"status":"UP"'; do
    sleep 5
done
echo "SonarQube est prêt."

# Créer le Quality Gate custom
echo "Création du Quality Gate..."
QG_ID=$(curl -s -u "$ADMIN_TOKEN:" -X POST \
    "$SONAR_URL/api/qualitygates/create?name=Joseph+Yusuf+Quality+Gate" \
    | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null)

if [ -n "$QG_ID" ]; then
    echo "Quality Gate créé (ID: $QG_ID)"

    # Ajouter les conditions
    curl -s -u "$ADMIN_TOKEN:" -X POST "$SONAR_URL/api/qualitygates/create_condition?gateName=Joseph+Yusuf+Quality+Gate&metric=new_coverage&op=LT&error=70" > /dev/null
    curl -s -u "$ADMIN_TOKEN:" -X POST "$SONAR_URL/api/qualitygates/create_condition?gateName=Joseph+Yusuf+Quality+Gate&metric=new_duplicated_lines_density&op=GT&error=3" > /dev/null
    curl -s -u "$ADMIN_TOKEN:" -X POST "$SONAR_URL/api/qualitygates/create_condition?gateName=Joseph+Yusuf+Quality+Gate&metric=new_maintainability_rating&op=GT&error=2" > /dev/null
    curl -s -u "$ADMIN_TOKEN:" -X POST "$SONAR_URL/api/qualitygates/create_condition?gateName=Joseph+Yusuf+Quality+Gate&metric=new_reliability_rating&op=GT&error=2" > /dev/null
    curl -s -u "$ADMIN_TOKEN:" -X POST "$SONAR_URL/api/qualitygates/create_condition?gateName=Joseph+Yusuf+Quality+Gate&metric=new_security_rating&op=GT&error=1" > /dev/null
    curl -s -u "$ADMIN_TOKEN:" -X POST "$SONAR_URL/api/qualitygates/create_condition?gateName=Joseph+Yusuf+Quality+Gate&metric=new_security_hotspots_reviewed&op=LT&error=100" > /dev/null

    # Définir comme Quality Gate par défaut
    curl -s -u "$ADMIN_TOKEN:" -X POST "$SONAR_URL/api/qualitygates/set_as_default?name=Joseph+Yusuf+Quality+Gate" > /dev/null
    echo "Quality Gate configuré comme défaut."
else
    echo "Quality Gate existe probablement déjà."
fi

# Créer les projets
PROJECTS=("joseph-yusuf-auth-service:Joseph Yusuf - Auth Service"
           "joseph-yusuf-income-service:Joseph Yusuf - Income Service"
           "joseph-yusuf-rule-engine-service:Joseph Yusuf - Rule Engine Service"
           "joseph-yusuf-frontend:Joseph Yusuf - Frontend")

for project in "${PROJECTS[@]}"; do
    KEY="${project%%:*}"
    NAME="${project##*:}"
    echo "Création du projet: $NAME..."
    curl -s -u "$ADMIN_TOKEN:" -X POST \
        "$SONAR_URL/api/projects/create?project=$KEY&name=$(echo $NAME | sed 's/ /+/g')" > /dev/null
done

# Générer un token d'analyse
echo ""
echo "Génération du token d'analyse..."
TOKEN_RESPONSE=$(curl -s -u "$ADMIN_TOKEN:" -X POST \
    "$SONAR_URL/api/user_tokens/generate?name=jenkins-analysis&type=GLOBAL_ANALYSIS_TOKEN")
ANALYSIS_TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('token',''))" 2>/dev/null)

if [ -n "$ANALYSIS_TOKEN" ]; then
    echo ""
    echo "=== TOKEN D'ANALYSE ==="
    echo "$ANALYSIS_TOKEN"
    echo "========================"
    echo "Ajoutez ce token dans Jenkins comme credential 'sonar-token'"
else
    echo "Token déjà existant ou erreur. Vérifiez manuellement."
fi

echo ""
echo "=== Configuration terminée ==="
echo "Dashboard: $SONAR_URL"
