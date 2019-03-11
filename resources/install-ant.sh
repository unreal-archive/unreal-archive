#!/bin/bash

ANT_VERSION=1.10.5

ANT_URL="https://www-eu.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.zip"

ANT_HOME=~/.ant/ant

# download and install ant
if [ ! -d "${ANT_HOME}/bin" ]; then
  echo "Downloading ant binary from ${ANT_URL}"
  curl "$ANT_URL" --output /tmp/ant-bin.zip
  (cd /tmp && unzip ant-bin.zip && rm ant-bin)
  mv "/tmp/apache-ant-${ANT_VERSION}" $ANT_HOME
  (cd $ANT_HOME && ./bin/ant -f fetch.xml -Ddest=system junit)
fi
