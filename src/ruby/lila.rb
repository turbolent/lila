(puts("Usage: lila <path>"); exit) if ARGV.empty?

require 'lila/interpreter'
require 'java'
java_import 'lila.runtime.RT'

RT.initialize
Lila::Interpreter.new.run_file(ARGV.first)
