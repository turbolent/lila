require 'rubygems'
require 'parslet'
require 'lila/transform'
require 'pp'

module Lila
  class Parser < Parslet::Parser
    def self.parse(source)
      parser = Parser.new
      transform = Transform.new
      cst = parser.parse(source)
      ast = transform.apply(cst)
      puts ast
      ast
    end

    rule(:root) {
      (tWS? >> statement >>
        tSEMICOLON).repeat.as(:statements)
    }

    rule(:statement) {
      (definition | expression)
    }

    rule(:definition) {
      (function_definition |
        multi_method_definition |
        predicate_method_definition |
        class_definition |
        variable_definition)
    }

    rule(:function_definition) {
      (kDEFN >> identifier >>
        parameter_list.as(:parameter_list) >>
        body.as(:body)).as(:function_definition)
    }

    rule(:multi_method_definition) {
      (kDEFMM >> identifier >>
        parameter_list.as(:parameter_list) >>
        body.as(:body)).as(:multi_method_definition)
    }

    rule(:predicate_method_definition) {
      (kDEFPM >> identifier >>
        parameter_list.as(:parameter_list) >>
        ((kWHEN >> predicate.as(:predicate)) |
          (kDEFAULT.as(:default))) >>
        body.as(:body)).as(:predicate_method_definition)
    }

    rule(:class_definition) {
      (kDEFCLASS >> identifier >>
        superclasses.as(:superclasses) >>
        properties.maybe).as(:class_definition)
    }

    rule(:properties) {
      tOPEN_BRACE >>
      (property.repeat(1,1) >>
        (tSEMICOLON >> property).repeat >>
        tSEMICOLON.maybe).as(:properties) >>
      tCLOSE_BRACE
    }

    rule(:property) {
      tNAME.as(:property) >> tWS?
    }

    rule(:superclasses) {
      tOPEN_PAREN >>
      (expression.maybe >>
        (tCOMMA >> expression).repeat).as(:superclasses) >>
      tCLOSE_PAREN
    }

    rule(:variable_definition) {
      kDEF >> identifier >>
        tEQUALS >> expression.as(:value)
    }

    rule(:identifier) {
      tNAME.as(:identifier) >> tWS?
    }

    rule(:parameter) {
       tNAME.as(:parameter) >> tWS? >>
       (tDOUBLE_COLON >> expression.as(:type) >> tWS?).maybe
    }

    # Predicates

    rule(:predicate) {
      or_predicate >> tWS?
    }

    rule(:or_predicate) {
      (and_predicate.as(:left) >> tOR >>
        or_predicate.as(:right)).as(:or_pred) | and_predicate
    }

    rule(:and_predicate) {
      (atom_predicate.as(:left) >> tAND >>
        and_predicate.as(:right)).as(:and_pred) | atom_predicate
    }

    rule(:atom_predicate) {
      (bracketed_predicate |
       test_predicate |
       not_predicate |
       binding_predicate |
       type_predicate) >> tWS?
    }

    rule(:test_predicate) {
      (kTEST >> expression).as(:test_pred)
    }

    rule(:not_predicate) {
      (kNOT >> predicate).as(:not_pred)
    }

    rule(:binding_predicate) {
      (tNAME.as(:name) >> tWS? >>
       tBIND >> expression.as(:expression)).as(:bind_pred)
    }

    rule(:type_predicate) {
      (expression.as(:expression) >>
        tDOUBLE_COLON >> expression.as(:type)).as(:type_pred)
    }

    rule(:bracketed_predicate) {
      (tOPEN_PAREN >> predicate >> tCLOSE_PAREN)
    }


    # Expressions

    rule(:expression) {
      or_expression >> tWS?
    }

    rule(:or_expression) {
      (and_expression.as(:left) >> tOR >>
        or_expression.as(:right)).as(:or_expr) | and_expression
    }

    rule(:and_expression) {
      (postfix_expression.as(:left) >> tAND >>
        and_expression.as(:right)).as(:and_expr) | postfix_expression
    }

    rule(:postfix_expression) {
      (primary_expression.as(:expression) >>
        arguments.repeat(1).as(:calls)) | primary_expression
    }

    rule(:arguments) {
      tOPEN_PAREN >>
      (expression.maybe >>
        (tCOMMA >> expression).repeat).as(:arguments) >>
      tCLOSE_PAREN
    }

    rule(:primary_expression) {
      (bracketed_expression |
       let_expression |
       if_expression |
       while_expression |
       dotimes_expression |
       function |
       value |
       identifier) >> tWS?
    }

    rule(:let_expression) {
      (kLET >> identifier.as(:identifier) >>
       tEQUALS >> expression.as(:value) >>
       body.as(:body)).as(:let_expr)
    }

    rule(:if_expression) {
      (kIF >> expression.as(:test) >>
        body.as(:consequent) >>
        (kELSE >> body.as(:alternate)).maybe)
    }

    rule(:while_expression) {
      kWHILE >> expression.as(:test) >>
        body.as(:body)
    }

    rule(:dotimes_expression) {
      kDOTIMES >> tOPEN_PAREN >>
        expression.as(:count) >> tCLOSE_PAREN >>
        body.as(:body)
    }

    rule(:bracketed_expression) {
      tOPEN_PAREN >>
      (expression.repeat(1,1) >>
        (tCOMMA >> expression).repeat).as(:body) >>
      tCLOSE_PAREN
    }

    rule(:function) {
      (kFN >> parameter_list.as(:parameter_list) >>
        body.as(:body))
    }

    rule(:parameter_list) {
      tOPEN_PAREN >>
      (parameter.maybe >>
        (tCOMMA >> parameter).repeat).as(:required_parameters) >>
      (tELLIPSIS >> parameter).as(:rest_parameter).maybe >>
      tCLOSE_PAREN
    }

    rule(:body) {
      tOPEN_BRACE >>
      (expression.repeat(1,1) >>
        (tSEMICOLON >> expression).repeat >>
        tSEMICOLON.maybe).as(:body) >>
      tCLOSE_BRACE
    }

    rule(:value) {
      integer | string | boolean
    }

    rule(:integer) {
      tDIGIT.repeat(1).as(:integer)
    }

    rule(:string) {
      str('"') >>
      (str('\\') >>
        any | str('"').absent? >>
        any).repeat.as(:string) >>
      str('"')
    }

    rule(:boolean) {
      (str("true") | str("false")).as(:boolean)
    }

    rule(:eof) { any.absent? }

    # tokens

    rule(:tWS) {
      (tCOMMENT | tMULTILINE_COMMENT | tWS_NO_COMMENT).repeat
    }

    rule(:tWS_NO_COMMENT) {
      match(/[ \t\f]/) | tEOL
    }

    rule(:tCOMMENT) {
      str('//') >> (tEOL.absent? >> any).repeat >> (tEOL | eof)
    }

    rule(:tMULTILINE_COMMENT) {
      (str('/*') >> (str('*/').absent? >> any).repeat >> str('*/'))
    }

    rule(:tEOL) {
      match('[\n\r]').repeat(1)
    }

    rule(:tWS?) {
      tWS.maybe
    }

    rule(:tESCAPED) {
      str("\\") >> match(/[tbnrf\\"']/)
    }

    rule(:tNAME) {
      tCHAR >> (tCHAR | tDIGIT).repeat
    }

    rule(:tDIGIT) {
      match('[0-9]')
    }

    rule(:tCHAR) {
      match(/[a-zA-Z_\-+*\/?!<>=]/)
    }

    def token (string)
      str(string) >> tWS?
    end

    # symbols

    def self.symbols(symbols)
      symbols.each do |name,symbol|
        rule("t#{name.to_s.upcase}") { token(symbol) }
      end
    end

    symbols :open_paren => '(',
            :close_paren => ')',
            :comma => ',',
            :semicolon => ';',
            :or => '||',
            :and => '&&',
            :open_brace => '{',
            :close_brace => '}',
            :equals => '=',
            :double_colon => '::',
            :ellipsis => '...',
            :bind => ':='

    # keywords

    def self.keywords(*names)
      names.each do |name|
        rule("k#{name.to_s.upcase}") { token(name.to_s) }
      end
    end

    keywords :fn, :def, :defclass, :defn, :defmm, :defpm,
      :if, :else, :let, :while, :when, :not, :test, :default, :dotimes
  end
end
