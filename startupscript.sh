#!/bin/bash

java -Xss512k \
    -Dio.netty.resolver.dns.DnsServerAddressStreamProviders=default \
    -Duser.timezone=Asia/Kolkata \
    -jar /Users/legend/IdeaProjects/tsqco/target/tsqco-1.0.1-SNAPSHOT.jar
