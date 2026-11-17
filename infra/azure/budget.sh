#!/usr/bin/env bash
# Creates (or updates) a monthly cost budget for the whole subscription, with email alerts at 80% and 100%.
# Run once per subscription; it is independent of up.sh/down.sh. The email is passed in, never stored in the repo.
#
# Usage: BUDGET_EMAIL=you@example.com ./infra/azure/budget.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./config.sh

: "${BUDGET_EMAIL:?Set BUDGET_EMAIL to the address that receives the alerts}"
az account get-access-token -o none 2>/dev/null || { echo "!! Azure session expired or missing: run 'az login' (with MFA) first"; exit 1; }

SUBSCRIPTION_ID="$(az account show --query id -o tsv)"
NAME="shared-courier-pooling-monthly"
START="$(date -u +%Y-%m-01)T00:00:00Z"   # budgets start on the first day of a month

notification() { # $1 = percentage
  printf '{"enabled": true, "operator": "GreaterThanOrEqualTo", "threshold": %s, "thresholdType": "Actual", "contactEmails": ["%s"]}' "$1" "$BUDGET_EMAIL"
}

az rest --method put \
  --url "https://management.azure.com/subscriptions/$SUBSCRIPTION_ID/providers/Microsoft.Consumption/budgets/$NAME?api-version=2023-05-01" \
  --body "{
    \"properties\": {
      \"category\": \"Cost\",
      \"amount\": $BUDGET_AMOUNT,
      \"timeGrain\": \"Monthly\",
      \"timePeriod\": {\"startDate\": \"$START\"},
      \"notifications\": {
        \"actual-80\": $(notification 80),
        \"actual-100\": $(notification 100)
      }
    }
  }" -o none

echo ">> Budget '$NAME': USD $BUDGET_AMOUNT/month, alerts at 80% and 100% to $BUDGET_EMAIL"
