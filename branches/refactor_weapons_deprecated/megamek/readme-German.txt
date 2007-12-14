EINLEITUNG
----------
MegaMek ist eine Version von BattleTech, die ihr mit euren Freunden übers
Internet oder LAN spielen könnt. Es wurde mit JAVA geschrieben.  Das ganze
Equipment von Level 1 (3025), der Großteil des Level 2 Equipments sowie beinahe
alle Level 1 und 2 Regeln sind enthalten. Ausserdem sind sogar einen Level 3
Regeln enthalten.

Wenn ihr Informationen darüber braucht, wie man das Spiel spielt, schaut in
die "DAS SPIEL SPIELEN" Sektion unten. Solltet ihr Probleme haben MegaMek zu
starten, schaut einfach in die "MEGAMEK STARTEN" Sektion weiter unten.

MEGAMEK STARTEN
---------------
Java Programme laufen in ihrer eigenen Umgebung, genannt 'Virtual Machine'
oder kurt VM. Diese Java VMs sind für die meisten Systeme aus unterschiedlichen
Quellen verfügbar. Die meisten Systeme haben eine standardmässig installiert.

Windows Nutzer: Um MegaMek zu starten, starte die MegaMek.bat Datei. Wenn das
nicht funktioniert, siehe die "INSTALLATION ODER AKTUALISIERUNG DER JAVA 
LAUFZEIT UMGEBUNG" Sektion, weiter unten.

Andere graphische Betriebssysteme: Viele andere graphische Betriebssysteme, wie
MacOS und OSX erlauben das Doppelklicken der "MegaMek.jar" Datei um sie aus-
zuführen. Wenn das nicht funktionert, versuche MegaMek von der Kommandozeile
aus zu starten.

MegaMek von der Kommandozeile starten: Um dies zu tun, wenn Sun Java, oder die
meisten anderen Implementationen, verwendet wird, navigiere in das Verzeichnis
das die "MegaMek.jar" Datei enthält, und führe:

java -jar MegaMek.jar

aus.

Wenn keine der obigen Optionen funktioniert, siehe die "INSTALLATION ODER
AKTUALISIERUNG DER JAVA LAUFZEIT UMGEBUNG" Sektion, weiter unten.

Wenn Du eine VM hast, diese aber keine .jar Dateien unterstützt, benutze das
Entpackprogramm Deiner Wahl um "MegaMek.jar", "collections.jar", "TinyXML.jar" 
und "Ostermiller.jar" in ein neues "classes" Verzeichnis (erstelle dieses
im "megamek" Ordner) zu entpacken. Wenn Du das durchgeführt hast (dies muss
nur einmal gemacht werden), muss der classpath auf das "classes" Verzeichnis
sowie auf das aktuelle Verzeichnis gesetzt werden, und "MegaMek.class" ausge-
führt werden. Zum Beispiel bei Nutzung von Sun Java, führe:

java -cp .;classes megamek/MegaMek

aus.

Beachte das in einigen Fällen das Semikolen escaped werden muss:
java -cp .\;classes megamek/MegaMek


INSTALLATION ODER AKTUALISIERUNG DER JAVA LAUFZEIT UMGEBUNG
-----------------------------------------------------------

Wenn Dein System nicht mit einer Java VM geliefert wurde, oder Du Deine Java
VM aktualisieren musst, sind hier einige Links.

Sun Java (Linux, Solaris, Windows):
http://java.sun.com/j2se/downloads.html

Microsoft VM (Windows):
Microsoft stellt seine VM nicht mehr für den separaten Download zur Verfügung.
Bitte frag im Forum (http://sourceforge.net/forum/forum.php?forum_id=154580)
nach Hilfe. Beachte das Du die Java VM von Sun (oben) als Alternative nutzen
kannst, da Sun eine Windows Version zur Verfügung stellt.

Mac OSX 10.1 und höher:
Java Aktualisierungen sind in den OS-Aktualisierungen für 10.1 und höher ein-
geschlossen. Es gibt keinen externe Weg, die Java VM zu aktualisieren.

Frühere Mac OS Versionen:
http://developer.apple.com/java/download.html

IBM VM:
MegaMek funktioniert nur schlecht mit dieser VM - sie ist nicht empfohlen.


VERBINDEN
---------
MegaMek ist ein Netzwerk Spiel. Ein Spieler öffnet den Server für das Spiel 
und der Rest muss sich mit diesem Server verbinden. Die verbindenden Spieler
können von überall her mit der TCP/IP Verbindung zum Server connecten,
inklusive des selben Computer auf dem der Server läuft. 
(Siehe Hotseat, weiter unten)

Um einen Server zu starten, klickt man auf den "Host a New Game..." Button
im Main Menü.  Bestimmt euren Namen und klickt dann auf "Okay". Das password
Feld erlaubt es, ein bestimmtes Passwort für bestimmte Serverbefehle zu setzen
(Tippe /help im Chatfenster um eine Liste der Serverbefehle zu bekommen). 
Wenn dann der Lobby Bildschirm auftaucht, können die anderen Spieler connecten,
und das Spiel kann beginnen. Die anderen Spieler müssen die IP Adresse des
Servers kennen um sich mit ihm zu verbinden. Um diese unter Windows 95,
98 oder ME zu erfahren muss man ins Startemenü gehen, und dann bei "AUSFÜHREN"
"WINIPCFG" eingeben. In Windows 2000 oder XP dort zuerst "CMD" und im dann
aufspringenden Fenster "IPCONFIG" eingeben. Ausserdem gibt es einige 
Internetseiten, wie zum Beispiel www.whatismyip.com, wo sie euch gezeigt wird
(dies funktioniert aber nicht, wenn der PC sich hinter einem Proxy oder Gateway
befindet.)

Um an einem Spiel teilzunehmen, drückt man den "Connect to a Server..."
Button im Main Menü und gibt dann seinen Namen und die Server IP Adresse
ein.

Um eine Hotseat Partie zu spielen (Mehrere Personen an einem PC), kann man
den Server auf dem PC starten, und dann so oft zu sich selbst connecten wie
Leute mitspielen wollen. Als erstes starten man MegaMek und macht einen
Server auf. Dann starten man MegaMek nochmal, so oft wie Spieler mitspielen
wollen, und in der Spalte "Server Address" im Verbindungsfenster gibt
man "localhost" ein. Jeder Spieler muss dann zu seinem Fenster von MegaMek
wechseln um seinen Zug zu machen.

Um einen dedizierten Server zu starten, benutze die -dedicated Kommandozeilen-
option. Ein dedizierter Server lässt nur den Server in der Konsole laufen,
ohne einen "Host" Spieler im Spiel. Mitspieler, inklusive Dirselbst, können
sich mit dem Server verbinden wie bei einem Spiel mit Host. Der dedizierte
Server liest das Passwort und die Portoptionen aus der MegaMek.cfg Datei,
oder verwendet die Standardoptionen (kein Passwort und Port 2346).
Wenn Du willst, das der dedizierte Server ein gespeichertes Spiel lädt,
übergib den Dateinamen nach der -dedicated Option, z.B. so:

 "-dedicated gespeichertesspiel.sav"

Um den Server zu stoppen, musst Du die Java VM anhalten, was auf den meisten
Plattformen durch drücken von STRG + C erreicht wird.


DAS SPIEL SPIELEN
----------------

VOR-DEM-SPIEL BERICHT:

Hier kann man chatten, seine Mechs aussuchen, die Karte wählen, seinen
Startpunkt aussuchen, und seine Spielerfarbe einstellen . Sobald jeder
mindesten einen Mech hat, können alle auf "ready" klicken und das Spiel kann
beginnen. Die meisten Änderungen an den Spiel Parametern setzten den ready
status zurück, also sollte man am besten warten bis alle ihre Mechs und
Positionen gewöhlt haben.

EINLEITUNGS-REPORT:

Die Anfangswürfe jedes Spielers und die entsprechende Zug-Reihenfolge werden
hier angezeigt.

AUFSTELLUNGSPHASE:

In dieser Phase kann man seine Einheiten in den gelb umrandeten Hexfeldern 
aufstellen, falls vorher eine bestimmte Startposition gewählt wurde. Anonsten
kann man seine Einheiten aufstellen, wo man will.


BEWEGUNGS PHASE:

Mit den Knöpfe am rechten unteren Rand kann man zwischen verschiedenen
Bewegungsarten wählen, zwischen den Einheiten wechseln oder den gewählten
Bewegungs Pfad bestätigen. Man muss nicht die erste angewählte Einheit
bewegen.

Ein Links-Klick auf die Karte wählt das Hex-Feld aus, auf das man ziehen
will. Der Pfad muss komplett ausgewählt sein, und im Bereich der möglichen
Züge liegen. Die Nummern im Zentrum zeigen an, wie viele Bewegungspunkte
man benötigt um den Zug durch zu führen. HELLBLAUE Farbe zeigt den GEHEN-Bereich
an, GELB den LAUFEN-Bereich und ROT den Bereich in den man Springen kann.
GRAUE Bereiche zeigen an, das man seine Reichweite überschritten hat, oder
andere unmögliche Bewegungen. Wenn man seinen Mauszeiger herum zieht, kann
man sich verschiedene Wege über die Hex-Felder ansehen.

Einheiten können sich auch bewegen, indem sie Wegpunkte benutzen. Man muss
dann einfach jedes Hex-Feld anklicken, zwischen der derzeitigen Position der
Einheit und dem Ziel. Man kann so exakt den Weg beschreiben, den die Eihneit
nehmen soll.

Wenn man die Shift Taste drücken, während man auf die Karte klicken, kann
man die Sicht-Richtung der gewählten Einheit bestimmen, ohne die Einheit zu
bewegen. Am besten macht man das am Ende des Zuges.

Um sich zurück zu bewegen, drückt man den "Back Up" Knopf bevor man den Weg hinter
dem Mech festlegt. Um zurück zu springen, drückt man erst "Back Up" und dann "Jump".
Sichtrichtungs wechsel während des Sprunges sind frei.

Hingefallene Einheiten können die Sichtrichtung ändern, ohne aufzustehen.
Man klickt auf "Get Up", und dann gewünschte Blickrichtung. Blickrichtungsänderungen
im selben Feld sind frei für Einheiten die versuchen Aufzustehen. Um Aufzustehen,
und sich dabei zurück zu ziehen, muss man erst "Get Up" anklicken, und dann "Back Up"
und dann das gewünschte Feld oder die gewünschte Richtung. Man kann im selben Zug nicht
aufstehen und springen.

Den Piloten eines Mechs kann man über den 'EJECT' Knopf aussteigen lassen, dieser Mech
ist dann sofort aus dem Spiel.

Rammen und Todessprung (D.F.A.) sind Aangriffe, die während der Bewegungsphase
hemacht werden müssen. Man Klickt auf den "Charge" oder "D.F.A." Knopf und klickt
dann auf die Einheit die man angreifen will. Wenn der Angriff durchgeführt werden kann,
wird es sofort ausgeführt, und der Zug ist beendet. Das Ergebnis wird dann bei den
physischen Anngriffen aufgeführt. Sollte MEGAMEK nicht denn gewünschten Weg benutzen, 
kann man selber den Pfad auswehlen, indem man einen eigenen Weg in die Nähe des Ziels
aufbaut, und dann auf "Charge" oder "D.F.A." klickt um den Angriff zu beenden.

Die Esc Taste stop alle derzeitigen Züge.


Bewegungs Report:

Wenn irgendeine Einheit während der Bewegung die Piloten Fähigkeit auswürfeln muss,
so werden die Ergebnisse in einem Bericht gezeigt, ebenso wie ausgestiegene Piloten.


WAFFEN-FEUER PHASE:

Wenn Du Entfernung und Sichtline (LoS) zwischen zwei Hexfeldern überprüfen
willst, hast Du die Wahl zwischen zwei Tools: Das "LoS tool" und den "Ruler".
Um das "LoS tool" zu benutzen, halte STRG und clicke die beiden Hexfelder die 
du überprüfen willst. Der "Ruler" funktioniert wie das "LoS tool", nutzt aber
die ALT Taste anstatt der STRG Taste. Der "ruler" kann auch genutzt werden,
indem man mit der mittleren Maustaste einer drei-Tasten Maus die beiden Hex-
felder anklickt. Beide Tools öffnen ein Fenster das mitteilt, welches Terrain
zwischen den beiden Feldern liegt und ob eins oder beide Hexfelder teilweise
Deckung hat. Der "ruler" zieht ausserdem die Sichtlinie auf dem Spielfeld, so
das man sehen kann durch welche Hexfelder die Los geht.

Du kannst zwischen 'Mech und nicht-'Mech Sichtlinie im "LoS Setting" Fenster
umschalten, dies erreichst Du über das "View" Menü am oberen Ende des Fensters.
'Mech- und nicht-'Mech Sichtlinie unterscheidet sich dadurch, das 'Mechs von
einem Level höher als das Höhenlevel des Terrains "gucken", während Infantrie
und Panzer nur das Höhenlevel des Terrains beanspruchen.

Die Buttons am unteren Rand erlauben euch, eure Waffen abzufeuern,
eine andere Einheit auszuwählen, oder euren derzeitigen Schuss zu
bestätigen. Nochmal, ihr müsst nicht mit der als erstes gewählten Einheit
feuern.

Um auf eine andere Einheit zu zielen, klickt man sie auf dem Spielfeld an.
Um auf Wälder zu zielen, klickt man diese mit der rechten Maustaste an und
wählt, ob man sie roden oder in Brand setzen möchte.
Mit dem "MODE" Knopf kann man bei einigen Waffen den Feuermodus umschalten,
z.B. bei Ultra- oder Rotations-Autokanonen oder bei LBX-Autokanonen.
Die Zielinformationen über die grade gewählte Waffe werden dann im 
MECHINFORMATIONSFELD angezeigt. Um die gewählte Waffe aufs Ziel abzufeuer,
drückt man den "FIRE" Knopf. Um zu einer anderen Waffe zu wechseln, klickt auf
den Namen der Waffe in der Liste im MECHINFORMATIONSFELD. So kann man sein Feuer
auf verschiedene Ziele verteilen, indem man diese Schritte wiederholt. Wenn alle
Waffen auf ihre Ziele ausgerichtet sind, bzw. nicht abgefeuert werden können, 
drückt man den "DONE" Knopf.

Die Waffen werden dann in der Reihenfolge abgefeuert, in der sie ausgewählt worden
sind.

Man kann seine Blickrichtung ein zweites Mal ändern (Torso drehung), wenn man die
Shift-Taste gedrückt halt, und dann auf ein entsprechendes Feld in der gewünschten
Blickrichtung klickt. Am Rand der Karte geht dies nur über die Shift-Taste und die
Pfeiltasten der Tastatur. Die gewählte Einheit wird nun versuchen den Torso in die
gewünschte Richtung zu drehen. Wenn man mit einer Waffe auf eine Einheit zielt,
und dann die Blickrichtung ändert, werdern alle Feuerbefehle gelöscht, und müssen
nochmal gegeben werden.

Man kann nicht mehr zu einer anderen Einheit wechseln, sobald man einmal mit
einer Waffe gefeuert hat. Um zu einer anderen Einheit zu wechseln, muss man dann
erst das gesamte Waffenfeuer abbrechen, indem man die ESC Taste drüclt.
Wenn man die "FIRE" Knopf für de letzte vorhandene Waffe drückt, werden alle
Schüsse ausgeführt, und der nächste Spieler ist am Zug.


WAFFEFEUER BERICHT:

Sollten irgendwelche Waffen abgefeuert worden sein, so wird der Server alle
Ergebnisse am Ende der Runde in einem Feuer-Bericht anzeigen.


PHYSISCHE-ANGRIFFS PHASE:

Um einen physische Angriff auszuführen, wählt man zuerst sein Ziel und klickt
dann auf den Knopf für den entsprechenden Angriff (KICK/PUNCH/PUSH). Sollte
einer der Angriffe nicht möglich sein (oder noch nicht einprogramiert), so
wird der Knopf GRAU, und nicht aktiv.


ENDE-DES-ZUGS BERICHT:

Sollten irgendwelche physikalischen Angriffe statt gefunde haben
(Schläge/Tritte/Todessprünge/Rammangriffe), so werden hier die Ergebnisse
gezeigt, sowie die Resultate der Hitze und evtl. brennende Wälder und die
Felder, die mit Rauch gefüllt sind.

Nach dieser Phase beginnt eine neue Runde.



UNTERSCHIEDE ZWISCHEN DEM BRETTSPIEL UND MEGAMEK
------------------------------------------------
MEGAMEK versucht den original Brettspiel Regeln treu zu bleiben. In einigen
Fällen, die aufgrund der technischen- und design- Beschränkungen auftreten,
ist dies nicht immer möglich. Diese Unterschiede sind keine Fehler (BUGS).
Solltet ihr noch irgendwelche Unterschide feststellen, kontaktet bitte den
Autor (wie, steht in "CONTACT" unten.)

- Man kann jeden Weg springen, nicht nur den kürzesten.
    
- Wenn die Feuerlinie entlang zweier Hex-Feld-Kanten liegt, wählte im
Brettspiel der Verteidiger aus, über welches Feld der Angreifer schiessen
muss, Derzeit wählt MEGAMEK einfach das für den Verteidiger vorteilhaftere
Feld aus (so daß der Verteidiger schwerer zu treffen ist)

- Während eines Schlages, schlägt man automatisch mit beiden Armen, wenn
dies möglich ist. Das heisst, du kannst nicht zwei Ziele in einer Runde
schlagen.

- Bei einem Tritt, benutzt der Mech automatisch das Bein, das bessere Chancen
auf einen Treffer hat.

- Es gibt viele Situationen, besonder den ANGRIFF VON OBEN (D.F.A *DEATH
FROM ABOVE*), bei dem die Einheit in ein anderes Feld verschoben wird.
Normal kann der Spieler dieses Feld selbst wählen, MEGAMEK allerdings
bestimmt derzeit diese Feld noch selbst für sie (und wählt dabei das Feld
mit der höchsten Höhenstufe, so daß unbeabsichtiges Fallen von Klippen
vermieden wird)
    
- Ein paar die ich vergessen habe...


KONTAKT & WEITERE INFORMATIONEN
-------------------------------
Für mehr Informationen, und um die neueste MEGAMEK Version zu bekommen, besucht die Homepage:
http://megamek.sourceforge.net/

Für mehr Informationen über das Battletech Brettspiel besucht:
http://www.classicbattletech.com/ oder http://www.battletech.info/


Um dem Autor Fehler Berichte, Vorschläge oder sonst was zu machen, besucht
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

