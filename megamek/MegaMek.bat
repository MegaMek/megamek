@echo off
if "%JAVA_HOME%"=="" goto MicrosoftJRE
java -jar MegaMek.jar
goto done

:MicrosoftJRE
start wjview /cp collections.jar;TinyXML.jar;MegaMek.jar megamek/MegaMek

:done
@echo on
