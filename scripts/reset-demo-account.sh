#!/usr/bin/env bash
#
# reset-demo-account.sh
#
# Recrée le compte démo public utilisé par le portfolio de Rey Dedy Pangou.
# Idempotent : supprime le compte s'il existe puis le recrée vierge avec
# ~6 mois de revenus fictifs et 2 objectifs d'épargne.
#
# Variables d'environnement requises (charger via .env.demo) :
#   API_BASE_URL          URL gateway (ex: https://api.josephyusuf.com)
#   ADMIN_EMAIL           Email du compte super-admin
#   ADMIN_PASSWORD        Mot de passe du super-admin
#   DEMO_EMAIL            Email du compte démo (ex: demo.portfolio@josephyusuf.com)
#   DEMO_PASSWORD         Mot de passe du compte démo
#   DEMO_FIRSTNAME        Prénom à afficher (défaut: Demo)
#   DEMO_LASTNAME         Nom à afficher (défaut: Portfolio)
#   DEMO_COUNTRY          Code pays (défaut: SN)
#   DEMO_CURRENCY         Devise (défaut: XOF)
#
# Usage :
#   set -a; source .env.demo; set +a
#   ./scripts/reset-demo-account.sh
#
# Cron suggéré (sur le VPS) :
#   0 3 * * *  cd /opt/joseph-yusuf && set -a && . .env.demo && set +a && ./scripts/reset-demo-account.sh >> /var/log/joseph-demo-reset.log 2>&1

set -euo pipefail

API_BASE_URL="${API_BASE_URL:?API_BASE_URL non défini}"
ADMIN_EMAIL="${ADMIN_EMAIL:?ADMIN_EMAIL non défini}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:?ADMIN_PASSWORD non défini}"
DEMO_EMAIL="${DEMO_EMAIL:?DEMO_EMAIL non défini}"
DEMO_PASSWORD="${DEMO_PASSWORD:?DEMO_PASSWORD non défini}"
DEMO_FIRSTNAME="${DEMO_FIRSTNAME:-Demo}"
DEMO_LASTNAME="${DEMO_LASTNAME:-Portfolio}"
DEMO_COUNTRY="${DEMO_COUNTRY:-SN}"
DEMO_CURRENCY="${DEMO_CURRENCY:-XOF}"

CURL_OPTS=(--silent --show-error --max-time 30 -H "Content-Type: application/json")

log() { printf '[%s] %s\n' "$(date -Is)" "$*"; }
die() { log "ERREUR: $*"; exit 1; }

require() {
  command -v "$1" >/dev/null 2>&1 || die "outil manquant: $1"
}
require curl
require jq

# 1. Login admin
log "Authentification super-admin…"
admin_resp=$(curl "${CURL_OPTS[@]}" -X POST "$API_BASE_URL/api/auth/login" \
  -d "$(jq -n --arg e "$ADMIN_EMAIL" --arg p "$ADMIN_PASSWORD" \
       '{email:$e,password:$p}')")
admin_token=$(echo "$admin_resp" | jq -r '.accessToken // empty')
[ -n "$admin_token" ] || die "login admin échoué — réponse: $admin_resp"

# 2. Chercher le compte démo (best-effort)
log "Recherche compte démo existant…"
search_resp=$(curl "${CURL_OPTS[@]}" \
  -H "Authorization: Bearer $admin_token" \
  "$API_BASE_URL/api/auth/users?search=$DEMO_EMAIL&size=5")
demo_id=$(echo "$search_resp" | jq -r --arg e "$DEMO_EMAIL" \
  '.content[]? | select(.email == $e) | .id' | head -n1)

if [ -n "$demo_id" ]; then
  log "Compte démo trouvé (id=$demo_id) — suppression…"
  curl "${CURL_OPTS[@]}" -X DELETE \
    -H "Authorization: Bearer $admin_token" \
    "$API_BASE_URL/api/auth/users/$demo_id" -o /dev/null -w 'HTTP %{http_code}\n'
else
  log "Aucun compte démo préexistant — création directe."
fi

# 3. Recréer le compte démo
log "Création du compte démo $DEMO_EMAIL…"
register_resp=$(curl "${CURL_OPTS[@]}" -X POST "$API_BASE_URL/api/auth/register" \
  -d "$(jq -n \
        --arg e "$DEMO_EMAIL" \
        --arg p "$DEMO_PASSWORD" \
        --arg f "$DEMO_FIRSTNAME" \
        --arg l "$DEMO_LASTNAME" \
        --arg c "$DEMO_COUNTRY" \
        --arg cu "$DEMO_CURRENCY" \
        '{email:$e,password:$p,firstName:$f,lastName:$l,country:$c,currency:$cu}')")
demo_token=$(echo "$register_resp" | jq -r '.accessToken // empty')
[ -n "$demo_token" ] || die "register démo échoué — réponse: $register_resp"

AUTH_HDR=(-H "Authorization: Bearer $demo_token")

# 4. Créer 1 source de revenu (FREELANCE pour rester FREE-compatible)
log "Création source de revenu…"
src_resp=$(curl "${CURL_OPTS[@]}" "${AUTH_HDR[@]}" -X POST \
  "$API_BASE_URL/api/incomes/sources" \
  -d "$(jq -n --arg cu "$DEMO_CURRENCY" \
       '{name:"Mission freelance consulting",type:"FREELANCE",currency:$cu}')")
source_id=$(echo "$src_resp" | jq -r '.id // empty')
[ -n "$source_id" ] || die "création source échouée — réponse: $src_resp"

# 5. Seed de 6 mois de revenus variables (jeu réaliste : NORMAL → ABUNDANCE → LEAN)
log "Seed des 6 derniers mois de revenus (source $source_id)…"
current_y=$(date +%Y)
current_m=$(date +%m | sed 's/^0//')

# Montants en XOF — variations pour déclencher ABUNDANCE et LEAN
# Moyenne ≈ 800 000 ; seuils : ABUNDANCE > 920 000 ; LEAN < 680 000
amounts=(750000 820000 780000 1200000 850000 450000)

for offset in 5 4 3 2 1 0; do
  m=$((current_m - offset))
  y=$current_y
  if [ $m -le 0 ]; then m=$((m + 12)); y=$((y - 1)); fi
  amount=${amounts[$((5 - offset))]}
  curl "${CURL_OPTS[@]}" "${AUTH_HDR[@]}" -X POST \
    "$API_BASE_URL/api/incomes/entries" \
    -d "$(jq -n --arg s "$source_id" --argjson a "$amount" --argjson m "$m" --argjson y "$y" \
         '{incomeSourceId:$s,amount:$a,month:$m,year:$y,note:"Revenu mission"}')" \
    -o /dev/null -w "  ${y}-${m}: HTTP %{http_code}\n"
done

# 6. Créer 2 objectifs d'épargne
log "Création de 2 objectifs d'épargne…"
start_date=$(date -d "6 months ago" +%Y-%m-%d 2>/dev/null || date -v-6m +%Y-%m-%d)
target_date_1=$(date -d "+18 months" +%Y-%m-%d 2>/dev/null || date -v+18m +%Y-%m-%d)
target_date_2=$(date -d "+6 months" +%Y-%m-%d 2>/dev/null || date -v+6m +%Y-%m-%d)

curl "${CURL_OPTS[@]}" "${AUTH_HDR[@]}" -X POST \
  "$API_BASE_URL/api/incomes/savings/goals" \
  -d "$(jq -n --arg s "$start_date" --arg t "$target_date_1" \
       '{name:"Fonds de sécurité 6 mois",targetAmount:4800000,monthlyTargetPercent:15,startDate:$s,targetDate:$t}')" \
  -o /dev/null -w "  goal 1: HTTP %{http_code}\n"

curl "${CURL_OPTS[@]}" "${AUTH_HDR[@]}" -X POST \
  "$API_BASE_URL/api/incomes/savings/goals" \
  -d "$(jq -n --arg s "$start_date" --arg t "$target_date_2" \
       '{name:"Voyage famille",targetAmount:1200000,monthlyTarget:200000,startDate:$s,targetDate:$t}')" \
  -o /dev/null -w "  goal 2: HTTP %{http_code}\n"

log "✔ Compte démo reset OK — $DEMO_EMAIL"
