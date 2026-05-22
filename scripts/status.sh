#!/bin/bash

VPS_IP="178.105.151.226"
VPS_USER="deploy"

echo "📊 Joseph·Yusuf — Status Production"
echo "====================================="

ssh "$VPS_USER@$VPS_IP" bash -s <<'ENDSSH'
  echo "🐳 Containers Docker :"
  docker compose -f /opt/joseph-yusuf/docker-compose.prod.yml ps

  echo ""
  echo "💾 Ressources système :"
  free -h
  df -h /

  echo ""
  echo "🌐 Health checks services :"
  services=(
    "Discovery:8761"
    "Gateway:8080"
    "Auth:8081"
    "Income:8082"
    "RuleEngine:8083"
    "Alert:8084"
    "Report:8085"
    "Subscription:8086"
    "Admin:8087"
    "Support:8088"
  )

  for service in "${services[@]}"; do
    name="${service%%:*}"
    port="${service##*:}"
    status=$(curl -s -o /dev/null -w "%{http_code}" \
      http://localhost:$port/actuator/health 2>/dev/null || echo "000")
    if [ "$status" = "200" ]; then
      echo "  ✅ $name ($port)"
    else
      echo "  ❌ $name ($port) — HTTP $status"
    fi
  done

  echo ""
  echo "🖥️ Frontends :"
  FRONTEND=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:4200 2>/dev/null || echo "000")
  ADMIN=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:4201 2>/dev/null || echo "000")
  [ "$FRONTEND" = "200" ] && echo "  ✅ Frontend (4200)" || echo "  ❌ Frontend (4200) — HTTP $FRONTEND"
  [ "$ADMIN" = "200" ] && echo "  ✅ Admin Frontend (4201)" || echo "  ❌ Admin Frontend (4201) — HTTP $ADMIN"

  echo ""
  echo "🌍 URLs publiques :"
  domains=(
    "https://josephyusuf.com"
    "https://admin.josephyusuf.com"
    "https://api.josephyusuf.com/actuator/health"
  )

  for url in "${domains[@]}"; do
    status=$(curl -s -o /dev/null -w "%{http_code}" \
      --connect-timeout 5 "$url" 2>/dev/null || echo "000")
    if [ "$status" = "200" ] || [ "$status" = "301" ] || [ "$status" = "302" ]; then
      echo "  ✅ $url"
    else
      echo "  ❌ $url — HTTP $status"
    fi
  done

  echo ""
  echo "📋 Logs récents (erreurs) :"
  docker compose -f /opt/joseph-yusuf/docker-compose.prod.yml logs --tail=5 --no-color 2>/dev/null | grep -i "error\|exception" | tail -5 || echo "  Aucune erreur récente"
ENDSSH
