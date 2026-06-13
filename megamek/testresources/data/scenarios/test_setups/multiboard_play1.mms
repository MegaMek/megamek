# MegaMek Data (C) 2025 by The MegaMek Team is licensed under CC BY-NC-SA 4.0.
# To view a copy of this license, visit https://creativecommons.org/licenses/by-nc-sa/4.0/
#
# NOTICE: The MegaMek organization is a non-profit group of volunteers
# creating free software for the BattleTech community.
#
# MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
# of The Topps Company, Inc. All Rights Reserved.
#
# Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
# InMediaRes Productions, LLC.
#
# MechWarrior Copyright Microsoft Corporation. MegaMek Data was created under
# Microsoft's "Game Content Usage Rules"
# <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
# affiliated with Microsoft.
MMSVersion: 2
name: Multiboard Playtest Scenario 1
planet: None
description: Your lone Grasshopper is attacked by two Grasshoppers. You have a Sai aerospace fighter to support your 
  mek.
map:
  - file: Beginner Box/16x17 Grassland 1.board
    name: Grassland
    id: 3

  - type: sky
    width: 35
    height: 20
    name: Sky
    embed:
      - at: [ 31, 11 ]
        id: 3
    id: 5

options:
  on:
  off:
    - check_victory

factions:
- name: Human

  deploy:
    area:
      terrain:
        type: woods
        maxdistance: 1
        boards: 3

  units:
    - fullname: Grasshopper GHR-5N
      board: 3

    - fullname: Sai S-8
      at: [ 5, 6 ]
      board: 5
      facing: 2
      altitude: 7

    - fullname: Sai S-8
      at: [ 12, 12 ]
      board: 5
      facing: 2
      altitude: 4


- name: Princess
  deploy: N
  units:
    - fullname: Grasshopper GHR-5N
      board: 3

    - fullname: Grasshopper GHR-5N
      board: 3
