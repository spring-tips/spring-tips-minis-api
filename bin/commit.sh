#!/usr/bin/env bash
set -e
set -o pipefail
cd $(dirname "$0") && cd .. && mvn -DskipTests spring-javaformat:apply clean  package && git commit -am polish && git push
