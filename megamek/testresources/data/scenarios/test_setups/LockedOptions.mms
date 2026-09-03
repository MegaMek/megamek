# MegaMek Data (C) 2026 by The MegaMek Team is licensed under CC BY-NC-SA 4.0.
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
name: Locked Options Test
description: >
  Test fixture. A scenario that switches objectives on and locks that choice, plus the turn limit, so a
  player cannot change the mission at load - while every other option stays theirs to set.

map: qrf_airbase_50x50.board

# V2 scenarios fix every option unless told otherwise, and a fixed scenario shows no options dialog
# at all - so locked: only means something when the dialog opens. This is the pairing that lets a
# player set the rest while the mission stays as written.
FixedGameOptions: false

options:
  on:
    - use_objectives
    - use_game_turn_limit
  locked:
    - use_objectives
    - use_game_turn_limit

factions:
  - name: Defenders
    units:
      - fullname: Cheetah F-11
        at: [ 10, 40 ]
        facing: 0
        altitude: 5
        velocity: 1

  - name: Raiders
    units:
      - fullname: Cheetah F-11
        at: [ 40, 10 ]
        facing: 3
        altitude: 5
        velocity: 1
