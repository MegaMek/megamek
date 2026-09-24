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
name: Air Ground From Save
description: >
  The user's live mixed-battle test setup (Air-Ground.sav.gz), converted: two flights of two
  fighters with full bomb racks over a four-mek ground lance each, on the 50x50 Arid CarvedLands
  board from the live rotation. The standing arena for air-cover doctrine work - both sides carry
  enough ordnance that whoever's bombers deliver first buys the fight, which is exactly what the
  intercept credit exists to contest.

map:
  boards:
    - file: unofficial/Cakefish/General/50x50 Arid CarvedLands.board

options:
  on:
    - aero_ground_move
  off:
    - check_victory
    - stratops_capital_fighter
    # Pinned OFF: inherited simultaneous phases trip bot turn-handling races (see #8712) and
    # spray stale attack packets; the live games that inherited them measured the race, not
    # the doctrine.
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
      - fullname: Hellcat II HCT-214
        at: [ 12, 48 ]
        facing: 0
        altitude: 5
        velocity: 2
        bombs:
          external:
            HE: 10
        crew:
          name: Sentra Takaki
          gunnery: 4
          piloting: 5

      - fullname: Huscarl HSCL-1-O
        at: [ 20, 48 ]
        facing: 0
        altitude: 5
        velocity: 2
        bombs:
          external:
            HE: 15
        crew:
          name: Paultje Aldershoff
          gunnery: 4
          piloting: 5

      - fullname: Phoenix Hawk PXH-3M
        at: [ 14, 44 ]
        facing: 0
        crew:
          name: Fyfa Channing
          gunnery: 4
          piloting: 5

      - fullname: Chameleon CLN-7V
        at: [ 18, 44 ]
        facing: 0
        crew:
          name: Ines Barreto
          gunnery: 4
          piloting: 5

      - fullname: Grasshopper GHR-5J
        at: [ 22, 44 ]
        facing: 0
        crew:
          name: Milo Renders
          gunnery: 4
          piloting: 5

      - fullname: Warhammer WHM-8D
        at: [ 26, 44 ]
        facing: 0
        crew:
          name: Talia Voss
          gunnery: 4
          piloting: 5

  - name: Defence Force
    bot:
      ai: PRINCESS

    units:
      - fullname: Hellcat II HCT-213B
        at: [ 30, 4 ]
        facing: 3
        altitude: 5
        velocity: 2
        bombs:
          external:
            HE: 10
        crew:
          name: Iqbal Langah
          gunnery: 4
          piloting: 5

      - fullname: Rapier RPR-100
        at: [ 38, 4 ]
        facing: 3
        altitude: 5
        velocity: 2
        bombs:
          external:
            HE: 17
        crew:
          name: Magda Krebs
          gunnery: 4
          piloting: 5

      - fullname: Phoenix Hawk PXH-3M
        at: [ 28, 8 ]
        facing: 3
        crew:
          name: Bram Odendaal
          gunnery: 4
          piloting: 5

      - fullname: Ostroc OSR-2D
        at: [ 32, 8 ]
        facing: 3
        crew:
          name: Militsa Nyukhalov
          gunnery: 4
          piloting: 5

      - fullname: Chameleon CLN-7V
        at: [ 36, 8 ]
        facing: 3
        crew:
          name: Sunda Marik
          gunnery: 4
          piloting: 5

      - fullname: Griffin GRF-3M
        at: [ 40, 8 ]
        facing: 3
        crew:
          name: Denny Okafor
          gunnery: 4
          piloting: 5
