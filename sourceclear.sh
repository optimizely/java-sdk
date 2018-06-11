#!/bin/sh

if [ "$RUN_SOURCECLEAR" = "true" ]; then
    echo "RUN_SOURCECLEAR is set, running sourceclear"
    curl -sSL https://download.sourceclear.com/ci.sh |  bash
else
    echo "RUN_SOURCECLEAR it not set, skipping"
fi
