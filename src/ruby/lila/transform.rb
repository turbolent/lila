require 'lila/nodes'

java_import 'lila.runtime.dispatch.predicate.TestPredicate'
java_import 'lila.runtime.dispatch.predicate.TruePredicate'
java_import 'lila.runtime.dispatch.predicate.NotPredicate'
java_import 'lila.runtime.dispatch.predicate.AndPredicate'
java_import 'lila.runtime.dispatch.predicate.OrPredicate'
java_import 'lila.runtime.dispatch.predicate.TypePredicate'
java_import 'lila.runtime.dispatch.predicate.BindingPredicate'


module Lila
  class Transform < Parslet::Transform

    rule(:integer => simple(:value)) {
      IntegerValue.new value.to_i
    }

    rule(:string => simple(:value)) {
      StringValue.new value.to_s
    }

    rule(:boolean => simple(:value)) {
      BooleanValue.new value == 'true'
    }

    rule(:identifier => simple(:name)) {
      Identifier.new name.to_s
    }

    rule(:required_parameters => simple(:parameter)) {
      ParameterList.new(if parameter.instance_of? String then []
                        else [parameter] end)
    }

    rule(:required_parameters => simple(:parameter),
         :rest_parameter => simple(:rest_parameter)) {
      parameters = if parameter.instance_of? String then []
                   else [parameter] end
      ParameterList.new parameters + [rest_parameter], true
    }

    rule(:required_parameters => sequence(:parameters)) {
      ParameterList.new parameters
    }

    rule(:required_parameters => sequence(:parameters),
         :rest_parameter => simple(:rest_parameter)) {
      ParameterList.new parameters + [rest_parameter], true
    }

    rule(:parameter => simple(:parameter)) {
      Parameter.new parameter.to_s
    }

    rule(:superclasses => simple(:expression)) {
      [expression]
    }

    rule(:superclasses => sequence(:expressions)) {
      expressions
    }

    rule(:parameter => simple(:parameter),
         :type => simple(:type)) {
      Parameter.new parameter.to_s, type
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
        Call.new result, arguments
      }
    }

    rule(:statements => sequence(:statements)) {
      Program.new statements
    }

    rule(:function_definition =>
         {:identifier => simple(:name),
          :parameter_list => simple(:parameter_list),
          :body => simple(:body)}) {
      function = Function.new name.to_s, parameter_list, body
      VariableDefinition.new name.to_s, function
    }

    rule(:multi_method_definition =>
         {:identifier => simple(:name),
          :parameter_list => simple(:parameter_list),
          :body => simple(:body)}) {
      MultiMethodDefinition.new name.to_s, parameter_list, body
    }

    rule(:predicate_method_definition =>
         {:identifier => simple(:name),
          :parameter_list => simple(:parameter_list),
          :predicate => simple(:predicate),
          :body => simple(:body)}) {
      PredicateMethodDefinition.new name.to_s, parameter_list, predicate, body
    }

    rule(:predicate_method_definition =>
         {:identifier => simple(:name),
          :parameter_list => simple(:parameter_list),
          :default => simple(:default),
          :body => simple(:body)}) {
      PredicateMethodDefinition.new name.to_s, parameter_list, TruePredicate.INSTANCE, body
    }


    rule(:class_definition =>
         {:identifier => simple(:name),
          :superclasses => sequence(:superclasses)}) {
      ClassDefinition.new name.to_s, superclasses
    }

    rule(:class_definition =>
         {:identifier => simple(:name),
          :superclasses => sequence(:superclasses),
          :properties => sequence(:properties)}) {
      ClassDefinition.new name.to_s, superclasses, properties
    }

    rule(:property => simple(:name)) {
      name.to_s
    }

    rule(:parameter_list => simple(:parameter_list),
         :body => simple(:body)) {
      Function.new nil, parameter_list, body
    }

    rule(:test => simple(:test),
         :consequent => simple(:consequent),
         :alternate => simple(:alternate)) {
      Conditional.new test, consequent, alternate
    }

    rule(:test => simple(:test),
         :consequent => simple(:consequent)) {
      Conditional.new test, consequent,
                      BooleanValue.new(false)
    }

    rule(:count => simple(:count),
         :body => simple(:body)) {
      Repetition.new count, body
    }

    rule(:test => simple(:test),
         :body => simple(:body)) {
      Loop.new test, body
    }

    rule(:body => sequence(:expressions)) {
      Sequence.new expressions
    }

    rule(:identifier => simple(:name),
         :value => simple(:value)) {
      VariableDefinition.new name.to_s, value
    }

    # macros

    rule(:let_expr => {:identifier => simple(:identifier),
                       :value => simple(:value),
                       :body => simple(:body)}) {
      Macros.bind identifier.name.to_s, value, body
    }

    rule(:or_expr => {:left => simple(:left),
                      :right => simple(:right)}) {
      name = Context.new_name
      value = Identifier.new name
      Macros.bind name, left,
                  Sequence.new([Conditional.new(value, value, right)])
    }

    rule(:and_expr => {:left => simple(:left),
                       :right => simple(:right)}) {
      Conditional.new left,
                      Sequence.new([right]),
                      Sequence.new([BooleanValue.new(false)])
    }

    rule(:test_pred => simple(:expression)) {
      TestPredicate.new expression
    }

    rule(:not_pred => simple(:predicate)) {
      NotPredicate.new predicate
    }

    rule(:and_pred => {:left => simple(:left),
                       :right => simple(:right)}) {
      AndPredicate.new left, right
    }

    rule(:or_pred => {:left => simple(:left),
                      :right => simple(:right)}) {
      OrPredicate.new left, right
    }

    rule(:type_pred => {:expression => simple(:expression),
                        :type => simple(:type)}) {
      predicate = TypePredicate.new expression, nil
      # temporarily store expresssion,
      # evaluated to actual class by interpreter
      predicate.typeExpression = type
      predicate
    }

    rule(:bind_pred => {:name => simple(:name),
                        :expression => simple(:expression)}) {
      BindingPredicate.new(name.to_s, expression)
    }
  end

  module Macros
    def Macros.bind(name, value, body)
      parameters = ParameterList.new [Parameter.new(name)]
      function = Function.new nil, parameters, body
      arguments = Arguments.new [value]
      Call.new function, arguments
    end
  end
end
