+++
title = "bril2jb: A tool that translates Bril to Java bytecode"
extra.author = "Siqiu Yao"
extra.bio = """
  [Siqiu Yao](http://www.cs.cornell.edu/~yaosiqiu/) is a 3rd year Ph.D. student interested in security about systems and programming languages.
"""
+++

### Goal
The goal of this project is to provide a tool to translate Bril code to Java bytecode.

### Design
At first glance, translating Bril to [Java bytecode](https://en.wikipedia.org/wiki/Java_bytecode) seems a 
pretty straightforward job, since Bril (currently) only contains simple instructions and supports no function invocation.
But it turns out that making something directly runnable by *Java Virtual Machine* (JVM, the runtime environment of Java bytecode)
is not trivial, because we need to construct our program as a Java *class*, 
which is the only code format JVM accepts. 
Also, every class as a standalone application need a `main` function as an entry point 
(Since currently, Bril code contains also only one `main` function, it's a perfect match!).

To demonstrate this, suppose we want to translate the following Bril program:
`"
main {
    a:int = const 1;
    b:int = const 2;
    c:int = add a b;
}
```
The counterpart Java program of it would be:
```JAVA
public class Wrapper {
  public static void main(java.lang.String[] args) {
      long a = 1;
      long b = 2;
      long c = a + b;
  }
}
```
Please notice that: (1) the class name can be an arbitrary valid class name; 
(2) bril2jb is not generating Java code, the Bril code is directly translated to Java bytecode.    

Before talking about how to translate, 
I want to introduce how JVM runs the code and what a compiled Java class looks like.

#### Structure of JVM

![JVM Architecture by Michelle Ridomi - Own work, CC BY-SA 4.0, https://commons.wikimedia.org/w/index.php?curid=35963523](https://upload.wikimedia.org/wikipedia/commons/d/dd/JvmSpec7.png)

The picture above shows an overview of JVM architecture.

To run a Java class, JVM will first load the compiled class file into memory, 
and then start to execute the code (by default, the function `main`). 
There is one significant difference between JVM's execution model and other languages like C:
the stack in JVM is a stack of _stack frames_, each stack frame contains _local variable arrays_, _frame data_, and _oprand stack_,
while operands for arithmetic or logical operations are most often placed into registers and operated on there, 
they happen in operand stack in JVM. 
Thus, in terms of computation, JVM is more like a stack-based machine.   

#### Structure of a compiled Java class

| Item Name | A Brief Introduction  |
|---|---|
|magic| The magic number identifying the class file format; it has the value 0xCAFEBABE. |
|minor_version, major_version|The minor and major version numbers of this class file. |
|constant_pool_count, constant_pool[]| The constant_pool is a table of structures representing various string constants, class and interface names, field names, and other constants that are referred to within the class and its substructures. constant_pool_count represents the size of this table.|
|access_flags|A mask of flags used to denote access permissions to and properties of this class. |
|this_class|A valid index into the constant_pool table referring to a CONSTANT_Class_info structure representing this class. |
|super_class|Zero or a valid index into the constant_pool table referring to a CONSTANT_Class_info representing the superclass of this class.|
|interfaces_count, interfaces[]|Each value in interfaces array is a valid index into the constant_pool table referring to a CONSTANT_Class_info structure representing a superinterface of this class.|
|fields_count, fields[]|Each value in the fields table is a field_info structure describing a field in this class.|
|methods_count, methods[]|Each value in the methods table is a method_info structure describing a method in this class.|
|attributes_count, attributes[]|Each value in the attributes table is an attribute_info structure describing an attribute in this class.|

The table above summarizes the overall structure of a compiled class. 
More details can be found in Chapter 4 of [JVM document](https://docs.oracle.com/javase/specs/jvms/se13/jvms13.pdf).
 
You can inspect the structure of a compiled Java class using the command `javap` provided by Java JDK.
For example, we can inspect the Wrapper class introduced earlier.

```java
~/G/bril2jb ❯❯❯ javap -v Wrapper
Classfile /home/animula/GitRep/bril2jb/Wrapper.class
  Last modified Oct 18, 2019; size 291 bytes
  MD5 checksum e4dc4c91a1fcd5333b7617f95cd75232
  Compiled from "Wrapper.java"
public class Wrapper
  minor version: 0
  major version: 54
  flags: (0x0021) ACC_PUBLIC, ACC_SUPER
  this_class: #4                          // Wrapper
  super_class: #5                         // java/lang/Object
  interfaces: 0, fields: 0, methods: 2, attributes: 1
Constant pool:
   #1 = Methodref          #5.#14         // java/lang/Object."<init>":()V
   #2 = Long               2l
   #4 = Class              #15            // Wrapper
   #5 = Class              #16            // java/lang/Object
   #6 = Utf8               <init>
   #7 = Utf8               ()V
   #8 = Utf8               Code
   #9 = Utf8               LineNumberTable
  #10 = Utf8               main
  #11 = Utf8               ([Ljava/lang/String;)V
  #12 = Utf8               SourceFile
  #13 = Utf8               Wrapper.java
  #14 = NameAndType        #6:#7          // "<init>":()V
  #15 = Utf8               Wrapper
  #16 = Utf8               java/lang/Object
{
  public Wrapper();
    descriptor: ()V
    flags: (0x0001) ACC_PUBLIC
    Code:
      stack=1, locals=1, args_size=1
         0: aload_0
         1: invokespecial #1                  // Method java/lang/Object."<init>":()V
         4: return
      LineNumberTable:
        line 1: 0

  public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: (0x0009) ACC_PUBLIC, ACC_STATIC
    Code:
      stack=4, locals=7, args_size=1
         0: lconst_1
         1: lstore_1
         2: ldc2_w        #2                  // long 2l
         5: lstore_3
         6: lload_1
         7: lload_3
         8: ladd
         9: lstore        5
        11: return
      LineNumberTable:
        line 3: 0
        line 4: 2
        line 5: 6
        line 6: 11
}
SourceFile: "Wrapper.java"
```
 
The Wrapper class contains no field members and two methods. One is a default init function,
the other is `main` where we put the translated code.



#### The translation process
I chose [ASM](https://asm.ow2.io), a Java bytecode manipulation and analysis framework to help with the translation.
ASM provides APIs for generating (and transforming, which is not used in this project) 
compiled JAVA classes. 
It provides two APIs: the core API provides an event-based representation of class, while the tree API provides an object-based representation.
And we only leveraged the core API. 

The core API is described in [the ASM document](https://asm.ow2.io/asm4-guide.pdf) as:
> With the event based model a class is represented with a sequence of events, 
> each event representing an element of the class, 
> such as its header, a field, a method declaration, an instruction, etc. 
> The event based API defines the set of possible events and the order in which they must occur, 
> and provides a class parser that generates one event per element that is parsed, 
> as well as a class writer that generates compiled classes from sequences of such events.

The translation process is pretty regular. 
Most instructions share the pattern: 
"load arguments to stack" -> "evaluate" -> "store the result".
One interesting point is that Java bytecode is strictly-typed,
so I made this design decision that all variables in Bril should also be strictly-typed, 
and for all possible execution traces each variable's type should be the same. 
What's more, some instructions in Bril are polymorphic (`print` and `id`).
Therefore, we preprocess the instructions first to gather all type information and labels.

Another interesting point and also the hardest part of this project is translating `print`.
There are two steps involved: 
(1) Convert all arguments of print to `String` and concatenate them (Separated with one space);
(2) Call `java.lang.System.println` with the previous `String` as its argument.
 
The step (2) is a straightforward function call, so we'll focus on step (1).

The step (1) is interesting, because 
the way Java compiler deals with static `String` concatenation changed significantly.
In Java 8 or earlier, 
the compiler leverages class `StringBuilder`,
it converts arguments to `String`s and appends them to `StringBuilder` one by one,
while since Java 9, 
the compiler simply pushes all arguments into the stack
and call a _dynamic method_ `java.lang.invoke.StringConcatFactory.makeConcatWithConstants​`.

The latter approach was claimed to 
"enable future optimizations of String concatenation without requiring further changes to the bytecode emitted by javac."
So we go with the later one. 
The tricky part is that,
a dynamic method is generated at runtime, 
to generate it, 
a static _bootstrap method_ is required.
The work this method does here is that, 
given a descriptor (a string that describes types of arguments) and a formatter
(a string that describes the format of output string),
generate a dynamic function accordingly ([further reading](https://www.guardsquare.com/en/blog/string-concatenation-java-9-untangling-invokedynamic)).
Luckily, ASM handles most of the generation of this bootstrap method for us, 
what we need to do is for each usage of `print`, 
generate the corresponding descriptor and formatter.


### Implementation
[This tool](https://github.com/Neroysq/bril2jb) is implemented in Java. It translates Bril code (in JSON format) to 
a same-name Java class file, which can run on the JVM. 

To use this tool, specify the Bril source file (in JSON format) and output path, 
it will output a Java class file, and then you can run this class using `java`.

For example,
Suppose we have a Bril code implementing [Euclidean algorithm for computing the greatest common divisor](https://en.wikipedia.org/wiki/Euclidean_algorithm):
`"
main {
    a: int = const 2341234;
    b: int = const 653266234;
    zero: int = const 0;
loop:
    cond: bool = eq b zero;
    br cond final here;
here:
    c: int = div a b;
    c: int = mul c b;
    c: int = sub a c;
    a: int = id b;
    b: int = id c;
    print a b;
    jmp loop;
final:
    print a b;
}
```
Then we run `bril2json` and get its JSON form, and use `bril2jb` to generate `gcd.class`.
```console
~/G/bril2jb ❯❯❯ ./bril2jb test/gcd.json ./
~/G/bril2jb ❯❯❯ java gcd
653266234 2341234
2341234 61948
61948 49158
49158 12790
12790 10788
10788 2002
2002 778
778 446
446 332
332 114
114 104
104 10
10 4
4 2
2 0
2 0
```
You can also use `javap -v gcd` to see more details.

In our implementation, our tool expects the input program will be strictly-typed and 
it typechecks as a Bril program. 
Because we think error-handling is not the topic of this project.
If the program doesn't typecheck 
(For example, use a variable that's never defined as an argument.), 
the tool will crash.

There are some tricky details:

1. Since `int` in Bril is 64-bit, all `int` variables are `long` variables in JVM; 
there is no boolean type in JVM, so all `bool` variables are `int` (32-bit integer) type in JVM.

2. ASM handles the size of local variable arrays (that is, the size of space all local variables in one function need),
 but the indices of local variables need to be manually maintained (`long` type takes two index units while `int` takes 1).
 
3. ASM handles the construction of the constant pool automatically, which is convenient. 
 

### Evaluation
We manually tested our tool using hand-written test cases (including the ones in Bril repo and seven more in `test` folder in our repo) to ensure the correctness,
they cover all the instructions in Bril.
All of our tests' output results agree with the reference interpreter.
We also manually looked into some of the classes (`gcd` and `fibonacci`), and they all look reasonable.

### Conclusion
In conclusion, we successfully built a translator from Bril to Java bytecode. 
I look forward to further maintaining this tool to support potential new features 
such as function calls and memory allocation.
