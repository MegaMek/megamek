rem uses an unholy combination of the Sun & Microsoft Java SDKs

javac -O -target 1.1 -d classes megamek/*.java
jexegen /out:MegaMek.exe /base:classes /main:megamek.MegaMek /r  *.class
jar cmf manifest.txt MegaMek.jar -C classes .