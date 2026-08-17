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
name: Aerospace Duel - Low Altitude
description: >
  Two fighter pairs on a low-altitude map, deployed at matched altitude and closing head-on. This is the
  venue where turning is cheap and the dead zone is small, so the fight is about firing arcs and position
  rather than altitude - the ground-map duel owns the altitude question.

  The flights deploy at the SAME altitude deliberately. An earlier version deployed them two levels apart,
  and the Princess mirror match degenerated: with no altitude preference anywhere in the stock bot, both
  flights closed to adjacent hexes and then orbited for the rest of the game one hex apart, two altitudes
  of separation keeping every shot inside the dead zone (TW p.241) and therefore Impossible. Zero shots
  were fired in ten games. That is a genuine bot finding - the primer's PC-08-01/PC-08-02 live - but it
  makes a useless baseline, because the control arm cannot fight at all.

  Watch for: fighters working for arcs their target cannot answer, and velocity kept low enough to turn
  back into the fight. Set both factions to the same ai: value for a control run.

map:
  type: sky
  width: 36
  height: 30

options:
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

  - name: Epsilon Galaxy Flight
    bot:
      ai: PRINCESS

    units:
      - fullname: Cheetah F-11
        at: [ 8, 12 ]
        facing: 1
        altitude: 5
        velocity: 2
        crew:
          name: Marianne O'Brien
          gunnery: 4
          piloting: 4

      - fullname: Cheetah F-11
        at: [ 8, 16 ]
        facing: 1
        altitude: 5
        velocity: 2
        crew:
          name: Giulia DeMarco
          gunnery: 4
          piloting: 4

  - name: Star Corps Flight
    bot:
      ai: CASPAR

    units:
      - fullname: Cheetah F-11
        at: [ 28, 12 ]
        facing: 4
        altitude: 5
        velocity: 2
        crew:
          name: Tomas Reyes
          gunnery: 4
          piloting: 4

      - fullname: Cheetah F-11
        at: [ 28, 16 ]
        facing: 4
        altitude: 5
        velocity: 2
        crew:
          name: Ilse Vandenberg
          gunnery: 4
          piloting: 4
