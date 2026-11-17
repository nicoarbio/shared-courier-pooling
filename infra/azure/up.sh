#!/usr/bin/env bash
# Creates the demo environment on Azure: one Ubuntu VM that installs Docker and runs the whole system
# (see cloud-init.yaml). Everything goes into a single resource group; remove it all with down.sh.
#
# Requires: Azure CLI logged in (az login). Takes ~3 min to create, then ~15-20 min for the VM to set itself up.
# Cost while it exists: ~USD 0.06/hour (Standard_B2als_v2 in Chile Central) + disk and public IP.
set -euo pipefail
cd "$(dirname "$0")"
source ./config.sh

# get-access-token really validates the session (account show only reads the local cache); the tenant may require MFA.
az account get-access-token -o none 2>/dev/null || { echo "!! Azure session expired or missing: run 'az login' (with MFA) first"; exit 1; }
if [[ "$(az group exists --name "$RESOURCE_GROUP")" == "true" ]]; then
  echo "!! Resource group '$RESOURCE_GROUP' already exists: the environment is up (or being removed). Run down.sh first."
  exit 1
fi
[[ -f "$SSH_PUBLIC_KEY" ]] || { echo "!! SSH public key not found: $SSH_PUBLIC_KEY"; exit 1; }

# SSH is only allowed from the current public IP of this machine.
MY_IP="$(curl -fsS https://api.ipify.org)"
CLOUD_INIT="$(mktemp)"
trap 'rm -f "$CLOUD_INIT"' EXIT
sed -e "s|__REPO_URL__|$REPO_URL|g" -e "s|__REPO_BRANCH__|$REPO_BRANCH|g" -e "s|__ADMIN_USER__|$ADMIN_USER|g" \
  cloud-init.yaml > "$CLOUD_INIT"

echo ">> Subscription: $(az account show --query name -o tsv)"
echo ">> Creating resource group '$RESOURCE_GROUP' in $LOCATION..."
az group create --name "$RESOURCE_GROUP" --location "$LOCATION" --tags $TAGS -o none

echo ">> Creating VM '$VM_NAME' ($VM_SIZE)..."
az vm create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$VM_NAME" \
  --location "$LOCATION" \
  --image "$VM_IMAGE" \
  --size "$VM_SIZE" \
  --os-disk-size-gb "$OS_DISK_GB" \
  --storage-sku StandardSSD_LRS \
  --admin-username "$ADMIN_USER" \
  --ssh-key-values "$SSH_PUBLIC_KEY" \
  --public-ip-sku Standard \
  --public-ip-address-dns-name "$DNS_LABEL" \
  --nsg-rule NONE \
  --custom-data "$CLOUD_INIT" \
  --tags $TAGS \
  -o none

# Firewall: the app (HTTP 80) for everyone; SSH only from here.
NSG="${VM_NAME}NSG"
az network nsg rule create --resource-group "$RESOURCE_GROUP" --nsg-name "$NSG" --name allow-http \
  --priority 100 --direction Inbound --access Allow --protocol Tcp --destination-port-ranges 80 -o none
az network nsg rule create --resource-group "$RESOURCE_GROUP" --nsg-name "$NSG" --name allow-ssh-from-admin \
  --priority 110 --direction Inbound --access Allow --protocol Tcp --destination-port-ranges 22 \
  --source-address-prefixes "$MY_IP" -o none

IP="$(az vm show --show-details --resource-group "$RESOURCE_GROUP" --name "$VM_NAME" --query publicIps -o tsv)"
URL="http://$DNS_LABEL.$LOCATION.cloudapp.azure.com"
cat <<EOF

>> VM created. It is now installing Docker and starting the system (~15-20 min).
   Follow it:  ssh $ADMIN_USER@$IP 'cloud-init status --wait; cd /opt/shared-courier-pooling && docker compose ps'
   Then open:  $URL   (public link; also http://$IP)
   Remove it:  ./down.sh   (it keeps running, and costing, until then; see budget.sh for a cost alert)
EOF
