@if not exist classes mkdir classes
javac -target 1.1 -d classes -O -classpath .;collections.jar;TinyXML.jar megamek/*.java
jar cmf manifest.txt MegaMek.jar -C classes .