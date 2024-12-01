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
# Based on Battlecorps Scenario 3011, Lowering the Boom, originally published in FASA's "Kell Hounds" sourcebook
MMSVersion: 2
name: Lowering the Boom [Kell Hounds]
planet: Castor
description: |
  Lyran intelligence has found illegal atomic weapons on the Marik world of Castor. Katrina Steiner
  has authorized an attack to remove the weapons and provide the Kell Hounds with action.

  *Goal: Playing as the Kell Hounds force, catch the Marik forces before they can flee.*

map:
  boardrows: 2
  boards:
    - file: Map Set 2/16x17 BattleTech.board
    - file: Map Set 2/16x17 Lake Area.board
      modify: rotate
  postprocess:
    - type: convertterrain
      terrain: woods
      newlevel: 2
    - type: convertterrain
      terrain: water
      newlevel: 3

options:
  file: gameoptions3025.xml

factions:

  - name: Kell Hounds, First Battalion
    camo: MERC - 1st Kell Hounds.gif
    deploy:
      edge: S

    victory:
      - modify: onlyatend
        trigger:
          type: fledunits
          modify: atend
          units: [ 101, 102, 103, 104, 105, 106 ]
          atmost: 2

    units:
      include: LoweringTheBoom_units_kell.mmu

  # OPFOR ----------------------------
  - name: Thirtieth Marik Militia
    camo: Free Worlds League/Marik Militia/Marik Militia.jpg
    deploy: N

    bot:
      # try to get away
      selfpreservation: 8
      fallshame: 8
      hyperaggression: 4
      herdmentality: 1
      bravery: 3
      # Princess respects the edge she is set to flee from
      fleeto: north
      flee: true

    victory:
      - trigger:
          type: fledunits
          modify: atend
          units: [ 101, 102, 103, 104, 105, 106 ]
          atleast: 4
        modify: onlyatend

    units:
      include: LoweringTheBoom_units_marik.mmu


messages:
  - header: Situation
    text: |
      # Situation
      ## Castor
      ## Free Worlds League
      ## 7 June 3011

      The idea for a Steiner raid on the Marik world of Castor originated with Cranston Snord, a well-known
      eccentric and one of Katrina Steiner's long-serving mercenary commanders. Snord's sources had informed
      him of a priceless collection of Fabergé Eggs located on Castor, and he already envisioned them as part
      of his private museum on Clinton.

      Upon learning of Snord's plans, Morgan and Patrick Kell saw an opportunity to put their newly formed
      unit to the test in battle. With Lyran intelligence uncovering a cache of illegal atomic weapons on
      Castor, Katrina Steiner sanctioned the raid. The operation aimed to eliminate the weapons, secure the
      eggs for Snord, and provide combat experience for the Kell Hounds.
      
      Colonel Nage recognized the battle was lost and retreated with his command company, aiming to reach the 
      atomic weapons and unleash them on the mercenaries. Colonel Kell dispatched Salome Ward and her 
      Relentless Wolves to intercept him. Ward's company caught up with Nage just as he was nearing the depot. 
      She launched an attack as the Second Battalion's LAMs bombed the depot. 
      
      *This scenario is based on Battlecorps Scenario 3011, "Lowering the Boom", originally published in 
      the "Kell Hounds" sourcebook, FASA 01652.*
    image: loweringboom_splash.png
    trigger:
      type: gamestart

  - header: Defender's Task
    text: |
      ## Defender's Task
      
      In this scenario, it is your task to stop Col. Oliver Nage and his elements of 1st Company, 1st Battalion 
      from reaching the weapons depot which lies beyond the north edge of the map. They will try to reach and
      escape from this edge with as many Meks as possible.
      
      Some of their Meks have already sustained damage.
      
      *This game uses basic game rules.*
    image: loweringboom_map_reverse.png
    trigger:
      type: and
      triggers:
        - type: phasestart
          phase: movement
        - type: round
          round: 1

  - header: One Unit Safe
    text: Congratulations, one of your Meks has safely left the battlefield!
    trigger:
      type: fledunits
      modify: once
      units: [ 101, 102, 103, 104, 105, 106 ]
      count: 1

  - header: Another Unit Safe
    text: Three of your Meks have safely left the battlefield! This game is already considered a draw.
    trigger:
      type: fledunits
      modify: once
      units: [ 101, 102, 103, 104, 105, 106 ]
      atleast: 3

  - header: Decisive Victory
    text: |
      ## Decisive Victory
      
      Not more than one Marik Mek managed to evade the Kell Hounds forces. The Kell Hounds
      have won a decivise victory.
      
      Ward's Wolves inflicted significant losses on the Marik Meks. Witnessing the smoke rising 
      from the depot and his units sustaining severe damage, Nage decided to flee, with the relentless 
      pursuit of the Wolves close behind.
      
    image: loweringboom_splash.png
    trigger:
      type: fledunits
      modify: atend
      units: [ 101, 102, 103, 104, 105, 106 ]
      atmost: 1

  - header: Marginal Victory
    text: |
      ## Marginal Victory
      
      Only two Marik Meks managed to evade the Kell Hounds forces.
      
      Ward's Wolves inflicted significant losses on the Marik Meks. Witnessing the smoke rising 
      from the depot and his units sustaining severe damage, Nage decided to flee, with the relentless 
      pursuit of the Wolves close behind.
    image: loweringboom_splash.png
    trigger:
      type: fledunits
      modify: atend
      units: [ 101, 102, 103, 104, 105, 106 ]
      count: 2

  - header: A Draw!
    text: Three Marik Meks managed to evade the Kell Hounds forces. This result is considered
      a draw.
    
      Both sides suffered significant losses. With the atomic weapons supply
      depot bombarded, Col. Nage decided to withdraw.
    image: loweringboom_splash.png
    trigger:
      type: fledunits
      modify: atend
      units: [ 101, 102, 103, 104, 105, 106 ]
      count: 3

  - header: Marginal Defeat
    text: Four Marik Meks managed to evade the Kell Hounds forces. 
    
      Ward's Wolves failed to inflict significant losses on the Marik Meks. With the atomic weapons supply
      depot bombarded, Col. Nage decided to withdraw.
    image: loweringboom_splash.png
    trigger:
      type: fledunits
      modify: atend
      units: [ 101, 102, 103, 104, 105, 106 ]
      count: 4

  - header: Decisive Defeat
    text: At least five Marik Meks managed to evade the Kell Hounds forces. The opposing FWL
      has won a decivise victory.
    
      Ward's Wolves failed to inflict significant losses on the Marik Meks. With the atomic weapons supply
      depot bombarded, Nage decided to withdraw.
    image: loweringboom_splash.png
    trigger:
      type: fledunits
      modify: atend
      units: [ 101, 102, 103, 104, 105, 106 ]
      atleast: 5

end:
  - trigger:
      type: battlefieldcontrol

