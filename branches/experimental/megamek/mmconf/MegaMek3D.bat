@echo off
echo Starting MegaMek...
echo.

rem Uncomment the next two lines to profile memory use.
rem java -Xrunprof:depth=12 -jar MegaMek.jar
rem goto done

rem Uncomment the next two lines to run the packet tool.
rem java -classpath MegaMek.jar megamek.test.PacketTool
rem goto done

:jvm_tests
java > NUL
if errorlevel 2 goto failure

:sun_jvm
for %%A in (%1 %2 %3 %4 %5 %6 %7 %8 %9) do if X%%A==X-dedicated goto sun_dedicated
start javaw -jar -Dmegamek.client.ui.AWT.boardView=megamek.client.ui.AWT.boardview3d.BoardView3D -Xmx512m MegaMek.jar
goto done
:sun_dedicated
echo Starting dedicated server.  Closing this window or pressing [ctrl]-[c] will
echo quit the server.
echo.
java -jar MegaMek.jar %1 %2 %3 %4 %5 %6 %7 %8 %9
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
@echo on