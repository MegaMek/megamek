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
name: Aerospace Ground Attack Run
description: >
  Regression guard for air-to-ground. Two bombed-up fighters against a ground lance, with no enemy aircraft
  anywhere - so the air-to-air doctrine has nothing to say and must keep quiet.

  This is the scenario that catches the most likely way to break something: an altitude preference built for
  dogfighting has no business dragging a bomber off its attack profile, and a dead-zone check has no business
  refusing a ground attack, which is made along the flight path rather than at a range.

  Watch for: the fighters still dive bomb, they still descend into the strike band to do it, and they do not
  loiter at altitude waiting for an air target that does not exist. Compare an ai: PRINCESS run against an
  ai: CASPAR one - the behaviour here should look much the same either way.

# Board copied into testresources (qrf_airbase_50x50.board) so this scenario loads in CI, where
# the external data boards are not checked out - ScenarioBombLoadingTest depends on it.
map: qrf_airbase_50x50.board

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

  - name: Strike Flight
    bot:
      ai: CASPAR

    units:
      - fullname: Cheetah F-11
        at: [ 10, 40 ]
        facing: 0
        altitude: 5
        velocity: 1
        bombs:
          external:
            HE: 4
        crew:
          name: Tomas Reyes
          gunnery: 4
          piloting: 4

      - fullname: Cheetah F-11
        at: [ 14, 40 ]
        facing: 0
        altitude: 5
        velocity: 1
        bombs:
          external:
            HE: 4
        crew:
          name: Ilse Vandenberg
          gunnery: 4
          piloting: 4

  - name: Ground Lance
    bot:
      ai: PRINCESS

    units:
      - fullname: Atlas AS7-D
        at: [ 12, 12 ]
        facing: 0
      - fullname: Marauder MAD-3R
        at: [ 14, 12 ]
        facing: 0
      - fullname: Archer ARC-2R
        at: [ 16, 13 ]
        facing: 0
      - fullname: Locust LCT-1V
        at: [ 18, 13 ]
        facing: 0
