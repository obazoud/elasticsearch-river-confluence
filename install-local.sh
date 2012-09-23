#!/bin/bash

mvn clean install
mkdir -p /opt/elasticsearch/plugins/elasticsearch-river-confluence
rm -rf /opt/elasticsearch/plugins/elasticsearch-river-confluence/*
unzip target/releases/elasticsearch-river-confluence-0.0.1-SNAPSHOT.zip -d /opt/elasticsearch/plugins/elasticsearch-river-confluence

