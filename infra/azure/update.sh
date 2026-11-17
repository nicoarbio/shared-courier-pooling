#!/usr/bin/env bash
# Updates the running demo VM to the latest code, without recreating it (seconds instead of ~20 min):
# pulls the deployed branch, rebuilds the images built from the repo (backend, frontend) and recreates only the
# containers that changed. Data (PostgreSQL, Nominatim) lives in volumes and is kept; the app is down ~15 s.
#
# Also refreshes the SSH firewall rule to this machine's current public IP, in case it changed since up.sh.
set -euo pipefail
cd "$(dirname "$0")"
source ./config.sh

az account get-access-token -o none 2>/dev/null || { echo "!! Azure session expired or missing: run 'az login' (with MFA) first"; exit 1; }
if [[ "$(az group exists --name "$RESOURCE_GROUP")" != "true" ]]; then
  echo "!! Resource group '$RESOURCE_GROUP' does not exist: create the environment with up.sh"
  exit 1
fi

MY_IP="$(curl -fsS https://api.ipify.org)"
echo ">> Allowing SSH from $MY_IP..."
az network nsg rule update --resource-group "$RESOURCE_GROUP" --nsg-name "${VM_NAME}NSG" --name allow-ssh-from-admin \
  --source-address-prefixes "$MY_IP" -o none

HOST="$DNS_LABEL.$LOCATION.cloudapp.azure.com"
echo ">> Updating $HOST..."
# accept-new: trust the VM's key the first time (it changes when up.sh recreates the VM).
ssh -o StrictHostKeyChecking=accept-new -o ConnectTimeout=20 "$ADMIN_USER@$HOST" '
  set -e
  cd /opt/shared-courier-pooling
  git pull --ff-only
  git log --oneline -1
  sudo docker compose --profile app up -d --build
  sudo docker compose --profile app ps --format "{{.Service}}: {{.Status}}"
'

echo ">> Waiting for the app..."
for _ in $(seq 1 30); do
  if [[ "$(curl -s -o /dev/null -w '%{http_code}' "http://$HOST/api/merchants")" == "200" ]]; then
    echo ">> Up: http://$HOST"
    exit 0
  fi
  sleep 5
done
echo "!! The app did not answer after 150 s; check: ssh $ADMIN_USER@$HOST 'cd /opt/shared-courier-pooling && sudo docker compose logs --tail 50 backend'"
exit 1
