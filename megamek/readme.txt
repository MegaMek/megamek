PLAYING THE GAME
----------------

Pre-game Lounge:

Here you can chat, specify what mechs you wish to use, select map setting,
declare starting positions, and change your player's color.  When everybody
has at least one mech, you may all hit "ready" to start the game.  Most
changes to the game parameters will cancel your ready status, so you should
wait for everybody to finish choosing their mechs and positions.

Initiative Report:

Each player's initiative rolls and the corresponding turn order will be shown
here.


Movement Phase:

The buttons at the bottom right of the screen let you change between different
modes of movement, switch to another unit, or commit to your current path.
You do not have to move the first selected unit first.

Left-click on the map to specify a hex to move to.  A path should appear on
the board, showing your unit's path to the target hex.  The numbers in the
center represent how many movement points you will have to use to reach each
hex.  Green indicates walking, yellow running, and  cyan jump movement.  Red
sections of the path indicate where you have exceeded your movement capacity,
or other illegal moves.  You can drag the mouse to see movement options for 
several different hexes.

Units can also be moved by using "waypoints."  By clicking each hex between 
the unit's current location and destination, you can delineate the exact path 
to be taken.

Holding the Shift key while clicking on the map allows you to change the 
unit's facing without moving.  Since you automatically change facing while 
moving, this is best done at the end of any movement.

To back up, click the "Back Up" button before clicking a hex in the arc behind 
the unit.  To jump, click the "Jump" button before the destination hex.  
Facing changes during jumps are free.

Prone units can change facing without getting up.  To get up, click the 
"Get Up" button, then the desired destination hex and/or facing; facing 
changes within the same hex are free to a unit trying to get up.  To get up 
and back up, click "Get Up," then "Back Up," then the destination hex and/or 
facing.  You cannot get up and jump in the same turn.

The Esc key clears all current movement.


Movement Report:

If any units needed piloting skill rolls during their movement, a report
showing the results of these rolls will be shown.


Weapons Fire Phase:

The buttons at the bottom right of the screen allow you to fire your weapons,
switch to another unit, or commit to your currently declared fire.  Again, 
you do not need to declare fire for your first selected unit first.

To target another unit, click on it on the board.  Targetting information
for your current weapon should appear in the mech display window.  To fire
your current weapon at the target, press the "Fire" button at the bottom of 
the screen.  To switch to another weapon, click on its name at the top of the
mech display window.  Spread your fire among multiple targets by repeating 
these steps using unassigned weapons.  When all desired weapons have been 
assigned to a target, press the "Done" button at the bottom of the screen.

Note that fire is resolved in the order that it is declared, so if you feel 
that it is a good idea to fire weapons in a different order than they are 
listed, go ahead.

Secondary facing changes (torso twists) are accomplished by holding Shift and 
clicking on the board.  Your unit will attempt to change its secondary facing 
in the direction specified.  If you assign a weapon to a target and then
attempt a facing change, the weapons fire will be cancelled.

You can not switch to another unit after declaring some weapons fire.  To 
switch to another unit, first cancel all current fire by hitting the Esc key.  
When you hit the fire button for your last available weapon, all declared 
fire will be committed.


Weapons Fire Report:

If there were any weapon attacks, the server will resolve them all at the end
of the phase in the order they were declared.  The results will be shown in a
report.


Physical Attacks Phase:

To declare a physical attack, select your target on the board and click the
button corresponding to the attack you want to make.  If a physical attack 
type is unavailable (or isn't programmed yet) the button will be greyed out.

Units ineligible to make physical attacks due to being out of range, having
made weapons attacks, or for any other reason, will be skipped.  If all units 
are ineligible, the entire phase will be skipped.


End of Turn Report:

If there were any physical attacks, the results will be shown.  
The results of the Heat and End phases will be shown.  

After this phase, it's time for initiative again!  Hurrah!



DIFFERENCES BETWEEN THE BOARD GAME AND MEGAMEK
----------------------------------------------
Although MegaMek tries to be faithful to the original board game rules.  In
some cases, due to technical or design limitations, this is not possible.
These differences are not considered "bugs."

- You can jump in any path, not just the shortest one.
- You can only get up at the beginning of your turn.  If you fall during
    movement, you're down for the round.
- When punching, you automatically punch with both arms, if possible.  This
    means you can not punch two different targets in the same round.
- When kicking, you automatically use the leg with the better chance to hit.
- Some more that I've forgotten...


RUNNING MEGAMEK
---------------
Java programs run in their own enviroment, called a Virtual Machine or VM
for short.  These Java VMs are available on most systems from a variety of 
sources.  Most modern systems have one installed by default.

Windows users: To start MegaMek, run the MegaMek.exe file.

Other graphical OSes: Many other graphical OSes, such as MacOS and OSX, will
allow you to double-click the .jar file to run it.  If this does not work,
try running MegaMek from the command line

Running MegaMek from the command line: To do this using Sun Java, or most 
other implementaions, navigate to the directory containing the .jar file and 
run:

java -jar MegaMek.jar

If your Java VM does not support .jar files, the compiled Java .class files
are also included in the "classes" directory.  You will need to set the 
classpath to this folder and run MegaMek.class.  To do this using Sun Java,
run:

java -cp classes megamek/MegaMek


INSTALLING OR UPDATING YOUR JAVA RUNTIME
----------------------------------------
If your system did not come with a Java VM, or you need to update your Java
VM, here are some links.

Windows (Microsoft VM):
http://www.microsoft.com/java/vm/dl_vm40.htm

Sun Java (Linux, Solaris, Windows):
http://java.sun.com/j2se/1.4/download.html

Mac OSX 10.1 and above:
Java updates are included in OS updates for 10.1 and above.  There is no
external way to update your Java VM.

Earlier Mac OS versions:
http://developer.apple.com/java/download.html

