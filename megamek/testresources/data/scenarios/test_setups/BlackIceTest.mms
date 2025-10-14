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
name: Black Ice Tests
planet: None
description: Units on a map with briges and roads in black ice conditions
map: testiceonwater.board

options:
  on: black_ice
  off:
    - check_victory


planetaryconditions:
  temperature: -40
  weather: ice storm

factions:
  - name: Test Player

    units:
      - fullname: Locust LCT-1M
        crew:
          piloting: 0

      - fullname: Locust LCT-1M
        crew:
          piloting: 0
