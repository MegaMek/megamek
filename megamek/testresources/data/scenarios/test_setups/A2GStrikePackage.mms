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
name: A2G Strike Package (from save)
description: >
  Converted from the A2G.sav.gz test save (2026-08-13): the force mix of the live air-to-ground test
  games. CASPAR flies a bombed-up Chippewa (10 HE) and Cheetah (5 HE) against a Princess Akuma and
  Albatross. The save used a generated 50x50 map; a fixed flat board stands in so runs are
  reproducible. Crews carry the save names and skills - the piloting-5/6 pilots are part of the test,
  since risk pricing scales with them.

map:
  boards:
    - file: unofficial/Jakes Map Pack/55x55 Abysmal Flats.board

options:
  on:
    - aero_ground_move
  off:
    - check_victory
    - stratops_capital_fighter
    # Pinned OFF: inherited simultaneous phases trip a BotClient guard (ignoreSimTurn) that drops
    # every bot turn after its first each phase - a two-fighter bot fires ONE unit per firing phase,
    # which silently zeroed all dive bombing across ~50 headless benchmark games.
    - simultaneous_targeting
    - simultaneous_firing
    - simultaneous_physical
    - simultaneous_deployment

factions:
  - name: Observer

  - name: Strike Flight
    bot:
      ai: CASPAR

    units:
      - fullname: Chippewa CHP-W5b
        at: [ 28, 52 ]
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
        at: [ 34, 52 ]
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

  - name: Ground Lance
    bot:
      ai: PRINCESS

    units:
      - fullname: Akuma AKU-1XJ
        at: [ 26, 22 ]
        facing: 3
        crew:
          name: Magda Krebs
          gunnery: 3
          piloting: 6

      - fullname: Albatross ALB-4U
        at: [ 32, 24 ]
        facing: 3
        crew:
          name: Militsa Nyukhalov
          gunnery: 4
          piloting: 5
