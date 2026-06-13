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
name: A2G and G2A from the atmo map
planet: None
description:
  For playing around with A2G and G2A, incl bombing and strafing from the atmo map
map:
  - type: sky
    width: 35
    height: 20
    name: Sky
    embed:
      - at: [ 24, 15 ]
        id: 5
    id: 2

  - file: unofficial/Cakefish/General/50x50 Grass QRF Airbase.board
    name: Airport
    id: 5


options:
  on:
    - friendly_fire
  off:
    - aero_ground_move
    - check_victory
    - stratops_ecm

factions:
  - name: P1

    units:
        - fullname: Atlas AS7-D
          at: [ 13,10 ]
          board: 5
          facing: 2

        - fullname: Cheetah IIC
          at: [ 21, 14 ]
          board: 2
          facing: 2
          altitude: 5
          bombs:
            internal:
              HE: 2
              LG: 2
            external:
              CLUSTER: 1

