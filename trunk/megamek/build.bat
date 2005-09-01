@if not exist classes mkdir classes
rem The last phrase of the javac command below is a kludge to get around an
rem odd problem where the AWT subdirectory of the source tree was not being
rem compiled at all.  I don't know what is actually going on.
javac -target 1.4 -source 1.4 -d classes -O -classpath src;lib/collections.jar;lib/TinyXML.jar;lib/Ostermiller.jar;lib/PngEncoder.jar;lib/TabPanel.jar src/megamek/*.java src/megamek/client/ui/AWT/*.java
jar cmf lib/manifest.txt MegaMek.jar -C classes .
jar uf MegaMek.jar -C l10n .
jar uf MegaMek.jar -C src megamek\MegaMek.properties
