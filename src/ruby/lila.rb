(puts("Usage: lila <path>"); exit) if ARGV.empty?

require 'lila/interpreter'

Lila::Interpreter.new.run_file(ARGV.first)
