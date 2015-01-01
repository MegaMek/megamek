@if not exist classes mkdir classes
javac -target 1.6 -source 1.6 -d classes -O -classpath src;lib/TinyXML.jar;lib/Ostermiller.jar;lib/PngEncoder.jar;lib/TabPanel.jar;lib/vecmath.jar;lib/j3dutils.jar;lib/j3dcore.jar src/megamek/*.java src/megamek/client/ui/AWT/*.java src/megamek/client/ui/swing/*.java src/megamek/client/ui/AWT/boardview3d/*.java
jar cmf lib/manifest.txt MegaMek.jar -C classes .
jar uf MegaMek.jar -C l10n .
jar uf MegaMek.jar -C src megamek\MegaMek.properties
