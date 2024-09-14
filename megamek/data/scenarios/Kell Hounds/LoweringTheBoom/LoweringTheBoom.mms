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
name: Lowering the Boom
planet: Castor
description: >
  Lyran intelligence has found illegal atomic weapons on the Marik world of Castor. Katrina Steiner
  has authorized an attack to remove the weapons and provide the Kell Hounds with action.

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

factions:
- name: Thirtieth Marik Militia
  camo: Free Worlds League/Marik Militia/Marik Militia.jpg
  deploy: N

  victory:
    - trigger:
        type: fledunits
        modify: atend
        units: [ 101, 102, 103, 104, 105, 106 ]
        atleast: 4
      modify: onlyatend

  units:
  - fullname: Thunderbolt TDR-5S
    id: 101
    at: [ 12, 29 ]
    remaining:
      armor:
        LT: 2
        CT: 15
    crew:
      name: Col. Oliver Nage
      portrait: Male/MekWarrior/MW_M_15.png
      piloting: 5
      gunnery: 5

  - fullname: Griffin GRF-1N
    id: 102
    at: [ 9, 32 ]
    remaining:
      armor:
        LT: 0
        CT: 15
      internal:
        LT: 10
    crew:
      name: Maj. Abraham Morrison
      portrait: Male/MekWarrior/MW_M_13.png
      piloting: 4
      gunnery: 4

  - fullname: Hunchback HBK-4G
    id: 103
    at: [ 16, 29 ]
    remaining:
      armor:
        HD: 5
        RL: 10
    crew:
      name: Lt. Alicia Devon
      piloting: 4
      gunnery: 4

  - fullname: Centurion CN9-A
    id: 104
    at: [ 14, 31 ]
    remaining:
      armor:
        CT: 12
    crew:
      name: Sgt. Jonathan Taylor
      piloting: 4
      gunnery: 4

  - fullname: Hermes II HER-2S
    id: 105
    at: [ 5, 30 ]
    crew:
      name: Samantha Blaustein
      piloting: 4
      gunnery: 4

  - fullname: Javelin JVN-10N
    id: 106
    at: [ 2, 29 ]
    crew:
      name: Deborah Ryan
      piloting: 4
      gunnery: 4

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
    - fullname: Wolverine WVR-6R
      id: 201
      deploymentround: 2
      crew:
        name: Maj. Salome Ward
        piloting: 4
        gunnery: 3

    - fullname: Shadow Hawk SHD-2H
      id: 202
      crew:
        name: Lee Kennedy
        piloting: 4
        gunnery: 4

    - fullname: Dervish DV-6M
      id: 203
      deploymentround: 2
      crew:
        name: Brian Martell
        piloting: 4
        gunnery: 4

    - fullname: Trebuchet TBT-5N
      id: 204
      deploymentround: 2
      crew:
        name: Judith Nesmith
        piloting: 4
        gunnery: 4

    - fullname: Phoenix Hawk PXH-1
      id: 205
      crew:
        name: Nathan Mack
        piloting: 4
        gunnery: 4

    - fullname: Phoenix Hawk PXH-1
      id: 206
      crew:
        name: Stuart O'Grady
        piloting: 4
        gunnery: 4

    - fullname: Jenner JR7-D
      id: 207
      crew:
        name: Sarah Jette
        piloting: 4
        gunnery: 4

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

      In this scenario, it is your task to save Col. Oliver Nage and his elements of 1st Company, 1st Battalion
      from the attacking Kell Hounds. Try to escape by retreating off the north edge of the map with
      as many Meks as possible.

      Be careful! Some of your Meks have already sustained damage.

      *Technical note: you can currently retreat off any edge of the battlefield and it will count for victory.
      If you do this, Princess will be sad.*
    image: loweringboom_map.png
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

  - header: Decisive Defeat
    text: |
      ## Decisive Defeat

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

  - header: Marginal Defeat
    text: |
      ## Marginal Defeat

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

  - header: Decisive Marik Victory
    text: At least five Marik Meks managed to evade the Kell Hounds forces. The FWL
      has won a decivise victory.

      Ward's Wolves failed to inflict significant losses on the Marik Meks. With the atomic weapons supply
      depot bombarded, Nage decided to withdraw.
    image: loweringboom_splash.png
    trigger:
      type: fledunits
      modify: atend
      units: [ 101, 102, 103, 104, 105, 106 ]
      atleast: 5

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

  - header: Marginal Marik Victory
    text: Four Marik Meks managed to evade the Kell Hounds forces. The FWL
      has won a marginal victory.

      Ward's Wolves failed to inflict significant losses on the Marik Meks. With the atomic weapons supply
      depot bombarded, Col. Nage decided to withdraw.
    image: loweringboom_splash.png
    trigger:
      type: fledunits
      modify: atend
      units: [ 101, 102, 103, 104, 105, 106 ]
      count: 4

end:
  - trigger:
      type: battlefieldcontrol
