#!/bin/bash
set -e
apt-get update
apt-get install -y openjdk-17-jdk wget unzip
wget -q https://services.gradle.org/distributions/gradle-8.5-bin.zip -P /tmp
unzip -q -d /opt/gradle /tmp/gradle-8.5-bin.zip
ln -s /opt/gradle/gradle-8.5/bin/gradle /usr/local/bin/gradle
