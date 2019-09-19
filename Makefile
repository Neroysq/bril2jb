LIBPATH=lib

default: Bril2jb

Bril2jb: 
	javac -cp .:${LIBPATH}/* Bril2jb.java

clean:
	rm -f *.class
