#!/bin/bash
if [ ! -f "/config/core.json" ]; then
    echo "pterodactyl daemon configuration isn't mounted. Exiting"
    exit 1
fi

access_token=$(jq -r '.keys'[0] /config/core.json)
servers=$(curl -k https://daemon:8080/v1/servers)
counter=0
sleep_10_seconds=$(( 6*$SLEEP_MIN ))

for serverID in $(jq -r '. | keys | .[]' ${servers}); do
    curl -X PUT -d '{"action":"start"}' -H "X-Access-Server: ${serverID}" \
    -H "X-Access-Token: ${access_token}" -H "Content-Type: application/json" https://daemon:8080/v1/server/power
done

sleep 3m

while [ $counter -le ${sleep_10_seconds} ]
do
    if [[ $(mc-status --json ${HOSTNAME} | jq '.players.online') -eq 0 ]]; then
        counter=$(( $counter + 1 ))
    else
        counter=0
    fi
    sleep 10s
done

for serverID in $(jq -r '. | keys | .[]' ${servers}); do
    curl -X PUT -d '{"action":"stop"}' -H "X-Access-Server: ${serverID}" \
    -H "X-Access-Token: ${access_token}" -H "Content-Type: application/json" https://daemon:8080/v1/server/power
done

/usr/bin/doctl compute droplet delete -t ${DO_API_TOKEN} -f ${HOSTNAME}