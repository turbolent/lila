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

    rule(:required_parameters => simple(:parameter)) {
      ParameterList.new(if parameter.instance_of? String then []
                        else [parameter] end)
    }

    rule(:required_parameters => simple(:parameter),
         :rest_parameter => simple(:rest_parameter)) {
      parameters = if parameter.instance_of? String then []
                   else [parameter] end
      ParameterList.new(parameters + [rest_parameter], true)
    }

    rule(:required_parameters => sequence(:parameters)) {
      ParameterList.new parameters
    }

    rule(:required_parameters => sequence(:parameters),
         :rest_parameter => simple(:rest_parameter)) {
      ParameterList.new parameters + [rest_parameter], true
    }

    rule(:parameter => simple(:parameter)) {
      Parameter.new(parameter.to_s)
    }

    rule(:superclasses => simple(:expression)) {
      [expression]
    }

    rule(:superclasses => sequence(:expressions)) {
      expressions
    }

    rule(:parameter => simple(:parameter),
         :type => simple(:type)) {
      Parameter.new(parameter.to_s, type)
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
          :parameter_list => simple(:parameter_list),
          :body => simple(:body)}) {
      VariableDefinition.new(name.to_s,
        Function.new(parameter_list, body))
    }

    rule(:method_definition =>
         {:identifier => simple(:name),
          :parameter_list => simple(:parameter_list),
          :body => simple(:body)}) {
      MethodDefinition.new(name.to_s, parameter_list, body)
    }

    rule(:class_definition =>
         {:identifier => simple(:name),
          :superclasses => sequence(:superclasses)}) {
      ClassDefinition.new(name.to_s, superclasses)
    }

    rule(:parameter_list => simple(:parameter_list),
         :body => simple(:body)) {
      Function.new(parameter_list, body)
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

    rule(:let_expr => {:identifier => simple(:identifier),
                       :value => simple(:value),
                       :body => simple(:body)}) {
      Macros.bind(identifier.name.to_s, value, body)
    }

    rule(:or_expr => {:left => simple(:left),
                      :right => simple(:right)}) {
      name = Context.new_name
      value = Identifier.new(name)
      Macros.bind name, left,
                  Sequence.new([Conditional.new(value, value, right)])
    }

    rule(:and_expr => {:left => simple(:left),
                       :right => simple(:right)}) {
      Conditional.new left,
                      Sequence.new([right]),
                      Sequence.new([BooleanValue.new(false)])
    }
  end

  module Macros
    def Macros.bind(name, value, body)
      Call.new(Function.new(ParameterList.new([Parameter.new(name)]), body),
               Arguments.new([value]))
    end
  end
end
