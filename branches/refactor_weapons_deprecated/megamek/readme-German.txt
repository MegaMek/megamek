EINLEITUNG
----------
MegaMek ist eine Version von BattleTech, die ihr mit euren Freunden �bers
Internet oder LAN spielen k�nnt. Es wurde mit JAVA geschrieben.  Das ganze
Equipment von Level 1 (3025), der Gro�teil des Level 2 Equipments sowie beinahe
alle Level 1 und 2 Regeln sind enthalten. Ausserdem sind sogar einen Level 3
Regeln enthalten.

Wenn ihr Informationen dar�ber braucht, wie man das Spiel spielt, schaut in
die "DAS SPIEL SPIELEN" Sektion unten. Solltet ihr Probleme haben MegaMek zu
starten, schaut einfach in die "MEGAMEK STARTEN" Sektion weiter unten.

MEGAMEK STARTEN
---------------
Java Programme laufen in ihrer eigenen Umgebung, genannt 'Virtual Machine'
oder kurt VM. Diese Java VMs sind f�r die meisten Systeme aus unterschiedlichen
Quellen verf�gbar. Die meisten Systeme haben eine standardm�ssig installiert.

Windows Nutzer: Um MegaMek zu starten, starte die MegaMek.bat Datei. Wenn das
nicht funktioniert, siehe die "INSTALLATION ODER AKTUALISIERUNG DER JAVA 
LAUFZEIT UMGEBUNG" Sektion, weiter unten.

Andere graphische Betriebssysteme: Viele andere graphische Betriebssysteme, wie
MacOS und OSX erlauben das Doppelklicken der "MegaMek.jar" Datei um sie aus-
zuf�hren. Wenn das nicht funktionert, versuche MegaMek von der Kommandozeile
aus zu starten.

MegaMek von der Kommandozeile starten: Um dies zu tun, wenn Sun Java, oder die
meisten anderen Implementationen, verwendet wird, navigiere in das Verzeichnis
das die "MegaMek.jar" Datei enth�lt, und f�hre:

java -jar MegaMek.jar

aus.

Wenn keine der obigen Optionen funktioniert, siehe die "INSTALLATION ODER
AKTUALISIERUNG DER JAVA LAUFZEIT UMGEBUNG" Sektion, weiter unten.

Wenn Du eine VM hast, diese aber keine .jar Dateien unterst�tzt, benutze das
Entpackprogramm Deiner Wahl um "MegaMek.jar", "collections.jar", "TinyXML.jar" 
und "Ostermiller.jar" in ein neues "classes" Verzeichnis (erstelle dieses
im "megamek" Ordner) zu entpacken. Wenn Du das durchgef�hrt hast (dies muss
nur einmal gemacht werden), muss der classpath auf das "classes" Verzeichnis
sowie auf das aktuelle Verzeichnis gesetzt werden, und "MegaMek.class" ausge-
f�hrt werden. Zum Beispiel bei Nutzung von Sun Java, f�hre:

java -cp .;classes megamek/MegaMek

aus.

Beachte das in einigen F�llen das Semikolen escaped werden muss:
java -cp .\;classes megamek/MegaMek


INSTALLATION ODER AKTUALISIERUNG DER JAVA LAUFZEIT UMGEBUNG
-----------------------------------------------------------

Wenn Dein System nicht mit einer Java VM geliefert wurde, oder Du Deine Java
VM aktualisieren musst, sind hier einige Links.

Sun Java (Linux, Solaris, Windows):
http://java.sun.com/j2se/downloads.html

Microsoft VM (Windows):
Microsoft stellt seine VM nicht mehr f�r den separaten Download zur Verf�gung.
Bitte frag im Forum (http://sourceforge.net/forum/forum.php?forum_id=154580)
nach Hilfe. Beachte das Du die Java VM von Sun (oben) als Alternative nutzen
kannst, da Sun eine Windows Version zur Verf�gung stellt.

Mac OSX 10.1 und h�her:
Java Aktualisierungen sind in den OS-Aktualisierungen f�r 10.1 und h�her ein-
geschlossen. Es gibt keinen externe Weg, die Java VM zu aktualisieren.

Fr�here Mac OS Versionen:
http://developer.apple.com/java/download.html

IBM VM:
MegaMek funktioniert nur schlecht mit dieser VM - sie ist nicht empfohlen.


VERBINDEN
---------
MegaMek ist ein Netzwerk Spiel. Ein Spieler �ffnet den Server f�r das Spiel 
und der Rest muss sich mit diesem Server verbinden. Die verbindenden Spieler
k�nnen von �berall her mit der TCP/IP Verbindung zum Server connecten,
inklusive des selben Computer auf dem der Server l�uft. 
(Siehe Hotseat, weiter unten)

Um einen Server zu starten, klickt man auf den "Host a New Game..." Button
im Main Men�.  Bestimmt euren Namen und klickt dann auf "Okay". Das password
Feld erlaubt es, ein bestimmtes Passwort f�r bestimmte Serverbefehle zu setzen
(Tippe /help im Chatfenster um eine Liste der Serverbefehle zu bekommen). 
Wenn dann der Lobby Bildschirm auftaucht, k�nnen die anderen Spieler connecten,
und das Spiel kann beginnen. Die anderen Spieler m�ssen die IP Adresse des
Servers kennen um sich mit ihm zu verbinden. Um diese unter Windows 95,
98 oder ME zu erfahren muss man ins Startemen� gehen, und dann bei "AUSF�HREN"
"WINIPCFG" eingeben. In Windows 2000 oder XP dort zuerst "CMD" und im dann
aufspringenden Fenster "IPCONFIG" eingeben. Ausserdem gibt es einige 
Internetseiten, wie zum Beispiel www.whatismyip.com, wo sie euch gezeigt wird
(dies funktioniert aber nicht, wenn der PC sich hinter einem Proxy oder Gateway
befindet.)

Um an einem Spiel teilzunehmen, dr�ckt man den "Connect to a Server..."
Button im Main Men� und gibt dann seinen Namen und die Server IP Adresse
ein.

Um eine Hotseat Partie zu spielen (Mehrere Personen an einem PC), kann man
den Server auf dem PC starten, und dann so oft zu sich selbst connecten wie
Leute mitspielen wollen. Als erstes starten man MegaMek und macht einen
Server auf. Dann starten man MegaMek nochmal, so oft wie Spieler mitspielen
wollen, und in der Spalte "Server Address" im Verbindungsfenster gibt
man "localhost" ein. Jeder Spieler muss dann zu seinem Fenster von MegaMek
wechseln um seinen Zug zu machen.

Um einen dedizierten Server zu starten, benutze die -dedicated Kommandozeilen-
option. Ein dedizierter Server l�sst nur den Server in der Konsole laufen,
ohne einen "Host" Spieler im Spiel. Mitspieler, inklusive Dirselbst, k�nnen
sich mit dem Server verbinden wie bei einem Spiel mit Host. Der dedizierte
Server liest das Passwort und die Portoptionen aus der MegaMek.cfg Datei,
oder verwendet die Standardoptionen (kein Passwort und Port 2346).
Wenn Du willst, das der dedizierte Server ein gespeichertes Spiel l�dt,
�bergib den Dateinamen nach der -dedicated Option, z.B. so:

 "-dedicated gespeichertesspiel.sav"

Um den Server zu stoppen, musst Du die Java VM anhalten, was auf den meisten
Plattformen durch dr�cken von STRG + C erreicht wird.


DAS SPIEL SPIELEN
----------------

VOR-DEM-SPIEL BERICHT:

Hier kann man chatten, seine Mechs aussuchen, die Karte w�hlen, seinen
Startpunkt aussuchen, und seine Spielerfarbe einstellen . Sobald jeder
mindesten einen Mech hat, k�nnen alle auf "ready" klicken und das Spiel kann
beginnen. Die meisten �nderungen an den Spiel Parametern setzten den ready
status zur�ck, also sollte man am besten warten bis alle ihre Mechs und
Positionen gew�hlt haben.

EINLEITUNGS-REPORT:

Die Anfangsw�rfe jedes Spielers und die entsprechende Zug-Reihenfolge werden
hier angezeigt.

AUFSTELLUNGSPHASE:

In dieser Phase kann man seine Einheiten in den gelb umrandeten Hexfeldern 
aufstellen, falls vorher eine bestimmte Startposition gew�hlt wurde. Anonsten
kann man seine Einheiten aufstellen, wo man will.


BEWEGUNGS PHASE:

Mit den Kn�pfe am rechten unteren Rand kann man zwischen verschiedenen
Bewegungsarten w�hlen, zwischen den Einheiten wechseln oder den gew�hlten
Bewegungs Pfad best�tigen. Man muss nicht die erste angew�hlte Einheit
bewegen.

Ein Links-Klick auf die Karte w�hlt das Hex-Feld aus, auf das man ziehen
will. Der Pfad muss komplett ausgew�hlt sein, und im Bereich der m�glichen
Z�ge liegen. Die Nummern im Zentrum zeigen an, wie viele Bewegungspunkte
man ben�tigt um den Zug durch zu f�hren. HELLBLAUE Farbe zeigt den GEHEN-Bereich
an, GELB den LAUFEN-Bereich und ROT den Bereich in den man Springen kann.
GRAUE Bereiche zeigen an, das man seine Reichweite �berschritten hat, oder
andere unm�gliche Bewegungen. Wenn man seinen Mauszeiger herum zieht, kann
man sich verschiedene Wege �ber die Hex-Felder ansehen.

Einheiten k�nnen sich auch bewegen, indem sie Wegpunkte benutzen. Man muss
dann einfach jedes Hex-Feld anklicken, zwischen der derzeitigen Position der
Einheit und dem Ziel. Man kann so exakt den Weg beschreiben, den die Eihneit
nehmen soll.

Wenn man die Shift Taste dr�cken, w�hrend man auf die Karte klicken, kann
man die Sicht-Richtung der gew�hlten Einheit bestimmen, ohne die Einheit zu
bewegen. Am besten macht man das am Ende des Zuges.

Um sich zur�ck zu bewegen, dr�ckt man den "Back Up" Knopf bevor man den Weg hinter
dem Mech festlegt. Um zur�ck zu springen, dr�ckt man erst "Back Up" und dann "Jump".
Sichtrichtungs wechsel w�hrend des Sprunges sind frei.

Hingefallene Einheiten k�nnen die Sichtrichtung �ndern, ohne aufzustehen.
Man klickt auf "Get Up", und dann gew�nschte Blickrichtung. Blickrichtungs�nderungen
im selben Feld sind frei f�r Einheiten die versuchen Aufzustehen. Um Aufzustehen,
und sich dabei zur�ck zu ziehen, muss man erst "Get Up" anklicken, und dann "Back Up"
und dann das gew�nschte Feld oder die gew�nschte Richtung. Man kann im selben Zug nicht
aufstehen und springen.

Den Piloten eines Mechs kann man �ber den 'EJECT' Knopf aussteigen lassen, dieser Mech
ist dann sofort aus dem Spiel.

Rammen und Todessprung (D.F.A.) sind Aangriffe, die w�hrend der Bewegungsphase
hemacht werden m�ssen. Man Klickt auf den "Charge" oder "D.F.A." Knopf und klickt
dann auf die Einheit die man angreifen will. Wenn der Angriff durchgef�hrt werden kann,
wird es sofort ausgef�hrt, und der Zug ist beendet. Das Ergebnis wird dann bei den
physischen Anngriffen aufgef�hrt. Sollte MEGAMEK nicht denn gew�nschten Weg benutzen, 
kann man selber den Pfad auswehlen, indem man einen eigenen Weg in die N�he des Ziels
aufbaut, und dann auf "Charge" oder "D.F.A." klickt um den Angriff zu beenden.

Die Esc Taste stop alle derzeitigen Z�ge.


Bewegungs Report:

Wenn irgendeine Einheit w�hrend der Bewegung die Piloten F�higkeit ausw�rfeln muss,
so werden die Ergebnisse in einem Bericht gezeigt, ebenso wie ausgestiegene Piloten.


WAFFEN-FEUER PHASE:

Wenn Du Entfernung und Sichtline (LoS) zwischen zwei Hexfeldern �berpr�fen
willst, hast Du die Wahl zwischen zwei Tools: Das "LoS tool" und den "Ruler".
Um das "LoS tool" zu benutzen, halte STRG und clicke die beiden Hexfelder die 
du �berpr�fen willst. Der "Ruler" funktioniert wie das "LoS tool", nutzt aber
die ALT Taste anstatt der STRG Taste. Der "ruler" kann auch genutzt werden,
indem man mit der mittleren Maustaste einer drei-Tasten Maus die beiden Hex-
felder anklickt. Beide Tools �ffnen ein Fenster das mitteilt, welches Terrain
zwischen den beiden Feldern liegt und ob eins oder beide Hexfelder teilweise
Deckung hat. Der "ruler" zieht ausserdem die Sichtlinie auf dem Spielfeld, so
das man sehen kann durch welche Hexfelder die Los geht.

Du kannst zwischen 'Mech und nicht-'Mech Sichtlinie im "LoS Setting" Fenster
umschalten, dies erreichst Du �ber das "View" Men� am oberen Ende des Fensters.
'Mech- und nicht-'Mech Sichtlinie unterscheidet sich dadurch, das 'Mechs von
einem Level h�her als das H�henlevel des Terrains "gucken", w�hrend Infantrie
und Panzer nur das H�henlevel des Terrains beanspruchen.

Die Buttons am unteren Rand erlauben euch, eure Waffen abzufeuern,
eine andere Einheit auszuw�hlen, oder euren derzeitigen Schuss zu
best�tigen. Nochmal, ihr m�sst nicht mit der als erstes gew�hlten Einheit
feuern.

Um auf eine andere Einheit zu zielen, klickt man sie auf dem Spielfeld an.
Um auf W�lder zu zielen, klickt man diese mit der rechten Maustaste an und
w�hlt, ob man sie roden oder in Brand setzen m�chte.
Mit dem "MODE" Knopf kann man bei einigen Waffen den Feuermodus umschalten,
z.B. bei Ultra- oder Rotations-Autokanonen oder bei LBX-Autokanonen.
Die Zielinformationen �ber die grade gew�hlte Waffe werden dann im 
MECHINFORMATIONSFELD angezeigt. Um die gew�hlte Waffe aufs Ziel abzufeuer,
dr�ckt man den "FIRE" Knopf. Um zu einer anderen Waffe zu wechseln, klickt auf
den Namen der Waffe in der Liste im MECHINFORMATIONSFELD. So kann man sein Feuer
auf verschiedene Ziele verteilen, indem man diese Schritte wiederholt. Wenn alle
Waffen auf ihre Ziele ausgerichtet sind, bzw. nicht abgefeuert werden k�nnen, 
dr�ckt man den "DONE" Knopf.

Die Waffen werden dann in der Reihenfolge abgefeuert, in der sie ausgew�hlt worden
sind.

Man kann seine Blickrichtung ein zweites Mal �ndern (Torso drehung), wenn man die
Shift-Taste gedr�ckt halt, und dann auf ein entsprechendes Feld in der gew�nschten
Blickrichtung klickt. Am Rand der Karte geht dies nur �ber die Shift-Taste und die
Pfeiltasten der Tastatur. Die gew�hlte Einheit wird nun versuchen den Torso in die
gew�nschte Richtung zu drehen. Wenn man mit einer Waffe auf eine Einheit zielt,
und dann die Blickrichtung �ndert, werdern alle Feuerbefehle gel�scht, und m�ssen
nochmal gegeben werden.

Man kann nicht mehr zu einer anderen Einheit wechseln, sobald man einmal mit
einer Waffe gefeuert hat. Um zu einer anderen Einheit zu wechseln, muss man dann
erst das gesamte Waffenfeuer abbrechen, indem man die ESC Taste dr�clt.
Wenn man die "FIRE" Knopf f�r de letzte vorhandene Waffe dr�ckt, werden alle
Sch�sse ausgef�hrt, und der n�chste Spieler ist am Zug.


WAFFEFEUER BERICHT:

Sollten irgendwelche Waffen abgefeuert worden sein, so wird der Server alle
Ergebnisse am Ende der Runde in einem Feuer-Bericht anzeigen.


PHYSISCHE-ANGRIFFS PHASE:

Um einen physische Angriff auszuf�hren, w�hlt man zuerst sein Ziel und klickt
dann auf den Knopf f�r den entsprechenden Angriff (KICK/PUNCH/PUSH). Sollte
einer der Angriffe nicht m�glich sein (oder noch nicht einprogramiert), so
wird der Knopf GRAU, und nicht aktiv.


ENDE-DES-ZUGS BERICHT:

Sollten irgendwelche physikalischen Angriffe statt gefunde haben
(Schl�ge/Tritte/Todesspr�nge/Rammangriffe), so werden hier die Ergebnisse
gezeigt, sowie die Resultate der Hitze und evtl. brennende W�lder und die
Felder, die mit Rauch gef�llt sind.

Nach dieser Phase beginnt eine neue Runde.



UNTERSCHIEDE ZWISCHEN DEM BRETTSPIEL UND MEGAMEK
------------------------------------------------
MEGAMEK versucht den original Brettspiel Regeln treu zu bleiben. In einigen
F�llen, die aufgrund der technischen- und design- Beschr�nkungen auftreten,
ist dies nicht immer m�glich. Diese Unterschiede sind keine Fehler (BUGS).
Solltet ihr noch irgendwelche Unterschide feststellen, kontaktet bitte den
Autor (wie, steht in "CONTACT" unten.)

- Man kann jeden Weg springen, nicht nur den k�rzesten.
    
- Wenn die Feuerlinie entlang zweier Hex-Feld-Kanten liegt, w�hlte im
Brettspiel der Verteidiger aus, �ber welches Feld der Angreifer schiessen
muss, Derzeit w�hlt MEGAMEK einfach das f�r den Verteidiger vorteilhaftere
Feld aus (so da� der Verteidiger schwerer zu treffen ist)

- W�hrend eines Schlages, schl�gt man automatisch mit beiden Armen, wenn
dies m�glich ist. Das heisst, du kannst nicht zwei Ziele in einer Runde
schlagen.

- Bei einem Tritt, benutzt der Mech automatisch das Bein, das bessere Chancen
auf einen Treffer hat.

- Es gibt viele Situationen, besonder den ANGRIFF VON OBEN (D.F.A *DEATH
FROM ABOVE*), bei dem die Einheit in ein anderes Feld verschoben wird.
Normal kann der Spieler dieses Feld selbst w�hlen, MEGAMEK allerdings
bestimmt derzeit diese Feld noch selbst f�r sie (und w�hlt dabei das Feld
mit der h�chsten H�henstufe, so da� unbeabsichtiges Fallen von Klippen
vermieden wird)
    
- Ein paar die ich vergessen habe...


KONTAKT & WEITERE INFORMATIONEN
-------------------------------
F�r mehr Informationen, und um die neueste MEGAMEK Version zu bekommen, besucht die Homepage:
http://megamek.sourceforge.net/

F�r mehr Informationen �ber das Battletech Brettspiel besucht:
http://www.classicbattletech.com/ oder http://www.battletech.info/


Um dem Autor Fehler Berichte, Vorschl�ge oder sonst was zu machen, besucht
die 'contact' Seite auf der MegaMek Webseite, oder schickt eine E-Mail an
bmazur@sev.org



COPYRIGHT & LICENSE INFORMATION
-------------------------------
MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
  
This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by the Free 
Software Foundation; either version 2 of the License, or (at your option) 
any later version.
  
This program is distributed in the hope that it will be useful, but 
WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
for more details.

