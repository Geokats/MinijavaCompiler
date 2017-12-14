all: compile

compile:
	java -jar ./jar/jtb132di.jar -te minijava.jj
	java -jar ./jar/javacc5.jar minijava-jtb.jj
	javac MiniJavac.java

clean:
	rm -f *.class *~
