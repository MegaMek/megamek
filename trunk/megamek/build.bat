@rem uses an unholy combination of the Sun & Microsoft Java SDKs

del /S /Q classes\megamek
javac -target 1.1 -d classes -classpath classes -sourcepath . -O megamek/*.java
jexegen /out:MegaMek.exe /base:classes /main:megamek.MegaMek /r  *.class
jar cmf manifest.txt MegaMek.jar -C classes .