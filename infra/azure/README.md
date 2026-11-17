# infra/azure

Demo environment on Azure: a single Ubuntu VM that runs the whole system with the same Docker Compose as locally. Created and removed with two scripts, so it only costs while it exists.

| | |
|---|---|
| Region | `chilecentral` (closest to Buenos Aires among the regions the subscription allows) |
| VM | `Standard_B2als_v2`: 2 vCPU, 4 GB RAM (the system uses ~1.5 GB), Ubuntu 24.04, 32 GB Standard SSD |
| Network | Public IP with a stable name, `http://shared-courier-pooling.chilecentral.cloudapp.azure.com`; inbound HTTP 80 for everyone, SSH 22 only from the IP that ran `up.sh` |
| Setup | [`cloud-init.yaml`](cloud-init.yaml), on first boot: Docker, clone the repo, `osrm/prepare.sh`, `docker compose --profile app up -d --build` (app on port 80) |
| Safety net | The VM shuts itself down daily at 02:00 UTC (23:00 Buenos Aires) |

## Usage

Requires the Azure CLI, logged in (`az login`; the tenant may ask for MFA when the session expires), and an SSH key at `~/.ssh/id_ed25519.pub`. The VM clones `REPO_BRANCH` (default `main`) from GitHub, so changes must be pushed first.

```bash
./infra/azure/up.sh     # ~3 min to create + ~15-20 min for the VM to set itself up; prints the URL
./infra/azure/down.sh   # deletes everything (asks to type the resource group name)
```

Follow the setup and check the services:

```bash
ssh azureuser@<ip> 'cloud-init status --wait; cd /opt/shared-courier-pooling && docker compose ps'
```

Settings (region, VM size, branch to deploy, ...) are in [`config.sh`](config.sh) and can be overridden from the environment, e.g. `REPO_BRANCH=my-branch ./infra/azure/up.sh`.

## Cost

About USD 0.06/hour while it exists (VM + disk + public IP). `down.sh` brings it back to zero. A stopped but not deleted VM still costs its disk and public IP (~USD 7/month), so prefer `down.sh` over just stopping it.

Every `up.sh` starts from scratch: fresh database with the case study shops and a new IP, but the same public link (`DNS_LABEL` in `config.sh`).

## One-time subscription setup

New subscriptions need the resource providers registered (free; done once):

```bash
for p in Microsoft.Compute Microsoft.Network Microsoft.Storage Microsoft.DevTestLab; do az provider register -n $p; done
```
