require 'bitescript'

java_import 'lila.runtime.LilaObject'

java_import 'java.lang.invoke.MethodHandle'
java_import 'java.lang.invoke.MethodHandles'
java_import 'java.lang.invoke.MethodType'

module Lila
  class CompiledExpression <
    Struct.new :name, :code, :functions
  end

  class Context
    @@function_id = 0
    @@name_id = 0

    def self.new_function_name
      @@function_id += 1
      '__fun' + @@function_id.to_s
    end

    def self.new_name
      @@name_id += 1
      '$' + @@name_id.to_s
    end

    attr_accessor :functions, :parent, :function

    def initialize(parent = nil)
      @parent = parent
      @functions = {}
    end

    def register_internal_function(name, type)
      if @parent
        @parent.register_internal_function name, type
      else
        @functions[name] = type
      end
    end

    def find_parameter(name)
      (@function.parameters.find { |parameter|
        parameter.name == name
       } if @function) || @parent.find_parameter(name) if @parent
    end
  end

  class Compiler
    def initialize
      @class_id = 0
    end

    def new_class_builder(opts = {})
      # generate new class name
      @class_id += 1
      name = '__lila' + @class_id.to_s
      # common superclass is always LilaObject
      opts[:widen] = Proc.new { |a, b|
        BiteScript::Signature::path(LilaObject)
      }
      BiteScript::ClassBuilder.new nil, name, nil, opts
    end

    # compile an expression into a class
    def compile_expression(expression, context)
      # initialize set of functions and
      # create new class builder
      context.functions = {}
      builder = new_class_builder

      # create entry point into expression
      builder.public_static_method "run", [], LilaObject do |method|
        expression.compile(context, method)
        method.areturn
      end

      # return name and bytecode of class,
      # as well as function names created during compilation
      CompiledExpression.new builder.class_name,
        bytecode(builder),
        context.functions
    end

    def bytecode(builder)
      class_writer = builder.instance_variable_get("@class_writer")
      class_writer.to_byte_array
    end
  end
end
