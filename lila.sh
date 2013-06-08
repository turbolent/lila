#!/bin/sh
DIR=$( cd "$( dirname "$0" )" && pwd )
JRUBY=${JRUBY:-jruby}
CLASSPATH=$DIR/bin:$DIR/lib/asm-4.1.jar $JRUBY -I$DIR/src/ruby $DIR/src/ruby/lila.rb $@
