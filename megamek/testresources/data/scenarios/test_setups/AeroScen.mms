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
name: Sky Battle Test
description: Aero Fighter Test Setup on a terrain-less low atmo map

map:
  type: sky
  width: 36
  height: 19

options:
  off:
    - check_victory

factions:
  - name: Epsilon Galaxy Flight
    camo: Clans/Coyote/Epsilon Galaxy.jpg

    units:
      - fullname: Cheetah F-11
#        at: [2, 12]
#        facing: 1
#        altitude: 6
#        velocity: 3
        crew:
          name: Marianne O'Brien
          gunnery: 4
          piloting: 4
          portrait: Female/Aerospace Pilot/ASF_F_2.png

      - fullname: Cheetah F-11
#        at: [3, 17]
#        facing: 1
#        velocity: 2
        crew:
          name: Giulia DeMarco
          gunnery: 3
          piloting: 5
          portrait: Female/Aerospace Pilot/ASF_F_3.png

  - name: OpFor
    camo: Corporations/Star Corps.png
    units:
      - fullname: Cheetah F-11
        at: [ 32, 8 ]
        facing: 4
      - fullname: Cheetah F-11
        at: [ 34, 8 ]
        facing: 4
