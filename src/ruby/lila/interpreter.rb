require 'lila/nodes'
require 'lila/parser'
require 'lila/compiler'
require 'java'

java_import 'lila.runtime.DynamicClassLoader'
java_import 'lila.runtime.RT'
java_import 'lila.runtime.Core'
java_import 'lila.runtime.LilaObject'
java_import 'lila.runtime.LilaClass'


module Lila
  class Interpreter

    attr_reader :loader, :compiler, :context

    def initialize
      @loader = DynamicClassLoader.INSTANCE
      @compiler = Compiler.new
      @context = Context.new
    end

    def run_file(filename)
      source = IO.read(File.expand_path(filename))

      Parser.parse(source).statements.each do |statement|
        statement.interpret self
      end
    end

    def dump(result)
      File.open('/tmp/' + result.name + '.class', 'w') do |file|
        file.write(result.code)
      end
    end

    def load(result)
      clazz = @loader.define(result.name, result.code)
      # register internal functions created during compilation
      result.functions.each { |internal_name, name_type_rest|
        name, type, rest = name_type_rest
        # convert types to java classes
        type = type.map { |c| c.java_class }
        rtype, *ptypes = *type
        ptypes = ptypes.to_java Java::java.lang.Class
        # finally, call into runtime
        RT.registerInternalFunction clazz, internal_name, name, rest, rtype, ptypes
      }
      clazz
    end

    def eval(expression)
      # compile the expression into a class
      result = @compiler.compile_expression expression, @context
      dump result
      clazz = load result
      @loader.run clazz
    end
  end
end
