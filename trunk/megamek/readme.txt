
INTRODUCTION
------------
MegaMek is a version of BattleTech that you can play with your friends 
over the internet.  It it written in Java.  All of the level 1 (3025) 
equipment and nearly all of the level 1 rules are functioning. When these 
are done, work will begin on the level 2 rules and equipment. 

If you would like information about how to play the game, see the "PLAYING
THE GAME" section, below.  If you're having trouble getting MegaMek started, 
see "RUNNING MEGAMEK," further below.


CONNECTING
----------
MegaMek is a network game.  One player hosts a game and the rest of the
players connect.  The connecting players can connect from anywhere with a 
TCP/IP connection to the host, including the same computer (see hotseat, below.)

To host a game, press the "Host a New Game..." button in the main menu.  Fill
in your name and click "Okay".  The password field allows you to specify a
password for certain server commands.  (Type /help in the chat line to get a
list of server commands.)  If you don't specify a password, anybody is allowed
to reset the server or kick players, so be careful.

Once the lobby screen comes up, other players can connect and the game can 
begin.  The other players will need to know the IP address of the host computer.
There are several ways to determine your IP address.  If you are on 
Windows 9x/ME, you can use the Run command on the start menu and enter 
"winipcfg".  On Windows 2000/NT/XP, you will need to open a command prompt and 
type "ipconfig".  As a last resort, there are some web pages, such as 
www.whatismyip.com that will try to detect your IP address, but if your internet
service uses a proxy or firewall, these pages cannot accurately detect it.

To connect to a game, press the connect button on the main menu and fill in your
player name and the host's IP address.

To play in a hotseat fashion, you can run the host and any number of other 
players on the same computer.  First, launch and host a game.  Then, launch
another copy of the game for each connecting player, and in the "Server Address"
field of the connect dialog, type "localhost".  Each player will need to switch
to his/her copy of the game to play their turn.


PLAYING THE GAME
----------------

Pre-game Lobby:

Here you can chat, specify what mechs you wish to use, select map settings,
declare starting positions, and change your player's color.  When everybody
has at least one mech, you may all hit "ready" to start the game.  Most
changes to the game parameters will cancel your ready status, so you should
wait for everybody to finish choosing their mechs and positions.

You can enter the map selector by pressing the "Edit/View" button under the
map sizes.  You can change the dimensions of the map boards (in hexes) or the
dimensions of the whole map (in boards.)  All maps are loaded off the server.
All the map boards that MegaMek comes with are 16x17.  Whenever you change
either of these values, you will need to hit the "Update Size Settings" button
to have the rest of the dialog reflect the new values.

Below the map size inputs is a not-to-scale representation of how the boards
are laid out relative to each other.  The middle column lists the current maps
and the rightmost column lists the available maps.  To change a current map,
select it in the middle column, select the map you want in the right column,
and press the "<<" button between the two columns.  You may also select a map
by clicking on its number in the map layout grid.

[SURPRISE] means that the Server will pick a random map and not tell you what
it has picked until the game starts.  [RANDOM] means that the Server will pick
a map as soon as you hit "Okay".


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

Prone units can change facing without getting up at the normal cost.  To get up, 
click the "Get Up" button, then the desired destination hex and/or facing.  
Facing changes performed immediately after getting up are free.  To get up and 
back up, click "Get Up," then "Back Up," then the destination hex and/or 
facing.  You cannot get up and jump in the same turn.

Charging and death from above attacks are also declared during the movement 
phase.  Click the "Charge" or "D.F.A." button and then click on the mech you
wish to target.  If the attack is valid, it will be sent to the server 
immediately (but resolve during the physical attack phase.)  If the program is
not using the path you want for your attack, you may plot a path near the 
target using the appropriate type of movement and then use the charge or dfa
button to complete the attack.

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
These differences are not considered "bugs."  If you spot any more 
discrepancies, please contact the author (see "CONTACT" below.)

- You can jump in any path, not just the shortest one.

- You can only get up at the beginning of your turn.  If you fall during
    movement, you're down for the round.
    
- If the line of fire lies along the edge of two hexes, in the board game,
    the defender chooses which hex to use.  Currenty, MegaMek arbitrarily picks
    a hex.  In the future, this will be modified to favor the defender.
    
- When punching, you automatically punch with both arms, if possible.  This
    means you can not punch two different targets in the same round.
    
- When kicking, you automatically use the leg with the better chance to hit.

- Pushing... this is a rule breaker any way you look at it.  The way it's
    handled now is that pushing, and the accompanying unit displacement,
    happen as it's resolved, in the same phase as other physical attacks.
    This means pushing could interrupt an attack from a mech that "won"
    initiative, which seems wrong.  Any suggestions?

- There are several situations, notably death from above, where a unit is
    displaced out of a hex, and that unit's owner may pick the hex to move to.
    MegaMek currently picks the hex for you, choosing high elevations over low
    ones, to avoid falling damage.
    
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



CONTACT & FURTHER INFORMATION
-----------------------------
For more information, and to get the latest version of MegaMek, visit the
website at:
http://megamek.sourceforge.net/

For more information about the BattleTech board game, visit it's website at:
http://www.classicbattletech.com/


To contact the author with bug reports, suggestions, or anything else, visit 
the contact page on the MegaMek website, or send email to bmazur@sev.org



COPYRIGHT & LICENSE INFORMATION
-------------------------------
MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
  
This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by the Free 
Software Foundation; either version 2 of the License, or (at your option) 
any later version.
  
This program is distributed in the hope that it will be useful, but 
WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
for more details.

