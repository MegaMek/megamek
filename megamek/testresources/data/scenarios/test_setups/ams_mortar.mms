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
name: AMS vs Mortars
planet: None
description: AMS may not engage mortars - test setup
map: Beginner Box/16x17 Grassland 1.board

options:
  off:
    - check_victory
   # - auto_ams

factions:
  - name: Test Player

    units:
      #AMS
      - fullname: Atlas AS7-K
        at: [ 9, 7 ]
        facing: 3
      #Mortar and SRM
      - fullname: Minsk 2
        at: [ 9, 14 ]
        crew:
          gunnery: 0

