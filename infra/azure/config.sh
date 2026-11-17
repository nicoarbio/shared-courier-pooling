# Settings shared by up.sh and down.sh. Override any of them from the environment, e.g. VM_SIZE=Standard_B2s ./up.sh

# Everything lives in one resource group, so down.sh can remove it all at once (and costs go back to zero).
RESOURCE_GROUP="${RESOURCE_GROUP:-courier-pooling-rg}"
# Closest allowed region to Buenos Aires (the "Azure for Students" subscription only allows a few regions).
LOCATION="${LOCATION:-chilecentral}"

VM_NAME="${VM_NAME:-courier-pooling-vm}"
# Stable public name: http://<DNS_LABEL>.<LOCATION>.cloudapp.azure.com keeps working across down.sh/up.sh,
# even though the IP behind it changes. Must be unique in the region.
DNS_LABEL="${DNS_LABEL:-shared-courier-pooling}"
# 2 vCPU, 4 GB RAM: the whole system uses ~1.5 GB (Nominatim ~0.7 GB, backend ~0.4 GB, OSRM ~0.2 GB).
VM_SIZE="${VM_SIZE:-Standard_B2als_v2}"
VM_IMAGE="${VM_IMAGE:-Canonical:ubuntu-24_04-lts:server:latest}"
OS_DISK_GB="${OS_DISK_GB:-32}"
ADMIN_USER="${ADMIN_USER:-azureuser}"
SSH_PUBLIC_KEY="${SSH_PUBLIC_KEY:-$HOME/.ssh/id_ed25519.pub}"

# What the VM deploys.
REPO_URL="${REPO_URL:-https://github.com/nicoarbio/shared-courier-pooling.git}"
REPO_BRANCH="${REPO_BRANCH:-main}"

# Monthly cost alert (budget.sh): emails at 80% and 100% of this amount (USD).
BUDGET_AMOUNT="${BUDGET_AMOUNT:-30}"

TAGS="project=shared-courier-pooling"
