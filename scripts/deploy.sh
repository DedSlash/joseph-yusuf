#!/bin/bash
set -e

echo "🔄 Joseph·Yusuf — Déploiement mise à jour"
echo "=========================================="

VPS_IP="178.105.151.226"
VPS_USER="deploy"
PROJECT_DIR="/opt/joseph-yusuf"
SERVICES="${1:-all}"

# Copier .env.prod si mis à jour localement
if [ -f ".env.prod" ]; then
  echo "🔐 Synchronisation .env.prod..."
  scp .env.prod "$VPS_USER@$VPS_IP:$PROJECT_DIR/.env"
  ssh "$VPS_USER@$VPS_IP" "chmod 600 $PROJECT_DIR/.env"
fi

ssh "$VPS_USER@$VPS_IP" bash -s <<ENDSSH
  set -e
  cd $PROJECT_DIR

  echo "📥 Pull dernières modifications..."
  git pull origin main

  if [ "$SERVICES" = "all" ]; then
    echo "🐳 Build tous les services..."
    docker compose -f docker-compose.prod.yml build

    echo "🚀 Redémarrage tous les services..."
    docker compose -f docker-compose.prod.yml up -d --remove-orphans
  else
    echo "🐳 Build service: $SERVICES..."
    docker compose -f docker-compose.prod.yml build $SERVICES

    echo "🚀 Redémarrage service: $SERVICES..."
    docker compose -f docker-compose.prod.yml up -d --no-deps $SERVICES
  fi

  echo "⏳ Attente démarrage (45s)..."
  sleep 45

  echo "🏥 Health checks..."
  GATEWAY=\$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8080/actuator/health || echo "000")
  echo "  Gateway:      \$GATEWAY"

  FRONTEND=\$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:4200 || echo "000")
  echo "  Frontend:     \$FRONTEND"

  AUTH=\$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8081/actuator/health || echo "000")
  echo "  Auth:         \$AUTH"

  INCOME=\$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8082/actuator/health || echo "000")
  echo "  Income:       \$INCOME"

  SUBSCRIPTION=\$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8086/actuator/health || echo "000")
  echo "  Subscription: \$SUBSCRIPTION"

  ADMIN_FE=\$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:4201 || echo "000")
  echo "  Admin FE:     \$ADMIN_FE"

  echo ""
  if [ "\$GATEWAY" = "200" ] && [ "\$FRONTEND" = "200" ] && [ "\$AUTH" = "200" ]; then
    echo "✅ Déploiement réussi !"
    echo ""
    docker compose -f docker-compose.prod.yml ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
  else
    echo "❌ Health check échoué — services critiques non disponibles"
    echo ""
    echo "Logs des services en erreur :"
    [ "\$GATEWAY" != "200" ] && echo "--- Gateway ---" && docker compose -f docker-compose.prod.yml logs --tail=20 gateway-service
    [ "\$AUTH" != "200" ] && echo "--- Auth ---" && docker compose -f docker-compose.prod.yml logs --tail=20 auth-service
    [ "\$FRONTEND" != "200" ] && echo "--- Frontend ---" && docker compose -f docker-compose.prod.yml logs --tail=20 frontend
    echo ""
    echo "Pour rollback : ./scripts/rollback.sh"
    exit 1
  fi
ENDSSH
