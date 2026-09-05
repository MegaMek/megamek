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
name: Aerospace Duel (from save)
description: >
  Converted from the aerospace.sav.gz test save (2026-08-13): the 2v2 mixed-weight duel of the live
  air-to-air test games - a Chippewa and Cheetah per side, no bombs, matched deployment altitude. The
  save used a generated 50x50 map; a fixed flat board stands in so runs are reproducible (terrain
  blocks A2A fire over ground maps - the LosEffects altitude wart). Crews carry the save names and
  skills; Dylan Fountain (piloting 6) is the green pilot whose 28% Split-S motivated the odds floor.

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

  - name: North Flight
    bot:
      ai: PRINCESS

    units:
      - fullname: Chippewa CHP-W5b
        at: [ 24, 6 ]
        facing: 3
        altitude: 5
        velocity: 1
        crew:
          name: Mikaela Tobitt
          gunnery: 4
          piloting: 5

      - fullname: Cheetah F-10
        at: [ 32, 6 ]
        facing: 3
        altitude: 5
        velocity: 1
        crew:
          name: Iqbal Langah
          gunnery: 4
          piloting: 4

  - name: South Flight
    bot:
      ai: CASPAR

    units:
      - fullname: Chippewa CHP-W5b
        at: [ 24, 49 ]
        facing: 0
        altitude: 5
        velocity: 1
        crew:
          name: Uni Tamragouri
          gunnery: 3
          piloting: 5

      - fullname: Cheetah F-10
        at: [ 32, 49 ]
        facing: 0
        altitude: 5
        velocity: 1
        crew:
          name: Dylan Fountain
          gunnery: 3
          piloting: 6
