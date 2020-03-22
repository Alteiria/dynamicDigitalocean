FROM alpine:3.11

ENV SLEEP_MIN=30 \
      HOSTNAME=\
      DO_API_TOKEN=

RUN addgroup -g 1000 -S checker && \
    adduser -u 1000 -S checker -G checker

RUN apk add --no-cache bash jq npm nodejs

RUN npm i -g mc-server-status

WORKDIR /tmp

RUN wget https://github.com/digitalocean/doctl/releases/download/v1.39.0/doctl-1.39.0-linux-amd64.tar.gz -O /tmp/doctl.tar.gz

RUN tar xzvf doctl.tar.gz

RUN mv ./doctl /usr/bin

RUN rm /tmp/doctl.tar.gz

WORKDIR /

USER checker

ENTRYPOINT ["/ENTRYPOINT.sh"]

COPY ENTRYPOINT.sh /ENTRYPOINT.sh

LABEL maintainer="dynamicDigitalocean <https://github.com/Alteiria/dynamicDigitalocean>" \
      description="Dynamically deploy Minecraft servers to Digitalocean with the help of Pterodactyl, Bungeecord and Alpine Linux." \
      version="0.0.1"