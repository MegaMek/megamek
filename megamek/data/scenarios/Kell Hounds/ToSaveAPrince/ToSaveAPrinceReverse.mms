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
# Based on Battlecorps Scenario 3013, To Save a Prince, originally published in FASA's "Kell Hounds" sourcebook
MMSVersion: 2
name: To Save a Prince [Kell Hounds]
planet: Mallory's World
description: |
  Prince Ian Davion commands the Fourth Davion Guards in an attempt to repel the Combine invasion on
  Mallory's World. After Kurita successes against the planetary defenders, the prince summoned a Kell
  Hounds Regiment.
  
  *Goal: Playing as the Kell Hounds force, prevent Zakahashi's Zombies from breaking through your lines.*

map:
  boardrows: 2
  boards:
    - file: Map Set 5/16x17 Open Terrain 1.board
      modify: rotate
    - file: Map Set 5/16x17 Open Terrain 1.board
  postprocess:
    - type: settheme
      theme: desert

planetaryconditions:
  temperature: 70

factions:

  - name: Kell Hounds, Second Battalion
    camo: Mercs/Kell Hounds.jpg
    deploy:
      edge: S

    victory:
      - trigger:
          type: fledunits
          modify: atend
          units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
          atmost: 5

    units:
      - include: ToSaveAPrince_units_kell.mmu

  # OPFOR
  - name: Zakahashi's Zombies
    camo: Draconis Combine/Dieron Regulars/Dieron Regulars.jpg
    deploy: N

    bot:
      # try to get away
      selfpreservation: 8
      fallshame: 8
      hyperaggression: 4
      herdmentality: 1
      bravery: 3
      # Princess respects the edge she is set to flee from
      fleeto: south
      flee: true

#    fleefrom:
#      border: south

    victory:
      - trigger:
          type: fledunits
          units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
          atleast: 6

    units:
      - include: ToSaveAPrince_units_zombies.mmu

messages:
  - header: Situation
    text: |
      # Situation
      ## Mallory's World, Desolate Pass
      ## Federated Suns
      ## October 3013

      In a wave of assaults by House Kurita forces on Mallory's World, the Second Sword of Light
      Regiment, backed by the Twenty-fourth Dieron Regulars, landed and clashed with the defending Seventeenth
      Avalon Hussars. Prince Ian Davion, leading the Fourth Davion Guards, swiftly responded to the Combine's
      invasion.

      Despite early victories by Davion's forces, the Kurita units inflicted heavy damage on the
      Seventeenth Avalon Hussars, exposing the Fourth Guards to significant danger. In response, Prince Ian
      called for reinforcements from the Kell Hounds Regiment, stationed ten days away on Mara.

      By the time the Kell Hounds reached the battlefield, the situation for the defenders had grown dire. The
      Twenty-fourth Dieron Regulars had severed the Fourth Davion Guards' lines of communication and driven them
      into the desert, the Second Sword of Light Regiment in pursuit. At last, at Desolate Pass, Prince Ian turned
      his battered Atlas to confront the Warhammer of Yorinaga Kurita, commander of the Second Sword.

      In a desperate last stand Prince Ian Davion bought his regiment precious time to retreat, but did not
      survive the fight. Right before other Kurita forces could seize his body, the long-awaited Kell Hounds arrived.

      *This scenario is based on Battlecorps Scenario 3013, "To Save a Prince", originally published in
      the "Kell Hounds" sourcebook, FASA 01652.*
    image: tosaveaprince_splash.png
    trigger:
      type: gamestart

  - header: Defender's Task
    text: |
      ## Defender's Task

      In this scenario, it is your task to hold the Kell Hounds defense line and prevent the Zombies units from
      breaking through. Allow less than half their Meks to exit off the southern map edge until the end of round 15.

      The temperature in this desert area is at 70°C, adding heat to all Meks.
    image: tosaveaprince_map.png
    trigger:
      type: and
      triggers:
        - type: phasestart
          phase: deployment
        - type: round
          round: 0

  - header: One Unit Safe
    text: Congratulations, one of your Meks has successfully broken through!
    trigger:
      type: fledunits
      modify: once
      units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
      atleast: 1
      atmost: 2

  - header: Another Unit Safe
    text: Three of your Meks have broken through!
    trigger:
      type: fledunits
      modify: once
      units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
      count: 3

  - header:  Victory
    text: |
      ## Victory

      The Kell Hounds managed to drive off the Second Sword of Light, enabling the Fourth Davion
      Guards to step in reclaim Prince Ian's body.

      Yorinaga Kurita is not pleased.

    image: tosaveaprince_splash.png
    trigger:
      type: fledunits
      modify: [ atend, once ]
      units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
      atmost: 5

  - header: Defeat
    text: |
      ## Defeat

      The Second Sword of Light successfully broke through the line of the Davion defenses. They will not
      recover their Prince's body. This will be a heavy blow to their morale.

    image: tosaveaprince_splash.png
    trigger:
      type: fledunits
      modify: [ atend, once ]
      units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
      atleast: 6

end:
  - trigger:
      type: battlefieldcontrol

  - trigger:
      type: killedunits
      units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
      # can't get through with half the force anymore when 7 are killed
      atleast: 7

  - trigger:
      type: roundend
      round: 15
