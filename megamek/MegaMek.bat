@echo off
if "%JAVA_HOME%"=="" goto MicrosoftJRE
rem Uncomment the next two lines to profile memory use.
rem java -Xrunprof:depth=12 -jar MegaMek.jar
rem goto done
rem Uncomment the next two lines to run the packet tool.
java -classpath MegaMek.jar megamek.test.PacketTool
rem goto done
java -jar MegaMek.jar
goto done

:MicrosoftJRE
start wjview /cp collections.jar;TinyXML.jar;MegaMek.jar megamek/MegaMek

:done
@echo on
