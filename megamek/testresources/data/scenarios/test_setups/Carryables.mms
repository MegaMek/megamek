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
name: Test Setup for Carryables
planet: None
description: A few units on a small map with a few carryable objects for testing; most objects predeployed, some to deploy
map: buildingsnobasement/dropport2.board

options:
  off:
    - check_victory

factions:
- name: Test Player

  units:
  - fullname: Locust LCT-1M
    at: [ 9, 10 ]

  - fullname: Hunchback HBK-4G
    at: [ 10, 10 ]

  - fullname: Charger CGR-1A1
    at: [ 11, 10 ]

  - fullname: Atlas AS7-D
    at: [ 10, 11 ]

  objects:
    - name: Test Paperweight (invulnerable)
      at: [ 14, 11 ]
      weight: 0.02
      status: invulnerable

    - name: Crate (can be damaged)
      weight: 1
      at: [ 3, 11 ]

    - name: This is medium weight (invulnerable)
      weight: 50
      at: [ 2, 2 ]
      status: invulnerable

    - name: This is massive (can be damaged)
      weight: 150
      at: [ 8, 15 ]

    - name: Deploy this (can be damaged)
      weight: 20

    - name: Deploy this 2 (invulnerable)
      weight: 3
      status: invulnerable
