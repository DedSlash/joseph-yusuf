#!/bin/bash
set -e

echo "🚀 Joseph·Yusuf — Premier déploiement production"
echo "================================================="

VPS_IP="178.105.151.226"
VPS_USER="deploy"
REPO_URL="https://github.com/DedSlash/joseph-yusuf.git"
PROJECT_DIR="/opt/joseph-yusuf"

if [ ! -f ".env.prod" ]; then
  echo "❌ .env.prod introuvable à la racine du projet."
  echo "   Copie .env.prod.example en .env.prod et renseigne les valeurs production."
  exit 1
fi

# Vérifier la connexion SSH
echo "🔑 Test connexion SSH..."
if ! ssh -o ConnectTimeout=10 "$VPS_USER@$VPS_IP" "echo ok" > /dev/null 2>&1; then
  echo "❌ Impossible de se connecter à $VPS_USER@$VPS_IP"
  echo "   Vérifie ta clé SSH et que le user 'deploy' existe sur le VPS."
  exit 1
fi

# 1. Connexion et setup initial
echo "📁 Setup initial sur le VPS..."
ssh "$VPS_USER@$VPS_IP" bash -s <<ENDSSH
  set -e

  echo "📁 Création structure dossiers..."
  sudo mkdir -p $PROJECT_DIR
  sudo chown -R $VPS_USER:$VPS_USER $PROJECT_DIR
  cd $PROJECT_DIR

  echo "📥 Clone du repository..."
  if [ -d ".git" ]; then
    git pull origin main
  else
    git clone $REPO_URL .
  fi

  echo "✅ Repository prêt"
ENDSSH

# 2. Copier le fichier .env.prod sur le VPS
echo "🔐 Copie des variables d'environnement..."
scp .env.prod "$VPS_USER@$VPS_IP:$PROJECT_DIR/.env"
ssh "$VPS_USER@$VPS_IP" "chmod 600 $PROJECT_DIR/.env"

# 3. Lancer le déploiement sur le VPS
echo "🐳 Build et démarrage des services..."
ssh "$VPS_USER@$VPS_IP" bash -s <<ENDSSH
  set -e
  cd $PROJECT_DIR

  echo "🐳 Build des images Docker (cela peut prendre 10-15 min)..."
  docker compose -f docker-compose.prod.yml build --no-cache

  echo "🚀 Démarrage des services..."
  docker compose -f docker-compose.prod.yml up -d

  echo "⏳ Attente démarrage des services (90s)..."
  sleep 90

  echo "🏥 Health checks..."
  echo ""
  services=(
    "PostgreSQL:postgresql"
    "Redis:redis"
    "Kafka:kafka"
    "Discovery:discovery-server"
    "Gateway:gateway-service"
    "Auth:auth-service"
    "Income:income-service"
    "RuleEngine:rule-engine-service"
    "Alert:alert-service"
    "Report:report-service"
    "Subscription:subscription-service"
    "Admin:admin-service"
    "Support:support-service"
    "Frontend:frontend"
    "AdminFrontend:admin-frontend"
  )

  FAILED=0
  for service in "\${services[@]}"; do
    name="\${service%%:*}"
    container="\${service##*:}"
    status=\$(docker compose -f docker-compose.prod.yml ps --format '{{.Status}}' \$container 2>/dev/null | head -1)
    if echo "\$status" | grep -qi "up\|healthy"; then
      echo "  ✅ \$name"
    else
      echo "  ❌ \$name — \$status"
      FAILED=1
    fi
  done

  echo ""
  if [ "\$FAILED" = "0" ]; then
    echo "✅ Tous les services sont opérationnels !"
  else
    echo "⚠️  Certains services ne sont pas encore prêts."
    echo "   Vérifie les logs : docker compose -f docker-compose.prod.yml logs -f"
  fi

  echo ""
  docker compose -f docker-compose.prod.yml ps
ENDSSH

echo ""
echo "✅ Premier déploiement terminé !"
echo "🌐 Site accessible sur https://josephyusuf.com"
echo "🔧 Admin accessible sur https://admin.josephyusuf.com"
echo "📡 API accessible sur https://api.josephyusuf.com"
echo ""
echo "📋 Prochaines étapes :"
echo "   1. Vérifier les health checks : ./scripts/status.sh"
echo "   2. Configurer Nginx + SSL si pas encore fait"
echo "   3. Tester les URLs publiques"
