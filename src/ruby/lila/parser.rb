require 'parslet'
require 'lila/transform'
require 'pp'

module Lila
  class Parser < Parslet::Parser
    def self.parse(source)
      parser = Parser.new
      transform = Transform.new
      syntax = parser.parse(source)
      transform.apply(syntax)
    end

    rule(:root) {
      (tWS? >> statement >> tSEMICOLON).repeat.as(:statements)
    }

    rule(:statement) {
      (definition | expression)
    }

    rule(:definition) {
      (method_definition | variable_definition)
    }

    rule(:method_definition) {
      (tDEFINE >> tMETHOD >>
        identifier >> parameters.as(:parameters) >> body)
    }

    rule(:variable_definition) {
      (tDEFINE >> identifier >> tEQUALS >> expression.as(:value))
    }

    rule(:identifier) {
      tNAME.as(:identifier) >> tWS?
    }

    rule(:parameter) {
      tNAME.as(:parameter) >> tWS?
    }

    rule(:expression) {
      or_expression >> tWS?
    }

    rule(:or_expression) {
      (and_expression.as(:left) >>
        tOR >>
        or_expression.as(:right)).as(:or) | and_expression
    }

    rule(:and_expression) {
      (postfix_expression.as(:left) >>
        tAND >>
        and_expression.as(:right)).as(:and) | postfix_expression
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
       method |
       value |
       identifier) >> tWS?
    }

    rule(:let_expression) {
      tLET >> identifier >>
      tEQUALS >> expression.as(:value) >>
      body
    }

    rule(:if_expression) {
      (tIF >> expression.as(:test) >>
        body.as(:consequent) >>
        (tELSE >> body.as(:alternate)).maybe)
    }

    rule(:bracketed_expression) {
      tOPEN_PAREN >>
      (expression.repeat(1,1) >>
        (tCOMMA >> expression).repeat).as(:body) >>
      tCLOSE_PAREN
    }

    rule(:method) {
      (tMETHOD >> parameters.as(:parameters) >> body)
    }

    rule(:parameters) {
      tOPEN_PAREN >>
      (parameter.maybe >>
        (tCOMMA >> parameter).repeat).as(:parameters) >>
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

    # tokens

    rule(:tWS) {
      (tCOMMENT | tMULTILINE_COMMENT | tWS_NO_COMMENT).repeat
    }

    rule(:tWS_NO_COMMENT) {
      match(/[ \t\f]/) | tEOL
    }

    rule(:tCOMMENT) {
      str('//') >> (tEOL.absent? >> any).repeat >> tEOL
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
      match(/[a-zA-Z_\-+*\/?<>=]/)
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
            :equals => '='

    # keywords

    def self.keywords(*names)
      names.each do |name|
        rule("t#{name.to_s.upcase}") { token(name.to_s) }
      end
    end

    keywords :method, :define, :if, :else, :let
  end
end
