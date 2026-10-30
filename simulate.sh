#!/usr/bin/env bash
BASE=${BASE:-http://localhost:8080}
ADMIN=${ADMIN:-dev-admin-key}
N=${N:-5}

keys=()
for i in $(seq 1 "$N"); do
  lat=$(awk -v i="$i" 'BEGIN{printf "%.4f", -26.2041 + i*0.0008}')
  lon=$(awk -v i="$i" 'BEGIN{printf "%.4f", 28.0473 + i*0.0008}')
  res=$(curl -s -X POST "$BASE/api/devices" -H "X-Admin-Key: $ADMIN" \
        -H 'Content-Type: application/json' \
        -d "{\"name\":\"sim-device-$i\",\"latitude\":$lat,\"longitude\":$lon}")
  keys+=("$(echo "$res" | sed -E 's/.*"apiKey":"([^"]+)".*/\1/')")
done

send() {
  curl -s -o /dev/null -w "%{http_code}\n" -X POST "$BASE/api/devices/events" \
    -H "X-API-Key: $1" -H 'Content-Type: application/json' -d "{\"status\":\"$2\"}"
}

echo "Power cut: all devices OFF"
for k in "${keys[@]}"; do send "$k" OFF; sleep 0.3; done
curl -s "$BASE/api/outages"; echo

echo "Power back: 3 devices RESTORED"
for k in "${keys[@]:0:3}"; do send "$k" RESTORED; done
curl -s "$BASE/api/outages"; echo