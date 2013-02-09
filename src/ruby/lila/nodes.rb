require 'bitescript/bytecode'
require 'java'

java_import 'lila.runtime.LilaObject'
java_import 'lila.runtime.LilaString'
java_import 'lila.runtime.LilaInteger'
java_import 'lila.runtime.LilaBoolean'
java_import 'lila.runtime.LilaFunction'
java_import 'lila.runtime.RT'
java_import 'lila.runtime.StringNames'

java_import 'java.lang.invoke.MethodType'
java_import 'java.lang.invoke.MethodHandle'
java_import 'java.lang.invoke.CallSite'
java_import 'java.lang.invoke.MethodHandles$Lookup'

module Lila

  class Program < Struct.new :statements; end

  class MethodDefinition < Struct.new \
    :name, :parameters, :expressions
  end

  class ClassDefinition < Struct.new :name, :superclasses; end

  class VariableDefinition < Struct.new :name, :value; end

  class Expression
    def is_true(builder)
      builder.invokevirtual LilaObject, 'isTrue', [Java::boolean]
    end

#    def to_s
#      inspect
#    end

    # assumes constant on stack
    def box(builder, type, boxed_type, value = @value)
      builder.new boxed_type
      builder.dup_x1
      builder.swap
      builder.invokespecial boxed_type, '<init>', [Java::void, type]
    end

    def box_integer(builder)
      box builder, Java::int, LilaInteger
    end

    def box_string(builder)
      box builder, Java::java.lang.String, LilaString
    end

    def box_boolean(builder)
      builder.invokestatic LilaBoolean, 'box', [LilaBoolean, Java::boolean]
    end
  end

  class Value < Expression
    def initialize(value)
      @value = value
    end

    def close(context) end
  end

  class IntegerValue < Value
    def compile(context, builder)
      builder.ldc @value
      box_integer builder
    end
  end

  class StringValue < Value
    def compile(context, builder)
      builder.ldc @value
      box_string builder
    end
  end

  class BooleanValue < Value
    def compile(context, builder)
      builder.ldc @value
      box_boolean builder
    end
  end

  class Identifier < Expression
    attr_accessor :name

    def initialize(name)
      @name = name
    end

    def compile(context, builder)
      if @parameter
        index = @parameter.function.parameter_index(@parameter)
        builder.aload index
      else
        bootstrap = builder.h_invokestatic RT, "bootstrapValue",
          CallSite, Lookup, Java::java.lang.String, MethodType
        encoded_name = StringNames.toBytecodeName(@name)
        builder.invokedynamic encoded_name, [LilaObject], bootstrap
      end
    end

    def close(context)
      parameter = context.find_parameter self.name
      if parameter
        @parameter = maybe_close_parameter context, parameter
      end
    end

    def maybe_close_parameter(context, parameter)
      if parameter.function == context.function
        parameter
      else
        parameter = maybe_close_parameter context.parent, parameter
        context.function.add_closed_parameter parameter
      end
    end
  end

  class Function < Expression
    attr_reader :closed_parameters, :parameters

    def initialize(parameters, expressions)
      self.parameters = parameters
      @expressions = expressions
      # ordered set
      @closed_parameters = []
    end

    def parameter_index(parameter)
      @closed_parameters.index(parameter) ||
        (@closed_parameters.length +
          @parameters.index(parameter))
    end

    def add_closed_parameter(parameter)
      closed_parameter = @closed_parameters.find { |p|
        p.name == parameter.name
      }
      unless closed_parameter
        closed_parameter = ClosedParameter.new parameter
        closed_parameter.function = self
        @closed_parameters << closed_parameter
      end
      closed_parameter
    end

    def parameters=(parameters)
      @parameters = parameters
      @parameters.each { |parameter|
        parameter.function = self
      }
    end

    def compile(context, builder)
      name = Context.new_function_name

      close context

      # define new toplevel method
      param_count = (@closed_parameters.length + @parameters.length)
      param_type = [LilaObject] * param_count

      builder.class_builder.public_static_method name, [],
        LilaObject, *param_type do |method|
          @expressions.each { |expression|
            expression.compile(context, method)
          }
          method.areturn
        end

      # ensure function is registered after class is loaded,
      # so bootstrap (lookup) will succeed
      function_type = [LilaObject] + param_type
      context.register_internal_function name, function_type

      # link function value
      bootstrap = builder.h_invokestatic RT, 'bootstrapFunction',
        CallSite, Lookup, Java::java.lang.String, MethodType
      encoded_name = StringNames.toBytecodeName(name)
      builder.invokedynamic encoded_name, [LilaFunction], bootstrap

      unless @closed_parameters.empty?
        @closed_parameters.each { |closed_parameter|
          target_parameter = closed_parameter.parameter
          index = target_parameter.function.parameter_index(target_parameter)
          builder.aload index
          close_type = [LilaFunction, LilaObject]
          builder.invokevirtual LilaFunction, 'close', close_type
        }
      end
    end

    def close(context)
      unless @closed
        context = Context.new(context)
        context.function = self
        @expressions.each { |expression|
          expression.close context
        }
        @closed = true
      end
    end
  end

  class Parameter
    attr_accessor :function
    attr_reader :name
    attr_reader :type

    def initialize(name, type = nil)
      @name = name
      @type = type
    end
  end

  class ClosedParameter < Parameter
    attr_reader :parameter

    def initialize(parameter)
      super parameter.name
      @parameter = parameter
    end
  end

  class Call < Expression
    lilaObject_array = [].to_java(LilaObject).java_class

    def initialize(expression, arguments)
      @expression = expression
      @arguments = arguments
    end

    def compile(context, builder)
      @expression.compile(context, builder)
      @arguments.compile(context, builder)
      bootstrap = builder.h_invokestatic RT, "bootstrapCall",
        CallSite, Lookup, Java::java.lang.String, MethodType
      # +2: return value, function argument
      type = [LilaObject] * (2 + @arguments.length)
      builder.invokedynamic 'call', type, bootstrap
    end

    def close(context)
      @expression.close context
      @arguments.close context
    end
  end

  class Arguments
    def initialize(expressions)
      @expressions = expressions
    end

    def length
      @expressions.length
    end

    def compile(context, builder)
      @expressions.each { |argument|
        argument.compile context, builder
      }
    end

    def close(context)
      @expressions.each { |expression|
        expression.close context
      }
    end
  end

  class Conditional < Expression
    def initialize(test, consequent, alternate)
      @test = test
      @consequent = consequent
      @alternate = alternate
    end

    def compile(context, builder)
      @test.compile(context, builder)
      is_true builder
      builder.ifeq :else
      @consequent.compile(context, builder)
      builder.goto :end
      builder.label :else
      @alternate.compile(context, builder)
      builder.label :end
    end

    def close(context)
      [@test, @consequent, @alternate].each { |expression|
        expression.close context
      }
     end
  end

  class Sequence < Expression
    def initialize(expressions)
      @expressions = expressions
    end

    def compile(context, builder)
      @expressions.each { |expression|
        expression.compile(context, builder)
      }
    end

    def close(context)
      @expressions.each { |expression|
        expression.close context
      }
    end
  end
end
