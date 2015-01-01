@if not exist classes mkdir classes
javac -target 1.1 -source 1.2 -d classes -O -classpath .;collections.jar;TinyXML.jar;Ostermiller.jar;PngEncoder.jar;TabPanel.jar megamek/*.java
jar cmf manifest.txt MegaMek.jar -C classes .
