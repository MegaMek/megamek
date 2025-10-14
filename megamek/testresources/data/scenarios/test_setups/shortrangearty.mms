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
name: Short Range Arty Fire
planet: None
description: Test setup for arty fire range less than 17
map: unofficial/Lucho/50x50  Lagomortis.board

options:
  on:
    - friendly_fire
  off:
    - check_victory


factions:
  - name: Test Player

    units:

      - fullname: Mobile Long Tom Artillery LT-MOB-25
        at: [ 26, 30 ]
        facing: 0

      - fullname: Bulldog Medium Tank
        at: [ 20, 27 ]

      - fullname: Bulldog Medium Tank
        at: [ 27, 24 ]

      - fullname: Bulldog Medium Tank
        at: [ 12, 12 ]

      - fullname: Bulldog Medium Tank
        at: [ 16, 11 ]

      - fullname: Bulldog Medium Tank
        at: [ 18, 25 ]

      - fullname: Bulldog Medium Tank
        at: [ 15, 18 ]
