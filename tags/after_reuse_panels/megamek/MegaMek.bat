@echo off
echo Starting MegaMek...
echo.

rem Uncomment the next two lines to profile memory use.
rem java -Xrunprof:depth=12 -jar MegaMek.jar
rem goto done

rem Uncomment the next two lines to run the packet tool.
rem java -classpath MegaMek.jar megamek.test.PacketTool
rem goto done

java
if not errorlevel 2 goto sunjvm
echo.
echo Failed to find Sun JVM.  Trying Microsoft JVM instead...
jview
if not errorlevel 2 goto msjvm
goto failure

:msjvm
start wjview /vst /cp collections.jar;TinyXML.jar;MegaMek.jar megamek/MegaMek
goto done

:sunjvm
start javaw -jar MegaMek.jar
goto done

:failure
echo.
echo.
echo -----------------------------------------------------------------
echo Failed to find a Java Virtual Machine.  Please press any key and
echo read the sections titled:
echo "RUNNING MEGAMEK" and "INSTALLING OR UPDATING YOUR JAVA RUNTIME".
pause
start notepad readme.txt

:done
cls
@echo on