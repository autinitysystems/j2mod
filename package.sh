#!/bin/sh
cd $(dirname $0)/bin
jar -cvf ../j2mod.jar $(find . -name '*.class')
