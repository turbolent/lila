require 'lila/nodes'

module Lila
  class Transform < Parslet::Transform

    rule(:integer => simple(:value)) {
      IntegerValue.new(value.to_i)
    }

    rule(:string => simple(:value)) {
      StringValue.new(value.to_s)
    }

    rule(:boolean => simple(:value)) {
      BooleanValue.new(value == 'true')
    }

    rule(:identifier => simple(:name)) {
      Identifier.new(name.to_s)
    }

    rule(:parameters => simple(:parameter)) {
      if parameter.instance_of? String then []
      else [parameter] end
    }

    rule(:parameters => sequence(:parameters)) {
      parameters
    }

    rule(:parameter => simple(:parameter)) {
      Parameter.new(parameter.to_s)
    }

    rule(:arguments => simple(:argument)) {
      Arguments.new(if argument.instance_of? String then []
                    else [argument] end)
    }

    rule(:arguments => sequence(:arguments)) {
      Arguments.new(arguments)
    }

    rule(:expression => simple(:expression),
         :calls => sequence(:calls)) {
      calls.reduce(expression) { |result, arguments|
        Call.new(result, arguments)
      }
    }

    rule(:statements => sequence(:statements)) {
      Program.new(statements)
    }

    rule(:function_definition => 
         {:identifier => simple(:name),
          :parameters => sequence(:parameters),
          :body => sequence(:statements)}) {
      VariableDefinition.new(name.to_s, 
        Function.new(parameters, statements))
    }
    
    rule(:method_definition => 
         {:identifier => simple(:name),
          :parameters => sequence(:parameters),
          :body => sequence(:statements)}) {
      MethodDefinition.new(name.to_s, parameters, statements)
    }

    rule(:parameters => sequence(:parameters),
         :body => sequence(:statements)) {
      Function.new(parameters, statements)
    }

    rule(:test => simple(:test),
         :consequent => simple(:consequent),
         :alternate => simple(:alternate)) {
      Conditional.new(test, consequent, alternate)
    }

    rule(:test => simple(:test),
        :consequent => simple(:consequent)) {
      Conditional.new(test, consequent,
                      BooleanValue.new(false))
    }

    rule(:body => sequence(:expressions)) {
      Sequence.new(expressions)
    }

    rule(:identifier => simple(:name),
         :value => simple(:value)) {
      VariableDefinition.new(name.to_s, value)
    }

    # macros

    rule(:identifier => simple(:name),
         :value => simple(:value),
         :body => sequence(:expressions)) {
      Macros.bind(name.to_s, value, expressions)
    }

    rule(:or => {:left => simple(:left),
                 :right => simple(:right)}) {
      name = Context.new_name
      value = Identifier.new(name)
      Macros.bind(name, left,
                  [Conditional.new(value, value, right)])
    }

    rule(:and => {:left => simple(:left),
                 :right => simple(:right)}) {
      Conditional.new(left,
                      Sequence.new([right]),
                      Sequence.new([BooleanValue.new(false)]))
    }
  end

  module Macros
    def Macros.bind(name, value, expressions)
      Call.new(Function.new([Parameter.new(name)], expressions),
               Arguments.new([value]))
    end
  end
end
