require 'optparse'

$compile = false
$filename = nil

optparse = OptionParser.new do |o|
  o.on('-c') { |b| $compile = b }
  o.on('-f FILENAME') { |filename| $filename = filename }
  o.on('-h') { puts o; exit }
end

begin
  optparse.parse!
  unless $filename
    puts 'missing filename'
    puts optparse
    exit
  end
rescue OptionParser::InvalidOption, OptionParser::MissingArgument
  puts $!.to_s
  puts optparse
  exit 
end

if $compile then
  require 'lila/compiler'
  Lila::Compiler.new.compile_file $filename
else
  require 'lila/interpreter'
  require 'java'
  java_import 'lila.runtime.RT'
  RT.initialize
  Lila::Interpreter.new.run_file $filename
end
