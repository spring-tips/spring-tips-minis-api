#!/usr/bin/env bash
set -e
set -o pipefail
HOST=localhost:8080
ID=$1
OUTPUT_FN=${ID}.jpg
echo "calling http://${HOST}/tips/previews/${ID}"
curl -u ${SPRING_TIPS_BITES_USERNAME}:${SPRING_TIPS_BITES_PASSWORD} http://${HOST}/tips/previews/${ID} --output "${OUTPUT_FN}"
open "$OUTPUT_FN"
