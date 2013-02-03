#!/bin/sh
CLASSPATH=build jruby -Isrc/ruby src/ruby/lila.rb $@
