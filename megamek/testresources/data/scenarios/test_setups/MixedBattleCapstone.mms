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
name: Mixed Battle Capstone
description: >
  The final validation: combined arms on real terrain. Both sides field fighters AND meks on the
  citadel board (buildings, roads - from the live-game map rotation). Tests every doctrine layer at
  once: A2A engagement over terrain (where the LosEffects ground-map wart lets buildings block fire
  between aircraft - a known engine limitation, part of the test), A2G attack runs against meks that
  shoot back, ground-force posture coexisting with flight posture, and the velocity/climb discipline
  with both air and ground threats present.

map:
  boards:
    - file: buildingsnobasement/citadel.board

options:
  on:
    - aero_ground_move
  off:
    - check_victory
    - stratops_capital_fighter
    # Pinned OFF: inherited simultaneous phases trip the BotClient ignoreSimTurn guard, dropping
    # every bot turn after its first each phase.
    - simultaneous_targeting
    - simultaneous_firing
    - simultaneous_physical
    - simultaneous_deployment

factions:
  - name: Observer

  - name: Strike Force
    bot:
      ai: CASPAR

    units:
      - fullname: Chippewa CHP-W5b
        at: [ 12, 48 ]
        facing: 0
        altitude: 5
        velocity: 2
        bombs:
          external:
            HE: 10
        crew:
          name: Uni Tamragouri
          gunnery: 3
          piloting: 5

      - fullname: Cheetah F-10
        at: [ 20, 48 ]
        facing: 0
        altitude: 5
        velocity: 2
        bombs:
          external:
            HE: 5
        crew:
          name: Dylan Fountain
          gunnery: 3
          piloting: 6

      - fullname: Marauder MAD-3R
        at: [ 14, 44 ]
        facing: 0
        crew:
          name: Petra Vollan
          gunnery: 3
          piloting: 4

      - fullname: Archer ARC-2R
        at: [ 20, 44 ]
        facing: 0
        crew:
          name: Denny Okafor
          gunnery: 4
          piloting: 5

  - name: Defence Force
    bot:
      ai: PRINCESS

    units:
      - fullname: Cheetah F-10
        at: [ 28, 4 ]
        facing: 3
        altitude: 5
        velocity: 2
        crew:
          name: Iqbal Langah
          gunnery: 4
          piloting: 4

      - fullname: Akuma AKU-1XJ
        at: [ 24, 8 ]
        facing: 3
        crew:
          name: Magda Krebs
          gunnery: 3
          piloting: 6

      - fullname: Albatross ALB-4U
        at: [ 30, 8 ]
        facing: 3
        crew:
          name: Militsa Nyukhalov
          gunnery: 4
          piloting: 5

      - fullname: Atlas AS7-D
        at: [ 27, 6 ]
        facing: 3
        crew:
          name: Bram Odendaal
          gunnery: 3
          piloting: 5
