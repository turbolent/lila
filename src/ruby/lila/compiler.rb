require 'rubygems'
require 'bitescript'

require 'lila/parser'

java_import 'lila.runtime.LilaObject'
java_import 'lila.runtime.RT'

java_import 'java.lang.invoke.MethodHandle'
java_import 'java.lang.invoke.MethodHandles'
java_import 'java.lang.invoke.MethodType'

module Lila
  class CompilationResult <
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
      @vars = 0
    end

    def register_internal_function(internal_name, name, type, rest)
      if @parent
        @parent.register_internal_function internal_name, name, type, rest
      else
        @functions[internal_name] = [name, type, rest]
      end
    end

    def find_parameter(name)
      (@function.parameter_list.parameters.find { |parameter|
        parameter.name == name
       } if @function) || @parent.find_parameter(name) if @parent
    end

    def nextVar
      if @function
        @function.nextVar
      else
        var = @vars
        @vars += 1
        var
      end
    end
  end

  class Compiler
    def initialize
      @class_id = 0
    end

    def new_class_builder(opts = {})
      name = opts[:name] 
      unless name
        # generate new class name
        @class_id += 1
        name = "__lila#{@class_id}"
      end
      # common superclass is always LilaObject
      opts[:widen] = Proc.new { |a, b|
        BiteScript::Signature::path(LilaObject)
      }
      BiteScript::ClassBuilder.new nil, name, nil, opts
    end

    # compile an expression into a class
    def compile_expression(expression, context)
      compile expression, context, "run", [LilaObject] do |builder|
        builder.areturn
      end
    end

    def compile(expression, context, name, sig)
      # initialize set of functions and
      # create new class builder
      context.functions = {}
      builder = new_class_builder

      # create entry point into expression
      builder.public_static_method name, [], *sig do |method|
        expression.compile(context, method)
        yield method if block_given?
      end

      # return name and bytecode of class,
      # as well as function names created during compilation
      CompilationResult.new builder.class_name,
        bytecode(builder),
        context.functions
    end

    def bytecode(builder)
      class_writer = builder.instance_variable_get("@class_writer")
      class_writer.to_byte_array
    end
    
    def compile_file(filename)
      source = IO.read(File.expand_path(filename))
     
      class_name = File.basename(filename, ".*")
      class_file_name = File.join(File.dirname(filename), "#{class_name}.class")
      
      context = Context.new
      
      builder = new_class_builder :name => class_name
      sig = [Java::void, Java::java.lang.String[]]
      builder.public_static_method "main", [], *sig do |method|
        method.invokestatic RT, 'initialize', [Java::void]
        method.invokestatic builder, 'initialize', [Java::void]
        
        # TODO: call function initialization function
        
        Parser.parse(source).statements.each do |statement|          
            
          statement.compile context, method
          
        end
        method.returnvoid
      end
      
      puts context.functions
      
      # TODO:
      sig = [Java::void, 
             Java::java.lang.Class, Java::java.lang.String,
             Java::java.lang.String, Java::boolean, 
             Java::java.lang.Class, Java::java.lang.Class[]]
      
      builder.private_static_method "initialize", [], Java::void do |method|
        # TODO: 
        context.functions.each { |internal_name, name_type_rest|
          name, type, rest = name_type_rest
          rtype, *ptypes = *type
         
          method.ldc BiteScript::ASM::Type.getObjectType(class_name)
          method.ldc internal_name  
          method.ldc name || ""
          method.ldc rest || false
          method.ldc_class rtype
          method.ldc ptypes.length       
          method.anewarray Java::java.lang.Class
          ptypes.each_with_index { |ptype, index| 
            method.dup           
            method.ldc index    
            method.ldc_class ptype
            method.aastore 
          }
          
          method.invokestatic RT, 'registerInternalFunction', sig
        } 
        method.returnvoid           
      end
      
      File.open(class_file_name, 'w') do |file|
        file.write bytecode(builder)
      end
    end
  end
end
