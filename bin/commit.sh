#!/usr/bin/env bash
set -e
set -o pipefail
cd $(dirname "$0") && cd .. && mvn spring-javaformat:apply && git commit -am polish && git push
