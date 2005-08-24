@if not exist classes mkdir classes
javac -target 1.4 -source 1.4 -d classes -O -classpath src;lib/collections.jar;lib/TinyXML.jar;lib/Ostermiller.jar;lib/PngEncoder.jar;lib/TabPanel.jar src/megamek/*.java
jar cmf lib/manifest.txt MegaMek.jar -C classes .
jar uf MegaMek.jar -C l10n .
