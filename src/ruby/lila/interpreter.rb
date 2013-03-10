require 'lila/nodes'
require 'lila/parser'
require 'lila/compiler'
require 'java'

java_import 'lila.runtime.DynamicClassLoader'
java_import 'lila.runtime.RT'
java_import 'lila.runtime.Core'
java_import 'lila.runtime.LilaObject'
java_import 'lila.runtime.LilaClass'
java_import 'lila.runtime.dispatch.Utils'


module Lila
  class Interpreter

    def initialize
      @loader = DynamicClassLoader.new
      @compiler = Compiler.new
      @context = Context.new
      @eval = method(:eval)
    end

    def run_file(filename)
      source = IO.read(File.expand_path(filename))

      Parser.parse(source).statements.each do |statement|
        statement.interpret @eval
      end
    end

    def dump(expression)
      File.open('/tmp/' + expression.name + '.class', 'w') do |file|
        file.write(expression.code)
      end
    end

    def eval(statement)
      # compile the expression into a class
      expression = @compiler.compile_expression(statement, @context)

      dump expression

      clazz = @loader.define(expression.name, expression.code)

      # register internal functions created during compilation
      expression.functions.each { |name, type_rest|
        type, rest = type_rest
        # convert types to java classes
        type = type.map { |c| c.java_class }
        rtype, *ptypes = *type
        ptypes = ptypes.to_java(Java::java.lang.Class)
        # finally, call into runtime
        RT.registerInternalFunction clazz, name, rest, rtype, ptypes
      }

      @loader.run(clazz, 'run')
    end
  end
end
