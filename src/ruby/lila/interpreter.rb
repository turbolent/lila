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
    end

    def run_file(filename)
      source = IO.read(File.expand_path(filename))
      Parser.parse(source).statements.each do |statement|
        case statement
        when Expression
          puts eval(statement)
        when VariableDefinition
          value = eval statement.value
          puts value
          RT.setValue statement.name, value
        when MethodDefinition
          function = eval Function.new(statement.parameter_list,
                                       statement.expressions)
          statement.predicate.resolveTypes { |expression|
            eval expression
          }
          gf = RT.findOrCreateGenericFunction statement.name
          gf.addMethod statement.predicate, function.javaValue
          gf.dumpMethods
          df = gf.toDispatchFunction
          puts df
          puts gf
        when ClassDefinition
          superclasses = statement.superclasses.map { |superclass|
              eval(superclass)
          }.to_java(LilaClass)
          lilaClass = LilaClass.make(statement.name, superclasses)
          RT.setValue statement.name, lilaClass
          puts lilaClass
        else
          puts "Unknown statement #{statement}"
        end
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
