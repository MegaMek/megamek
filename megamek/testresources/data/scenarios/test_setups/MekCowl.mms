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
name: Test Setup for Mek Cowls
planet: None
description: A few units on a small map to test mek cowl (quirk) functions
map: Beginner Box/16x17 Grassland 1.board

options:
  off:
    - check_victory

factions:
- name: Test Player

  units:
  - fullname: Cyclops CP-10-Z
    at: [ 9, 12 ]
    facing: 0
    crew:
      gunnery: 0

  - fullname: Cyclops CP-10-Z
    at: [ 9, 8 ]
    facing: 3
    crew:
      gunnery: 0

  - fullname: Cyclops CP-10-Z
    at: [ 10, 8 ]
    facing: 3
    crew:
      gunnery: 0

  - fullname: Cyclops CP-10-Z
    at: [ 9, 15 ]
    facing: 0
    crew:
      gunnery: 0

  - fullname: Cyclops CP-10-Z
    at: [ 6, 9 ]
    facing: 3
    crew:
      gunnery: 0
