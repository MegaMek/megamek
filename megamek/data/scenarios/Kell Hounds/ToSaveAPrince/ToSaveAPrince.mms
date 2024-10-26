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
name: To Save a Prince
planet: Mallory's World
description: >
  Prince Ian Davion commands the Fourth Davion Guards in an attempt to repel the Combine invasion on
  Mallory's World. After Kurita successes against the planetary defenders, the prince summoned a Kell
  Hounds Regiment.

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
- name: Zakahashi's Zombies
  camo: Draconis Combine/Dieron Regulars/Dieron Regulars.jpg
  deploy: N

  fleefrom:
    border: south

  victory:
    - trigger:
        type: fledunits
        units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
        atleast: 6

  units:
  - fullname: BattleMaster BLR-1G
    id: 101
    force: 2nd Sword of Light|21||Zakahashi's Zombies|22||Assault Lance|23
    crew:
      name: Tai-i Tendoru Zakahashi
      piloting: 3
      gunnery: 3

  - fullname: Marauder MAD-3R
    id: 102
    force: 2nd Sword of Light|21||Zakahashi's Zombies|22||Assault Lance|23
    crew:
      name: Anson McMurphy
      piloting: 4
      gunnery: 4

  - fullname: Grasshopper GHR-5H
    id: 103
    force: 2nd Sword of Light|21||Zakahashi's Zombies|22||Assault Lance|23
    crew:
      name: Franco Jones
      callsign: Frito
      piloting: 4
      gunnery: 5

  - fullname: Ostsol OTL-4D
    id: 104
    force: 2nd Sword of Light|21||Zakahashi's Zombies|22||Assault Lance|23
    crew:
      name: Art Shrett
      piloting: 3
      gunnery: 5

  - fullname: Orion ON1-K
    id: 105
    force: 2nd Sword of Light|21||Zakahashi's Zombies|22||Support Lance|24
    crew:
      name: Chu-i Susie Elgin
      piloting: 3
      gunnery: 4

  - fullname: Rifleman RFL-3N
    id: 106
    force: 2nd Sword of Light|21||Zakahashi's Zombies|22||Support Lance|24
    remaining:
      armor:
        RTR: 0
    crits:
      LA: 5
    crew:
      name: Cletus Palmer
      piloting: 4
      gunnery: 3

  - fullname: Dervish DV-6M
    id: 107
    force: 2nd Sword of Light|21||Zakahashi's Zombies|22||Support Lance|24
    remaining:
      armor:
        CT: 10
    crits:
      CT: 11
      LL: [ 5, 6 ]
      RL: [ 5, 6 ]
    crew:
      name: Tom Meyer
      callsign: Hands
      piloting: 3
      gunnery: 5

  - fullname: Enforcer ENF-4R
    id: 108
    force: 2nd Sword of Light|21||Zakahashi's Zombies|22||Support Lance|24
    crew:
      name: Charlie Adams
      piloting: 4
      gunnery: 4

  - fullname: Javelin JVN-10N
    id: 109
    force: 2nd Sword of Light|21||Zakahashi's Zombies|22||Light Lance|25
    crew:
      name: Chu-i Bob Crenshaw
      callsign: Texas
      piloting: 4
      gunnery: 5

  - fullname: Stinger STG-3R
    id: 110
    force: 2nd Sword of Light|21||Zakahashi's Zombies|22||Light Lance|25
    crew:
      name: John Parthan
      callsign: Wrinkles
      piloting: 5
      gunnery: 3

  - fullname: Locust LCT-1V
    id: 111
    force: 2nd Sword of Light|21||Zakahashi's Zombies|22||Light Lance|25
    crew:
      name: Chuck Stork
      callsign: Bossman
      piloting: 5
      gunnery: 5

  - fullname: Locust LCT-1V
    id: 112
    force: 2nd Sword of Light|21||Zakahashi's Zombies|22||Light Lance|25
    crew:
      name: Jack Brockman
      callsign: Hun Killer
      piloting: 4
      gunnery: 5

# OPFOR -----------------------

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
    - fullname: Crusader CRD-3R
      id: 201
      force: 2nd Mek Battalion|11||1st Company|12||Assault Lance|13
      deploymentround: 2
      crew:
        name: Cpt. Carrol O'Caithan
        piloting: 4
        gunnery: 3

    - fullname: Warhammer WHM-6R
      id: 202
      force: 2nd Mek Battalion|11||1st Company|12||Assault Lance|13
      deploymentround: 2
      crew:
        name: Michael Kiltartan
        piloting: 5
        gunnery: 4

    - fullname: JagerMech JM6-S
      id: 203
      force: 2nd Mek Battalion|11||1st Company|12||Assault Lance|13
      deploymentround: 2
      crew:
        name: Rich O'Hare
        piloting: 5
        gunnery: 4

    - fullname: Catapult CPLT-C1
      id: 204
      force: 2nd Mek Battalion|11||1st Company|12||Assault Lance|13
      deploymentround: 2
      crew:
        name: Hypatia Donahue
        piloting: 4
        gunnery: 3

    - fullname: Clint CLNT-2-3T
      id: 205
      force: 2nd Mek Battalion|11||1st Company|12||Fire Lance|14
      deploymentround: 2
      crew:
        name: Lt. Jane Near
        piloting: 5
        gunnery: 4

    - fullname: Vulcan VL-2T
      id: 206
      force: 2nd Mek Battalion|11||1st Company|12||Fire Lance|14
      deploymentround: 2
      crew:
        name: Julian Goodrich
        piloting: 5
        gunnery: 4

    - fullname: Enforcer ENF-4R
      id: 207
      force: 2nd Mek Battalion|11||1st Company|12||Fire Lance|14
      deploymentround: 2
      crew:
        name: Robert Cross
        piloting: 4
        gunnery: 3

    - fullname: Dervish DV-6M
      id: 208
      force: 2nd Mek Battalion|11||1st Company|12||Fire Lance|14
      deploymentround: 2
      crew:
        name: Robin Buckley
        piloting: 5
        gunnery: 4

    - fullname: Jenner JR7-D
      id: 209
      force: 2nd Mek Battalion|11||1st Company|12||Recon Lance|15
      crew:
        name: Lt. Jim Morrel
        piloting: 4
        gunnery: 3

    - fullname: Valkyrie VLK-QA
      id: 210
      force: 2nd Mek Battalion|11||1st Company|12||Recon Lance|15
      crew:
        name: Kevin Connor
        piloting: 5
        gunnery: 4

    - fullname: UrbanMech UM-R60
      id: 211
      force: 2nd Mek Battalion|11||1st Company|12||Recon Lance|15
      crew:
        name: Estyn Burns
        piloting: 5
        gunnery: 4

    - fullname: Javelin JVN-10N
      id: 212
      force: 2nd Mek Battalion|11||1st Company|12||Recon Lance|15
      crew:
        name: Erin Finney
        piloting: 4
        gunnery: 3

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

  - header: Attacker's Task
    text: |
      ## Attacker's Task

      In this scenario, it is your task to break through the Kell Hounds defense line and exit at least half your
      Meks off the southern map edge by the end of round 15.

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

  - header:  Defeat
    text: |
      ## Defeat

      The Kell Hounds managed to drive off the Second Sword of Light, enabling the Fourth Davion
      Guards to step in reclaim Prince Ian's body.

      Yorinaga Kurita is not pleased.

    image: tosaveaprince_splash.png
    trigger:
      type: fledunits
      modify: [ atend, once ]
      units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
      atmost: 5

  - header: Victory
    text: |
      ## Victory

      The Second Sword of Light successfully broke through the line of the Davion defenses. They will not
      recover their Prince's body. This will be a heavy blow to their morale.

      Yorinaga Kurita commends your performance by not being displeased.

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
