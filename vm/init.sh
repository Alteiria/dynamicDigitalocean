#!/bin/bash
if [ ! -d /srv/git ] ; then
    git clone https://github.com/Alteiria/dynamicDigitalocean.git /srv/git
else
    cd /srv/git && git pull
fi
