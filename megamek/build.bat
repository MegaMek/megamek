@if not exist classes mkdir classes
javac -target 1.5 -source 1.5 -d classes -O -classpath src;lib/TinyXML.jar;lib/Ostermiller.jar;lib/PngEncoder.jar;lib/TabPanel.jar src/megamek/*.java src/megamek/client/ui/AWT/*.java
jar cmf lib/manifest.txt MegaMek.jar -C classes .
jar uf MegaMek.jar -C l10n .
jar uf MegaMek.jar -C src megamek\MegaMek.properties
