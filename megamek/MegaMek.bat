@echo off
echo Starting MegaMek...
echo.

rem Uncomment the next two lines to profile memory use.
rem java -Xrunprof:depth=12 -jar MegaMek.jar
rem goto done

rem Uncomment the next two lines to run the packet tool.
rem java -classpath MegaMek.jar megamek.test.PacketTool
rem goto done

if exist mmconf\jvm_ms.cfg goto ms_jvm
if exist mmconf\jvm_sun.cfg goto sun_jvm

:jvm_tests
java > NUL
if not errorlevel 2 goto sun_ok

:try_ms
echo.
echo Failed to find Sun JVM.  Trying Microsoft JVM instead...
jview > NUL
if errorlevel 2 goto failure
echo Microsoft JVM found.  Defaulting to Microsoft JVM from now on.
:ms_jvm_prefer
echo This file causes MegaMek to prefer the Microsoft JVM > mmconf\jvm_ms.cfg
:ms_jvm
for %%A in (%1 %2 %3 %4 %5 %6 %7 %8 %9) do if X%%A==X-dedicated goto ms_dedicated
start wjview /vst /cp collections.jar;TinyXML.jar;PngEncoder.jar;TabPanel.jar;MegaMek.jar megamek/MegaMek %1 %2 %3 %4 %5 %6 %7 %8 %9
goto done
:ms_dedicated
echo Starting dedicated server.  Closing this window or pressing [ctrl]-[c] will
echo quit the server.
echo.
jview /vst /cp collections.jar;TinyXML.jar;MegaMek.jar megamek/MegaMek %1 %2 %3 %4 %5 %6 %7 %8 %9
goto done

:only_found_sun
echo Microsoft JVM not found.  Defaulting to Sun JVM from now on.
:sun_jvm_prefer
echo This file causes MegaMek to prefer the Sun JVM > mmconf\jvm_sun.cfg
:sun_jvm
for %%A in (%1 %2 %3 %4 %5 %6 %7 %8 %9) do if X%%A==X-dedicated goto sun_dedicated
start javaw -Xmx128m -jar MegaMek.jar %1 %2 %3 %4 %5 %6 %7 %8 %9
goto done
:sun_dedicated
echo Starting dedicated server.  Closing this window or pressing [ctrl]-[c] will
echo quit the server.
echo.
java -jar MegaMek.jar %1 %2 %3 %4 %5 %6 %7 %8 %9
goto done

:sun_ok
echo.
echo Found Sun JVM.  Trying Microsoft JVM as well...
jview > NUL
if errorlevel 2 goto only_found_sun
echo Found Microsoft JVM.
echo.
echo Your system appears to have multiple JVMs installed. This means
echo that you have multiple ways of running MegaMek.  Here are some
echo differences between the two:
echo.
echo Sun JVM: This is the "official" JVM.  It is more up to date,
echo meaning it receives continuing bugfixes.  Also, some advanced
echo features (like transparency and zooming) require this JVM.
echo.
echo Microsoft JVM: This JVM integrates better with Windows, which
echo results in MegaMek's interface running faster.
:choose
echo.
echo Note: This menu will not be displayed anymore once you have made
echo your choice.  To choose again, delete any file that begins with
echo "jvm" in the mmconf directory.
echo.
echo Press [s] for the Sun JVM, [m] for the Microsoft JVM, or [q] to quit.
echo.
if %os% == Windows_NT goto choice_nt
choice /c:smq Please make your choice now: 
if errorlevel 3 exit
if errorlevel 2 goto ms_jvm_prefer
if errorlevel 1 goto sun_jvm_prefer
echo That is not a valid choice. Please try again.
echo.
goto choose

:choice_nt
set Choice=
set /p Choice=Type the letter and press [enter]: 
if not '%Choice%'=='' set Choice=%Choice:~0,1%
echo.
if /i '%Choice%'=='s' goto sun_jvm_prefer
if /i '%Choice%'=='m' goto ms_jvm_prefer
if /i '%Choice%'=='q' exit
echo "%Choice%" is not a valid choice. Please try again.
echo.
goto choose

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