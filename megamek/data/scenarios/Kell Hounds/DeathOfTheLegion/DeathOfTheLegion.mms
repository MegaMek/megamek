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
description: |
  During a raid on Mankova by Gorman Toth and his pirate unit known as the Legion of Honor, Toth learned that
  a Star League depot had been found during strip mining. Thinking he had tricked the Kell Hounds into
  taking action elsewhere he went straight for it.
  
  *Goal: Playing as the Kell Hounds force, destroy the Pirate forces.*

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
      include: DeathOfTheLegion_units_kell.mmu

  # OpFor
  - name: Legion of Honor
    camo: Pirates/Tortuga Fusiliers.jpg
    deploy: S

    units:
      include: DeathOfTheLegion_units_legion.mmu

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
      *Sgt. Clarence Wilson:* Sir, Jump Infantry is reporting several of the pirates who've fled have been caught 
      and captured or destroyed.
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



