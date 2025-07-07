#!/bin/sh

set -xe

./gradlew buildJAR
cd steam || exit 1
rm -f ./content/StatsTracker.jar
cp ../build/libs/StatsTracker.jar ./content/StatsTracker.jar
java -jar ~/.steam/steam/steamapps/common/SlayTheSpire/mod-uploader.jar upload -w .
