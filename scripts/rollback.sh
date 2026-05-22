#!/bin/bash
set -e

echo "⏪ Joseph·Yusuf — Rollback"
echo "=========================="

VPS_IP="178.105.151.226"
VPS_USER="deploy"
PROJECT_DIR="/opt/joseph-yusuf"
COMMIT="${1:-HEAD~1}"

echo "⚠️  Rollback vers : $COMMIT"
echo "   Appuie sur Entrée pour confirmer, Ctrl+C pour annuler."
read -r

ssh "$VPS_USER@$VPS_IP" bash -s <<ENDSSH
  set -e
  cd $PROJECT_DIR

  CURRENT=\$(git rev-parse --short HEAD)
  echo "📌 Commit actuel : \$CURRENT"
  echo "⏪ Rollback vers  : $COMMIT"

  git fetch origin
  git checkout $COMMIT

  echo "🐳 Rebuild des images..."
  docker compose -f docker-compose.prod.yml build

  echo "🚀 Redémarrage..."
  docker compose -f docker-compose.prod.yml up -d --remove-orphans

  echo "⏳ Attente démarrage (45s)..."
  sleep 45

  echo "🏥 Health check post-rollback..."
  GATEWAY=\$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8080/actuator/health || echo "000")
  FRONTEND=\$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:4200 || echo "000")

  if [ "\$GATEWAY" = "200" ] && [ "\$FRONTEND" = "200" ]; then
    echo "✅ Rollback réussi !"
  else
    echo "⚠️  Services pas encore prêts après rollback."
    echo "   Vérifie : docker compose -f docker-compose.prod.yml ps"
  fi

  echo ""
  echo "📋 Status :"
  docker compose -f docker-compose.prod.yml ps --format "table {{.Name}}\t{{.Status}}"
  echo ""
  echo "📌 Pour revenir à main : git checkout main && ./scripts/deploy.sh"
ENDSSH
