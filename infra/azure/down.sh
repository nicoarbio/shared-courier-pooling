#!/usr/bin/env bash
# Removes the whole demo environment (the resource group with the VM, disk, network and public IP).
# After this, nothing keeps costing. Data in the VM (e.g. orders entered during a demo) is lost.
set -euo pipefail
cd "$(dirname "$0")"
source ./config.sh

if [[ "$(az group exists --name "$RESOURCE_GROUP")" != "true" ]]; then
  echo ">> Resource group '$RESOURCE_GROUP' does not exist: nothing to remove."
  exit 0
fi

echo ">> This deletes everything in resource group '$RESOURCE_GROUP':"
az resource list --resource-group "$RESOURCE_GROUP" --query "[].{name:name, type:type}" -o table
read -r -p ">> Type the resource group name to confirm: " answer
[[ "$answer" == "$RESOURCE_GROUP" ]] || { echo "!! Not confirmed, nothing removed."; exit 1; }

echo ">> Deleting (takes a few minutes)..."
az group delete --name "$RESOURCE_GROUP" --yes
echo ">> Done: nothing left running or billing."
