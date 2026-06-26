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
name: Test Setup for Conventional Infantry
planet: None
description: Various conventional infantry on a board with varied terrain for testing digging in etc
map:
  - file: unofficial/Cakefish/General/50x50 Grass QRF Airbase.board

options:
  on:
    - friendly_fire
    - tacops_dig_in
    # apparently "hitting the deck" doesnt exist yet
  off:
    - check_victory

factions:
- name: Player

  units:
  - fullname: Foot Platoon (MG)
    at: [ 10, 12 ]

  - fullname: Mechanized Hover Platoon (Rifle)
    at: [ 17, 26 ]

  - fullname: Beast Infantry (Camel) (Auto-Rifle)
    at: [ 21, 20 ]

  - fullname: Jump Platoon (Flamer)
    at: [ 29, 23 ]

  - fullname: Motorized Squad (Rifle)
    at: [ 36, 18 ]

  - fullname: Clan Field Gun Point (Tracked AC2 Basic)
    at: [ 35, 24 ]

  - fullname: Field Artillery (Thumper)
    at: [ 20, 22 ]
