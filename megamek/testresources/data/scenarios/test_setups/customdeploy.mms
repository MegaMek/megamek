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
name: Custom deploy zones
planet: None
description: A few units on a small map with Princess deploying to custom zones at start of game and in round 2
map: buildingsnobasement/dropport2.board

options:
  off:
    - check_victory

factions:
- name: Test Player
  deploy:
    area:
      circle:
        center: [ 14, 10 ]
        radius: 4
  units:
  - fullname: Locust LCT-1M
  - fullname: Hunchback HBK-4G
    deploymentround: 2

- name: PrincessTest
  deploy:
    area:
      circle:
        center: [ 4, 10 ]
        radius: 4
  units:
  - fullname: Charger CGR-1A1
  - fullname: Atlas AS7-D
    deploymentround: 2


