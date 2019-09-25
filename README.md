# bril2jb
A tool to translate bril code to Java Bytecode
## prerequisite
* openjdk 10.0.2
* the libraries listed in `lib`
## usage
```make``` to compile the tool.

Use script ```bril2jb``` to generate `*.class` file.
```console
~/G/bril2jb ❯❯❯ ./bril2jb
Missing required parameters: <inputFile>, <outputPath>
Usage: bril2jb [-hV] <inputFile> <outputPath>
A tool to translate bril code to Java Bytecode
      <inputFile>    The source code file.
      <outputPath>   The output class path.
  -h, --help         Show this help message and exit.
  -V, --version      Print version information and exit.
```
 
Use `java` to run the compiled class.

For example,
```console
~/G/bril2jb ❯❯❯ ./bril2jb test/gcd.json ./
~/G/bril2jb ❯❯❯ java gcd
```
