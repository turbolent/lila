require 'bitescript/bytecode'
require 'java'

java_import 'lila.runtime.LilaObject'
java_import 'lila.runtime.LilaString'
java_import 'lila.runtime.LilaInteger'
java_import 'lila.runtime.LilaBoolean'
java_import 'lila.runtime.LilaFunction'
java_import 'lila.runtime.LilaArray'
java_import 'lila.runtime.RT'
java_import 'lila.runtime.StringNames'
# NOTE: monkey patching java classes has several
# limitations, so shadow and subclass instead
java_import 'lila.runtime.Expression' do
  'InternalExpression'
end

java_import 'java.lang.invoke.MethodType'
java_import 'java.lang.invoke.MethodHandle'
java_import 'java.lang.invoke.CallSite'
java_import 'java.lang.invoke.MethodHandles$Lookup'

module Lila

  class Program < Struct.new :statements; end

  class MethodDefinition < Struct.new \
    :name, :parameter_list, :predicate, :expressions

    def interpret(evaluate)
      # create actual function
      function = evaluate.call \
        Function.new(self.parameter_list,
                     self.expressions)
      # evaluate type expressions inside predicate
      self.predicate.resolveTypes { |expression|
        evaluate.call expression
      }
      # add method to implicit generic function
      gf = RT.findOrCreateGenericFunction self.name
      gf.addMethod self.predicate, function.javaValue
      gf.dumpMethods
      df = gf.toDispatchFunction
      puts df
      puts gf
    end
  end

  class ClassDefinition < Struct.new :name, :superclasses
    def interpret(evaluate)
      superclasses = self.superclasses.map { |superclass|
        evaluate.call superclass
      }.to_java(LilaClass)
      lilaClass = LilaClass.make(self.name, superclasses)
      RT.setValue self.name, lilaClass
      puts lilaClass
    end
  end

  class VariableDefinition < Struct.new :name, :value
    def interpret(evaluate)
      value = evaluate.call self.value
      puts value
      RT.setValue self.name, value
    end
  end


  class Expression < InternalExpression
    def is_true(builder)
      builder.invokevirtual LilaObject, 'isTrue', [Java::boolean]
    end

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

    def interpret(evaluate)
      puts evaluate.call(self)
    end

#    def toString
#      "#[#{self.class.name}]"
#    end
  end

  class Value < Expression
    def initialize(value)
      super()
      @value = value
    end

    def toString
      @value.to_s
    end

    def resolveBindings(env)
      self
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

    def toString
      @value.inspect
    end
  end

  class BooleanValue < Value
    def compile(context, builder)
      builder.getstatic LilaBoolean,
        (if @value then :TRUE else :FALSE end),
        LilaBoolean
    end
  end

  class Identifier < Expression
    attr_accessor :name

    def initialize(name)
      super()
      @name = name
    end

    def toString
      @name
    end

    def resolveBindings(env)
      env[@name] || self
    end

    def compile(context, builder)
      if @parameter
        index = @parameter.function.parameter_list.index @parameter
        builder.aload index
      else
        bootstrap = builder.h_invokestatic RT, 'bootstrapValue',
          CallSite, Lookup, Java::java.lang.String, MethodType
        encoded_name = StringNames.toBytecodeName @name
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

  class ParameterList

    attr_reader :parameters, :closed_parameters, :rest

    def initialize(parameters, rest = false)
      @parameters = parameters
      # ordered set
      @rest = rest
      @closed_parameters = []
    end

    def index(parameter)
       @closed_parameters.index(parameter) ||
         (@closed_parameters.length +
           @parameters.index(parameter))
    end

    def length
      @closed_parameters.length + @parameters.length
    end

    def to_s
      "(#{@parameters.join(', ')})"
    end
  end

  class Function < Expression
    attr_reader :parameter_list

    def initialize(parameter_list, body)
      super()
      @parameter_list = parameter_list
      parameter_list.parameters.each { |parameter|
        parameter.function = self
      }
      @body = body
    end

    def resolveBindings(env)
      # TODO: remove parameter names from env?
      @body = @body.resolveBindings env
      self
    end

    def add_closed_parameter(parameter)
      closed_parameter = @parameter_list.closed_parameters.find { |p|
        p.name == parameter.name
      }
      unless closed_parameter
        closed_parameter = ClosedParameter.new parameter
        closed_parameter.function = self
        @parameter_list.closed_parameters << closed_parameter
      end
      closed_parameter
    end

    def compile(context, builder)
      name = Context.new_function_name

      close context

      # define new toplevel method
      param_type = [LilaObject] * @parameter_list.length
      # rest parameter is of type LilaArray
      if @parameter_list.rest
        param_type[-1] = LilaArray
      end

      builder.class_builder.public_static_method name, [],
        LilaObject, *param_type do |method|
          @body.compile(context, method)
          method.areturn
        end

      # ensure function is registered after class is loaded,
      # so bootstrap (lookup) will succeed
      function_type = [LilaObject] + param_type
      context.register_internal_function name, function_type,
        @parameter_list.rest

      # link function value
      bootstrap = builder.h_invokestatic RT, 'bootstrapFunction',
        CallSite, Lookup, Java::java.lang.String, MethodType
      encoded_name = StringNames.toBytecodeName(name)
      builder.invokedynamic encoded_name, [LilaFunction], bootstrap

      unless @parameter_list.closed_parameters.empty?
        @parameter_list.closed_parameters.each { |closed_parameter|
          target_parameter = closed_parameter.parameter
          index = target_parameter.function.parameter_list.index(target_parameter)
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
        @body.close context
        @closed = true
      end
    end

    def toString
      "function #{@parameter_list} { #{@body} }"
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

    def to_s
      @name + (if @type then ":: #{@type}" else "" end)
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
      super()
      @expression = expression
      @arguments = arguments
    end

    def resolveBindings(env)
      @expression = @expression.resolveBindings env
      @arguments = @arguments.resolveBindings env
      self
    end

    def compile(context, builder)
      @expression.compile context, builder
      @arguments.compile context, builder
      bootstrap = builder.h_invokestatic RT, 'bootstrapCall',
        CallSite, Lookup, Java::java.lang.String, MethodType
      # +2: return value, function argument
      type = [LilaObject] * (2 + @arguments.length)
      builder.invokedynamic 'call', type, bootstrap
    end

    def close(context)
      @expression.close context
      @arguments.close context
    end

    def toString
      "#{@expression}#{@arguments}"
    end
  end

  class Arguments
    def initialize(expressions)
      @expressions = expressions
    end

    def length
      @expressions.length
    end

    def resolveBindings(env)
      @expressions = @expressions.map {|expression|
        expression.resolveBindings env
      }
      self
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

    def to_s
      "(#{@expressions.join(', ')})"
    end
  end

  class Conditional < Expression
    def initialize(test, consequent, alternate)
      super()
      @test = test
      @consequent = consequent
      @alternate = alternate
    end

    def resolveBindings(env)
      @test = @test.resolveBindings env
      @consequent = @consequent.resolveBindings env
      @alternate = @alternate.resolveBindings env
      self
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

     def toString
       "if (#{@test}) { #{@consequent} } else { #{@alternate} }"
     end
  end

  class Sequence < Expression
    def initialize(expressions)
      super()
      @expressions = expressions
    end

    def resolveBindings(env)
      @expressions = @expressions.map { |expression|
        expression.resolveBindings env
      }
      self
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

    def toString
      if @expressions.length == 1
        @expressions.first.to_s
      else
        "(#{@expressions.join(', ')})"
      end
    end
  end
end
