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
name: Test Setup for MMJB
planet: None
description: Units with MMJB and standard JJ
map: Map Pack Savannahs/16x17 River Delta-Drainage Basin 1 (Savannah).board

options:
  off:
    - check_victory
  on:
    - friendly_fire

factions:
- name: Test Player

  units:
  - fullname: Thunder Fox TFT-C3
    # only MMJB
    at: [ 9, 12 ]

  - fullname: Thunder Fox TFT-L8
    # standard JJ
    at: [ 9, 8 ]

  - fullname: Commando COM-MMJB
    # both JJ and MMJB
    # the mtf file must be moved so that it's part of the cache for this to work
    at: [ 10, 11 ]
