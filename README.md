Lila
====

Lila is a dynamic language running on the JVM with efficient multiple and 
predicate method dispatch. It was developed as part of the [thesis](thesis.pdf)
"Efficient Dynamic Method Dispatch on the Java Virtual Machine".

Requirements
------------

* jruby. Version 1.7.3 was tested and used during development.
  Version 1.5.6 is known to be incompatible with the dependencies.

Installation
------------

* Compile the runtime: ``ant``
* Install the dependencies: ``jruby -S gem install bitescript parslet``

Usage
-----

* Run a program: ``lila.sh -f <filename>``
* Compile a program into a class file: ``lila.sh -c -f <filename>``
  Run the compiled program by including the ``bin`` directory in 
  the classpath. 
  
The environment variable ``JRUBY`` is used to run jruby. 
The default value is ``jruby``.

The directory ``tests`` contains various examples.

