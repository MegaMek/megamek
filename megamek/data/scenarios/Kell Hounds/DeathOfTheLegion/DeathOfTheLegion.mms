#
#  Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
#
#  This file is part of MegaMek.
#
#  MegaMek is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  MegaMek is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
#
# Based on Death of the Legion, FASA's 01652 "Kell Hounds" sourcebook
MMSVersion: 2
name: Death of the Legion
planet: Mankova
description: >
  During a raid on Mankova by Gorman Toth and his pirate unit known as the Legion of Honor, Toth learned that
  a Star League depot had been found during strip mining. Thinking he had tricked the Kell Hounds into
  taking action elsewhere he went straight for it.

map:
  cols: 2
  boards:
    - file: Map Set 2/16x17 River Valley.board
    - file: Map Set 2/16x17 BattleTech.board
      modify: rotate

factions:

- name: Mek Company, Kell Hounds
  camo: Mercs/Kell Hounds.jpg
  deploy: W

  units:
    - fullname: Thunderbolt TDR-5S
      id: 101
      force: Command Lance|1
      crew:
        name: Lt. Col. Patrick M. Kell
        portrait: Male/MekWarrior/MW_M_27.png
        gunnery: 2
        piloting: 3

    - fullname: Orion ON1-K
      id: 102
      force: Command Lance|1
      crew:
        name: Lt. Anne Finn
        portrait: Female/MekWarrior/MW_F_41.png
        gunnery: 3
        piloting: 4

    - fullname: Marauder MAD-3R
      id: 103
      force: Command Lance|1
      crew:
        name: Sgt. Clarence Wilson
        portrait: Male/MekWarrior/MW_M_103.png
        callsign: Cat
        gunnery: 2
        piloting: 3

    - fullname: Crusader CRD-3R
      id: 104
      force: Command Lance|1
      crew:
        name: Bethany Connor
        portrait: Female/MekWarrior/MW_F_48.png
        gunnery: 4
        piloting: 5

    - fullname: Wolverine WVR-6R
      id: 105
      force: Assault Lance|2
      crew:
        name: Maj. Salome Ward
        portrait: Female/MekWarrior/MW_F_46.png
        gunnery: 2
        piloting: 3

    - fullname: Catapult CPLT-C1
      id: 106
      force: Assault Lance|2
      crew:
        name: Lt. Mike Fitzhugh
        portrait: Male/MekWarrior/MW_M_88.png
        gunnery: 3
        piloting: 4

    - fullname: Trebuchet TBT-5N
      id: 107
      force: Assault Lance|2
      crew:
        name: Sgt. Diane McWilliams
        portrait: Female/MekWarrior/MW_F_85.png
        gunnery: 3
        piloting: 4

    - fullname: Rifleman RFL-3N
      id: 108
      force: Assault Lance|2
      crew:
        name: Mary Lasker
        portrait: Female/MekWarrior/MW_F_5.png
        gunnery: 4
        piloting: 5

    - fullname: Valkyrie VLK-QA
      id: 109
      force: Scout Lance|3
      crew:
        name: Cpt. Daniel W. Allard
        portrait: Male/MekWarrior/MW_M_37.png
        gunnery: 3
        piloting: 4

    - fullname: Commando COM-2D
      id: 110
      force: Scout Lance|3
      crew:
        name: Lt. Austin Brand
        portrait: Male/MekWarrior/MW_M_5.png
        gunnery: 3
        piloting: 4

    - fullname: Wasp WSP-1A
      id: 111
      force: Scout Lance|3
      crew:
        name: Sgt. Margaret Lang
        callsign: Meg
        portrait: Female/MekWarrior/MW_F_1.png
        gunnery: 4
        piloting: 5

    - fullname: Jenner JR7-D
      id: 112
      force: Scout Lance|3
      crew:
        name: Eddie Baker
        portrait: Male/MekWarrior/MW_M_65.png
        gunnery: 4
        piloting: 5

# OpFor
- name: Legion of Honor
  camo: Pirates/Tortuga Fusiliers.jpg
  deploy: S

  units:
    - fullname: Marauder MAD-3R
      id: 201
      force: Leader Lance|11
      at: [ 27, 13 ]
      facing: 5
      remaining:
        armor:
          LA: 15
      crits:
        LA: 4
      crew:
        name: Gorman Toth
        gunnery: 4
        piloting: 3

    - fullname: JagerMech JM6-S
      id: 202
      at: [ 28, 12 ]
      facing: 4
      force: Leader Lance|11
      remaining:
        armor:
          HD: 5
      crew:
        name: Maj. Adolf Rillan
        gunnery: 4
        piloting: 4

    - fullname: Centurion CN9-A
      id: 203
      at: [ 25, 12 ]
      facing: 5
      force: Leader Lance|11
      crew:
        name: Joe Toomb
        callsign: Blackjack
        gunnery: 4
        piloting: 5

    - fullname: Assassin ASN-21
      id: 204
      facing: 5
      at: [ 24, 11 ]
      force: Leader Lance|11
      crew:
        name: Peter Manheim
        callsign: Slippery Pete
        gunnery: 4
        piloting: 4

    - fullname: Orion ON1-K
      id: 205
      facing: 4
      at: [ 29, 12 ]
      force: Attack Lance|12
      crew:
        name: Dan Glory
        gunnery: 4
        piloting: 4

    - fullname: Rifleman RFL-3N
      id: 206
      at: [ 30, 11 ]
      facing: 4
      force: Attack Lance|12
      remaining:
        armor:
          CT: 12
      crits:
        RA: 3
      crew:
        name: Sandra Fitzsimmons
        gunnery: 5
        piloting: 4

    - fullname: Scorpion SCP-1N
      id: 207
      at: [ 31, 12 ]
      facing: 5
      force: Attack Lance|12
      crew:
        name: Marcus Worrus
        gunnery: 4
        piloting: 4

    - fullname: Firestarter FS9-H
      id: 208
      at: [ 32, 12 ]
      facing: 5
      force: Attack Lance|12
      crew:
        name: Zeke Smuthers
        callsign: Zippo
        gunnery: 3
        piloting: 5

    - fullname: Valkyrie VLK-QA
      id: 209
      at: [ 23, 11 ]
      facing: 5
      force: Probe Lance|13
      crew:
        name: Roxanne Devers
        piloting: 4
        gunnery: 4

    - fullname: Spider SDR-5V
      id: 210
      at: [ 21, 10 ]
      facing: 5
      force: Probe Lance|13
      crew:
        name: Lancelot Smith
        gunnery: 5
        piloting: 4

    - fullname: Stinger STG-3R
      id: 211
      at: [ 20, 9 ]
      facing: 4
      force: Probe Lance|13
      crits:
        LA: 5
        RA: 6
      crew:
        name: Dorothy Gail
        gunnery: 5
        piloting: 5

    - fullname: Locust LCT-1V
      id: 212
      at: [ 19, 10 ]
      facing: 4
      force: Probe Lance|13
      crew:
        name: Sam Norgales
        gunnery: 4
        piloting: 4


messages:
  - header: Situation
    text: |
      # Situation
      ## Mankova, Foredam District
      ## Free Worlds League
      ## November 3017
      
      During a raid on Mankova led by Gorman Toth and his pirate band, the Legion of Honor, Toth discovered 
      that a Star League depot had been unearthed during strip-mining operations in the Foredam District. 
      Toth had been plundering the planet for nearly a week, having deceived the Kell Hounds into chasing 
      another pirate, Hassin Hys. Believing he had successfully evaded the Hounds, Toth made his way to the 
      remote mining district to seize the depot and its contents. Unbeknownst to him, he had walked right 
      into a carefully orchestrated trap laid by Lieutenant Colonel Patrick Kell, who had fabricated the story 
      about the Star League depot.
  
      As Toth's Legion of Honor advanced toward Foredam, Colonel Kell detached his AeroSpace company and 
      Jump Infantry Company to cut off all potential escape routes. When Major Salome Ward and her Relentless 
      Wolves engaged Toth's Legion, they had no options but to confront the Kell Hounds in direct battle.
      
      *This scenario is based on "Death of the Legion", published in the "Kell Hounds" sourcebook, FASA 01652.*
    image: deathlegion_splash.png
    trigger:
      type: gamestart

  - header: Attacker's Task
    text: |
      ## Attacker's Task
      
      In this scenario, it is your task to deal with the pirate rabble of the so-called Legion of Honor. Destroy,
      cripple or chase off the enemy.
    trigger:
      type: and
      triggers:
        - type: phasestart
          phase: deployment
        - type: round
          round: 0

  - header: Defeat
    text: |
      ## Defeat
      
      The pirates sadly proved to be hardier than Patrick Martin Kell had thought. It will take the Kell Hounds
      quite some time to recover from today's losses.
      
      It's a good thing the pirates won't be able to resupply at a Star League depot on this planet.
    image: deathlegion_splash.png
    trigger:
      type: activeunits
      modify: [ atend, once ]
      units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
      count: 0

  - header: Victory
    text: |
      ## Victory!

      Congratulations! With your leadership, the Kell Hounds wiped the pirates from the face of the 
      planet at almost no losses to themselves.  
      
      Let the opponents tremble when the Kell Hounds arrive.
    image: deathlegion_splash.png
    trigger:
      type: and
      modify: [ atend, once ]
      triggers:
        - type: activeunits
          units: [ 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212 ]
          count: 0
        - type: killedunits
          units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
          atmost: 1

  - header: Victory
    text: |
      ## Victory!
      
      Congratulations! The Kell Hounds have secured the battlefield. While every lost MekWarrior hurts and every lost
      Mek is expensive to replace, this victory came at not too great a cost.
      
      The pirates, as far as they survived will think of this day for a long time. Time to 
      collect the salvage.
    image: deathlegion_splash.png
    trigger:
      type: and
      modify: [ atend, once ]
      triggers:
        - type: activeunits
          units: [ 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212 ]
          count: 0
        - type: killedunits
          units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
          atleast: 2
          atmost: 4

  - header: Victory
    text: |
      ## Victory!
      
      The Legion of Honor is no more. But the Kell Hounds are limping off the battlefield. Let's hope that
      some of the wreckage can be salvaged.
    image: deathlegion_splash.png
    trigger:
      type: and
      modify: [ atend, once ]
      triggers:
        - type: activeunits
          units: [ 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212 ]
          count: 0
        - type: killedunits
          units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
          atleast: 5
          atmost: 8

  - header: Victory
    text: |
      ## Victory!

      The pirates exacted a heavy toll on the Kell Hounds today. It can only be hoped that the battlefield
      salvage and the contract payment can make up for the sustained damage.
    image: deathlegion_splash.png
    trigger:
      type: and
      modify: [ atend, once ]
      triggers:
        - type: activeunits
          units: [ 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212 ]
          count: 0
        - type: killedunits
          units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
          atleast: 9

  - header: Pilot Message
    text: |
      *Sgt. Clarence Wilson:* Sir, reporting the pirate's leader lance is gone. This will surely demoralize them.
    image: portraits/Male/MekWarrior/MW_M_103.png
    trigger:
      type: and
      triggers:
        - type: activeunits
          units: [ 103 ]
        - type: battlefieldcontrol
          modify: not
        - type: activeunits
          modify: once
          units: [ 201, 202, 203, 204 ]
          count: 0

  - header: Pilot Message
    text: |
      *Sgt. Clarence Wilson:* Sir, Jump Infantry is reporting several of the pirates who've fled have been caught and captured
      or destroyed.
    image: portraits/Male/MekWarrior/MW_M_103.png
    trigger:
      type: and
      triggers:
        - type: activeunits
          units: [ 103 ]
        - type: battlefieldcontrol
          modify: not
        - type: fledunits
          modify: once
          units: [ 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212 ]
          atleast: 3

  - header: Pilot Message
    text: |
      *Lt. Mike Fitzhugh:* One pirate down.
    image: portraits/Male/MekWarrior/MW_M_88.png
    trigger:
      type: and
      triggers:
        - type: activeunits
          units: [ 106 ]
        - type: battlefieldcontrol
          modify: not
        - type: activeunits
          modify: once
          units: [ 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212 ]
          count: 11

end:
  - trigger:
      type: battlefieldcontrol



