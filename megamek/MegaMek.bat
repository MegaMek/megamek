@echo off
echo Starting MegaMek...
echo If you get a message like, "Cannot find the file 'wjview'", please
echo open the file named "readme.txt" and refer to the sections named
echo "RUNNING MEGAMEK" and "INSTALLING OR UPDATING YOUR JAVA RUNTIME".

if "%JAVA_HOME%"=="" goto MicrosoftJRE
rem Uncomment the next two lines to profile memory use.
rem java -Xrunprof:depth=12 -jar MegaMek.jar
rem goto done
rem Uncomment the next two lines to run the packet tool.
rem java -classpath MegaMek.jar megamek.test.PacketTool
rem goto done
java -jar MegaMek.jar
goto done

:MicrosoftJRE
start wjview /vst /cp collections.jar;TinyXML.jar;MegaMek.jar megamek/MegaMek

:done
@echo on
