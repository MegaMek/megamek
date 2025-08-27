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
name: Submerged Arty Fire
planet: None
description: Test setup for arty fire from a submarine
map: unofficial/Cakefish/General/40x40 Arid Boarding Call.board

options:
  on:
    - friendly_fire
  off:
    - check_victory


factions:
  - name: Test Player

    units:

      - fullname: Test Cruise Missile Sub
        at: [ 22, 37 ]
        elevation: -2

      - fullname: New Arrow Sub
        at: [ 24, 37 ]
        elevation: -2

      - fullname: Test Cruise Missile Sub
        at: [ 21, 36 ]
        elevation: 0

      - fullname: New Arrow Sub
        at: [ 23, 36 ]
        elevation: 0

