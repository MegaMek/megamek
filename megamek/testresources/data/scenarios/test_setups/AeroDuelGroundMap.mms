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
name: Aerospace Duel - Ground Map
description: >
  Two fighter pairs over a ground mapsheet, deliberately deployed two altitudes apart. Over a ground map the
  air-to-air dead zone is measured in low-altitude hexes, so two levels of separation blocks all fire within
  33 ground hexes - most of this board. Neither side can shoot until one of them changes altitude.

  Princess flies the north flight and cannot: its ground-map path generation drives every path it considers
  to altitude 5, so the altitude it deployed at is the altitude it keeps. CASPAR flies the south flight and
  should close the gap, because it generates paths at the altitudes enemy fighters have already committed to.

  Note: this uses flat terrain on purpose. Air-to-air line of sight over a ground mapsheet is computed as if
  the aircraft were on the ground (LosEffects only applies altitude on low-altitude boards), so any woods or
  ridge blocks fire between fighters at altitude. Until that is fixed, a wooded ground map produces no
  air-to-air combat at all.

  Watch for: does the south flight converge on the north flight's altitude, and do the two sides ever
  actually exchange fire? Set both factions to the same ai: value to get a control run.

# Four 55x55 boards in a 2x2, giving a 110x110 playing area. TW p.91 is explicit that aerospace on ground
# mapsheets "requires a very large playing area; ideally an arrangement of nine, twelve, sixteen or more
# mapsheets" - a 50x50 board is roughly 3x3 mapsheets, so four of them is about twelve. On a single 50x50 the
# fighters simply overshoot: one velocity point is 16 ground hexes (TW p.92), so a velocity-3 fighter covers
# 48 and leaves the board every turn instead of fighting.
#
# The board is deliberately a flat one. LosEffects only honours a unit's altitude on a low-altitude board
# (LosEffects.java:667); over a ground mapsheet an airborne fighter is given line of sight from ground level,
# so woods, buildings and ridges block air-to-air fire between aircraft five altitudes up. On a wooded board
# that suppresses essentially every shot and there is nothing left to measure. Flat ground sidesteps it. This
# is an engine defect, not a scenario preference - see the note in the description.
map:
  cols: 2
  boards:
    - file: unofficial/Jakes Map Pack/55x55 Abysmal Flats.board
    - file: unofficial/Jakes Map Pack/55x55 Abysmal Flats.board
    - file: unofficial/Jakes Map Pack/55x55 Abysmal Flats.board
    - file: unofficial/Jakes Map Pack/55x55 Abysmal Flats.board

options:
  on:
    - aero_ground_move
  off:
    - check_victory
    # Pinned OFF explicitly: the server seeds its options from the user's saved mmconf/gameoptions.xml, so an
    # unpinned option silently takes whatever the last human lobby game used. With capital fighters on, a
    # fighter's getWeaponList() returns the weapon-group list, which scenario-started games never populate -
    # every fighter plans and fires with no visible weapons. Found after four bloodless 10-game batches.
    - stratops_capital_fighter
    # Pinned OFF: inherited simultaneous phases trip a BotClient guard (ignoreSimTurn) that drops
    # every bot turn after its first each phase - a two-fighter bot fires ONE unit per firing phase,
    # which silently zeroed all dive bombing across ~50 headless benchmark games.
    - simultaneous_targeting
    - simultaneous_firing
    - simultaneous_physical
    - simultaneous_deployment

factions:
  # The first player slot is the headless watcher (ScenarioGameRunner excludes it from the bot slots),
  # so the combatants must come after it or one side never gets a bot and the game waits for a human.
  - name: Observer

  - name: North Flight
    bot:
      ai: PRINCESS

    units:
      - fullname: Cheetah F-11
        at: [ 46, 14 ]
        facing: 3
        altitude: 5
        velocity: 1
        crew:
          name: Marianne O'Brien
          gunnery: 4
          piloting: 4

      - fullname: Cheetah F-11
        at: [ 54, 14 ]
        facing: 3
        altitude: 5
        velocity: 1
        crew:
          name: Giulia DeMarco
          gunnery: 4
          piloting: 4

  - name: South Flight
    bot:
      ai: CASPAR

    units:
      - fullname: Cheetah F-11
        at: [ 46, 96 ]
        facing: 0
        altitude: 3
        velocity: 1
        crew:
          name: Tomas Reyes
          gunnery: 4
          piloting: 4

      - fullname: Cheetah F-11
        at: [ 54, 96 ]
        facing: 0
        altitude: 3
        velocity: 1
        crew:
          name: Ilse Vandenberg
          gunnery: 4
          piloting: 4
