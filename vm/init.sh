#!/bin/bash
if [ ! -d /mnt/git ] ; then
    git clone https://github.com/Alteiria/dynamicDigitalocean.git /mnt/git
else
    cd /mnt/git && git pull
fi
