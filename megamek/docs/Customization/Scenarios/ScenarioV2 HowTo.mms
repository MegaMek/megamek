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

# This document explains how to write a scenario V2 file.
# ScenarioV2 uses YAML formatting which relies heavily on indentation

# Note that this file itself is not a well-formed scenario as it shows multiple examples of
# tags that may only be used once.
#

MMSVersion: 2                               # Required to be recognized as a Scenario file of this format
name: V2_test                   # Required title of the scenario; displayed in the scenario chooser
gametype: SBF                               # default: TW; other values: AS, BF, SBF
planet: Bellatrix                           # default: show no planet info

                                            # Required longer description; maximum length 350 characters
                                            # Use YAML formatting > to ignore linebreaks in the text (indentation!)
                                            # or write the text in one line "description: This scenario visits..."
description: >
  This scenario visits a fight of McCormack's Fusiliers in the campaign on Bellatrix against Ajax's
  Avengers. The retreating Avengers tried deperately to shake the Highlanders as they fled east across the southern continent of
  Bellatrix, but to no avail. Exhausted and bogged down by bad weather, the Avengers had no choice but to stand and fight.

singleplayer: yes                           # default: yes; the first player is the human player and all other players
                                            # are Princess bots. This will skip the Player/Camo assignment dialog
                                            # and the "Host game" dialog and directly connect
                                            # to a localhost Server and use the correct player name.
                                            # Other players can still join and replace the OpFor but will not receive
                                            # any messages intended for the single player


# Game Map -------------------------------------------------------------------------------------------
map:
  # Optional: the columns to arrange boards in. Default: 1
  # The number of rows follows from the number of boards given and the columns
  cols: 2                           # a 2x1 map, default: 1
  boards:
    - board1.board                            # all files are first searched relative to the scenario file, and
    - board2.board                            # if not found there, then relative to the appropriate data/... directory

# OR for a single board:
map: AGoAC Maps/16x17 Grassland 2.board

# OR "board node"
map:
  file: AGoAC Maps/16x17 Grassland 2.board

# surprise board from the given boards; require board nodes (file:)
# can modify individually and total
map:
  surprise:
    - file: AGoAC Maps/16x17 Grassland 2.board
      modify: rotate
    - file: AGoAC Maps/16x17 Grassland 3.board
    - file: AGoAC Maps/16x17 Grassland 4.board
  modify: rotate

# add modifiers to a single board
map:
  file: AGoAC Maps/16x17 Grassland 2.board
  modify: rotate

# atmospheric map without terrain
map:
  type: sky
  width: 65
  height: 35

# (deep) space map
map:
  type: space
  width: 65
  height: 35

# high altitude space map
map:
  type: highaltitude
  width: 65
  height: 35

# combined map with surprise maps
# when combining maps, full board nodes must be used (file:)
map:
  cols: 1
  boards:
    - file: unofficial/SimonLandmine/TheValley/30x15 TheValley-NorthEnd.board
    - surprise:
        - unofficial/SimonLandmine/TheValley/30x15 TheValley-Open1.board
        - unofficial/SimonLandmine/TheValley/30x15 TheValley-Open2.board
        - unofficial/SimonLandmine/TheValley/30x15 TheValley-Open3.board
        - unofficial/SimonLandmine/TheValley/30x15 TheValley-Open4.board
        - unofficial/SimonLandmine/TheValley/30x15 TheValley-Open5.board
        - unofficial/SimonLandmine/TheValley/30x15 TheValley-Quarry.board
        - unofficial/SimonLandmine/TheValley/30x15 TheValley-Forest.board
        - unofficial/SimonLandmine/TheValley/30x15 TheValley-Forest2.board
    - file: unofficial/SimonLandmine/TheValley/30x15 TheValley-SouthEnd1.board

# Multiple maps
maps:                                        # map and maps are 100% synonymous
  - file: AGoAC Maps/16x17 Grassland 2.board
  - boardcolumns: 2                           # a 2x1 map, default: 1
    boardrows: 1                              # default: 1
    boards:
      - board1.board
      - board2.board
    id: 1
  - type: sky                         # sky is atmospheric without terrain
    width: 65
    height: 35
  - type: space
    width: 20
    height: 20

# Multiple connected maps
map:
  - file: Beginner Box/16x17 Grassland 1.board
    # the name of this map, used in the map tab display
    name: Grassland
  - file: Battle of Tukayyid Pack/32x17 Pozoristu Mountains (CW).board
    name: Pozoristu Mountains
  - type: sky
    width: 65
    height: 35
    embed:
      # embed map id 0 (Grassland) at 5,5 in the sky map
      - at: [ 5, 5 ]
        id: 0
      # embed map id 1 (Pozoristu) at 8, 9 in the sky map
      - at: [ 8, 9 ]
        id: 1

# Post Processing:
map:
  file: AGoAC Maps/16x17 Grassland 2.board
  # post processing changes the resulting board in various ways (to be expanded in the future)
  # postprocess is always followed by a list of post processors (use dashes)
  postprocess:
    # settheme changes the theme of every hex
    - type: settheme
      theme: desert

    # removeterrain deletes the given terrain from all hexes
    - type: removeterrain
      # the terrain type as in the board files
      terrain: water
      # optional: the level; when given, only removes when the level is matched, otherwise all terrain of the type
      level: 0

    # convertterrain converts a terrain by changing the terrain type and/or level
    - type: convertterrain
      # required: the terrain type to change, as used in board files
      terrain: woods
      # optional: the terrain level to convert from
      # when omitted, any terrain level is converted (but only if the terrain is present)
      level: 1
      # optional: the new terrain type; if already present, it will be overwritten
      # when not given, the terrain type remains unchanged
      newterrain: rough
      # optional: the terrain level to convert to
      # when omitted, the terrain level is left unchanged
      newlevel: 2
      # obviously, at least one of newterrain and newlevel must be given

    # addterrain adds terrain in a given area
    - type: addterrain
      # required: the terrain type to change, as used in board files
      terrain: fire
      # required: the terrain level
      level: 1
      # required: an area to apply it to, see "areas" section
      area:
        list:
          - [ 10, 10 ]

    # hexlevel sets the hex levels in a given area
    - type: hexlevel
      # required: the new hex level
      level: 2
      # required: an area to apply it to, see "areas" section
      area:
        list:
          - [ 10, 10 ]


# Optional: game options
# when not given, the options from the latest game are used
options:
  #from: Example_options.xml
  file: Example_options.xml
  # The file is always loaded first, the following list overrides individual settings
  on:
    # Activate options by listing them; the values must be those from OptionsConstants, as used for the game type
    - double_blind
    - single_blind_bots
  off:
    # Deactivate settings
    - tacops_fatigue


# Planetary Conditions -----------------------------------------------------------------------------------
fixedplanetaryconditions: yes               # default: yes; in this case, the Planetary Conditions Dialog is
                                            # not shown before the scenario starts
planetaryconditions:                        # default: standard conditions
  temperature: -14                          # default: 25; only integer values
  pressure: trace                           # default: standard; other values: vacuum, trace, thin, high, very high
  gravity: 1.12                             # default: 1
  light: dusk                               # default: day; other values: dusk, full moon, moonless, pitchblack
  weather: gusting rain                     # default: none; other values: light rain, moderate rain, heavy rain
                                            # gusting rain, downpour, light snow, moderate snow, heavy snow
                                            # snow flurries, sleet, blizzard, ice storm
  wind:
    strength: moderate gale                 # default: none; other values: light gale, moderate gale, strong gale
                                            # storm, tornado, tornado f4
    #minimum: light gale                    # default: none
    #maximum: strong gale                   # default: tornado f4
    direction: NW                           # default: random; other values: N, NE, SE, S, SW, NW
    shifting: no                            # default: no

  fog: light                                # default: none; other values: light, heavy
  blowingsand: yes                          # default: no
  emi: yes                                  # default: no
  terrainchanges: yes                       # default: yes


# Forces -------------------------------------------------------------------------------------------

factions:
  # The first player is assumed to be the human player, while the rest get bots assigned by default.
  # To have the bots play all factions, insert a player without any units as the first player
  # Only this line is required:
  # - name: Human Observer


  - name: Player A
    team: 1                                   # default: each player goes into their own team
    deploy: N                                 # default: same as the home edge
    # or:
    deploy:
      edge: S
      # offset is 0 by default
      offset: 0
      # width is 3 by default
      width: 1
    # OR
    deploy:
      # see also area definitions
      area:
        union:
          first:
            circle:
              center: [ 10, 10 ]
              radius: 7
          second:
            list:
              - [2,2]
              - [5,5]

    minefields:                               # optional, availability depending on game type
    - conventional: 2
    - command: 0
    - vibra: 2
    camo: clans/wolf/Alpha Galaxy.jpg         # image file, relative to the scenario file, or in data/camos otherwise
                                              # use slashes

    # Units are always an array (use dashes)

    # Victory conditions; they always come as a list (use dashes). Victory conditions can be listed for the
    # player that wins or outside of the factions (see below)
    victory:
      # a victory is mainly a trigger
      - trigger:
          type: fledunits
          modify: atend
          units: [ 101, 102, 103, 104, 105, 106 ]
          atLeast: 4
        # the onlyatend modifier means that this victory condition will not end the game by itself; instead
        # it will only be checked once the game has ended for any other reason, such as a game end trigger
        # (for example, a round count end)
        # sometimes, victory conditions and game end conditions are are easier to write when they are kept
        # apart; other times, this modifier can be omitted; then, this condition will end the game
        modify: onlyatend

    units:
#    - include: Annihilator ANH-13.mmu
      - fullname: Atlas AS7-D
        # pre-deployed:
        # default: not offboard; values: NORTH, EAST, SOUTH, WEST
        offboard: NORTH
        # the offboard distance in hexes; defaults to 17
        distance: 200
        # Optional: when pre-deployed, set the facing. 5 = NW
        facing: 5
        at: [7, 4]                            # position 0704 (pre-deployed)
        # The board to be in; defaults to 0; this can also be used without a position (deploy to this board)
        board: 0
  #      x: 7                                 # alternative way to give position
  #      y: 4                                    # must have both x and y or neither
        # NOT pre-deployed:
        deploymentround: 2                    # default: deploy at start; here: reinforce at the start of round 2

#        elevation: 5                            # default: 5 for airborne ground; can be used in place of altitude
#        altitude: 8                             # default: 5 for aero
        status: prone, hidden                   # default: none; values: shutdown, hulldown, prone, hidden

        # Optional: the force is given as it is written to MUL files; from upper (regiment) to lower (lance) level
        # each level is <name>|<force id>; separate levels by a double pipe ||
        # the force ids are used to distinguish different forces with the same name (e.g. multiple "Assault Lance")
        force: 2nd Sword of Light|21||Zakahashi's Zombies|22||Assault Lance|23

        # pre-applied damage may assign remaining armor and internal structure values. Values
        # higher than the undamaged values of the unit are ignored. Negative values set to 0 (TODO)
        remaining:
          armor:
            # remaining armor values, use the usual location names
            LT: 2
            CTR: 0
          internal:
            # remaining internal structure is independent of armor and does not create any crits
            # TODO: have 0 internal destroy the location
            LA: 2

        # location crits
        # this usually requires looking up the unit file
        crits:
          # the usual location names. Give the slots as an array ([ 4, 8 ] or using dashes on separate lines)
          # slots are 1-based, i.e. CT has slots 1 to 12 (not 0)
          # location crits will mark the equipment as damaged, but never have any secondary effects
          # like explosions or pilot hits. Crits that destroy a unit are invalid (e.g. 3 engine hits)
          LA: 4
          RT: [ 1, 3 ]
          CT: 1
          # non-location crits (TODO)
          # Tanks
          # Engine hits (TW p.194); the number is ignored (more than one hit has no further effect) and can be omitted
          engine: 1
          # motive: 1
          # firecontrol: 1

        # ammo types and reduced amount
        # this usually requires looking up the unit file and possibly AmmoType.java for the type designations
        ammo:
          LA:
            slot: 5
            shots: 2
            # type: xyz (TODO)

        # Bombs for any IBomber types (LAM and aero units);
        bombs:
          # bombs can be given directly, which will place them on external hardpoints
          HE: 1
          CLUSTER: 2
          LG: 3
          # other types, see BombTypeEnum
          # RL, TAG, AAA, AS, ASEW, ARROW, HOMING, INFERNO, LAA, THUNDER, TORPEDO, ALAMO, FAE_SMALL, FAE_LARGE, RLP

        # alternatively, bombs can be separated into internal and external; in this case, do not give bombs directly
        # internal requires the IBB quirk
        bombs:
          internal:
            HE: 2
            LG: 2
          external:
            CLUSTER: 1

        # bombs:
        #   HE: 1    <---- wrong, must use external:
        #   internal:
        #     CLUSTER: 2

        # Optional: give details of the crew/pilot - currently only for single pilots (TODO)
        # by default, the pilot is an unnamed 4/5 pilot
        # all fields in crew are optional
        crew:
          name: Cpt. Frederic Nguyen
          callsign: MAGIC
          piloting: 4
          gunnery: 3
          # Optional: pilot hits, 0 to 6
          hits: 3
          # Optional: a portrait, relative to data/images/portraits
          portrait: Male/MechWarrior/MW_M_13.png

    # Carryable objects. These currently have no real owner, but if they are not pre-deployed, the present
    # player will deploy them. When pre-deployed (at: [ x, y ]), the owner is currently irrelevant.
    objects:

      # All objects require a name and weight. Currently the type is automatically Briefcase.java, later new
      # types may be incoming; currently no ID, might make sense
      - name: Black Briefcase With Codes
        # weight in tons, need not be integer
        weight: 1
        # pre-deployed at a position
        at: [ 2, 3 ]
        # currently the only available status: invulnerable
        # ideas:
        # forbidden_owner (may only be picked up by enemies) - this would require them to have an owner
        # respawn (respawns if destroyed or removed from the map)
        status: invulnerable

      - name: Cargo Crate
        weight: 5

      - name: Ambassador Hisho
        weight: 0.08

  - name: "Player B"
    home: "E"

    # Bots can be given specific settings
    bot:
      # Optional: currently the only type is princess
      type: princess
      # Princess settings always are 0...10, see BehaviorSettings.java
      # Optional: how worried about enemy damage am I?
      selfpreservation: 5
      # Optional: how much do I want to avoid failed Piloting Rolls?
      fallshame: 5
      # Optional: how close to I want to get to my enemies?
      hyperaggression: 5
      # Optional: how close do I want to stick to my teammates?
      herdmentality: 5
      # Optional: how quickly will I try to escape once damaged?
      bravery: 5
      # Optional: use forced Withdrawal, this is true by default
      forcedwithdrawal: true
      # Optional: the edge to retreat to, nearest by default; use south, north, west, east, nearest
      withdrawto: nearest
      # Optional: flee = true will try to reach the destination edge even when not crippled
      flee: true
      # Optional: the edge to flee to; use south, north, west, east, nearest
      fleeto: none

      #      private boolean goHome = false; // Should I immediately proceed to my home board edge?
      #      private final Set<String> strategicBuildingTargets = new HashSet<>(); // What (besides enemy units) do I want to blow up?
      #      private final Set<Integer> priorityUnitTargets = new HashSet<>(); // What units do I especially want to blow up?


    units:
      - fullname: Schrek PPC Carrier
        type: TW_UNIT
        at: [7, 4]                            # alternative way to indicate position
        #      x: 7                                    # position, indicates that the unit is deployed
        #      y: 4                                    # must have both x and y or neither
        elevation: 5                            # default: 5 for airborne ground; can be used in place of altitude
        altitude: 8                             # default: 5 for aero
        status: prone, hidden                   # default: none; other values: shutdown, hulldown
        offboard: N                             # default: not offboard; values: N, E, S, W
        crew:                                   # default: unnamed 4/5 pilot
          name: Cpt. Frederic Nguyen
          piloting: 4
          gunnery: 3
      - type: ASElement                         # default: TW standard unit
        fullname: Atlas AS7-D
        x: 5
        y: 3
        reinforce: 2                            # default: deploy at start; here: reinforce at the start of round 2
        # cannot be combined with a position
        crew:
          name: Cpt. Rhonda Snord
          piloting: 4
          gunnery: 3

#events:
#  - type: princesssettings
#    trigger:
#      - type: unitkilled
#        unit: 103
#    destination: south
#    flee: true

# ###############################################
# C3 networks:
# As there can be more than one network, c3 networks must always be given as a list (preceded with hyphen), even
# if there is only a single C3 network
c3:
  # C3 networks without a master can be given as a single list; the loader will determine if they are C3i, NC3 or Nova
  # The order of the units does not matter
  - [101, 102]
  - [2, 3, 6, 8]
  - [5, 9, 12]
  # Standard C3 must explicitly give the master as a single ID and the connected units as a list; the connected units
  # can be C3S or C3M (in this case the master will be set to company commander mode), note the rules TW p.132
  - c3m: 208
    connected: [ 202, 203 ]
  # 301 is next connected to 302 and 208 which are both C3M; 301 is set to CC mode and the network contains 5 units:
  - c3m: 301
    connected: [ 302, 208 ]
  # atm, double master units are not supported

# ###############################################
# Transporting units:
transports:
  # the carrier and the carried unit ids
  102: [ 104, 105 ]
  202: [ 205 ]


# ###############################################
# Game End, Victory and Victory/Defeat/Other Messages
# are three separate things.

# The game ends when any of the game end trigger conditions is met *at the end of a round*. Note that end
# triggers *must not* use the "once" nor the "atend" modifier, but they can use the "not" modifier.
# When the game ends, all victory conditions are checked to see which team wins. If no conditions are met, the
# game is a draw.
# In addition to the end triggers, the game can also end when a victory condition is met at the end of a round
# and that victory condition does not have the onlyatend modifier, see the victory: tag below.

# end: is always followed by an array of triggers, i.e. dashes must be used: - trigger:
end:
  - trigger:
      # in most scenarios, this makes sense...
      type: battlefieldcontrol

  - trigger:
      type: killedunits
      units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
      atLeast: 7

  - trigger:
      type: killedunit
      unit: 201

  - trigger:
      type: roundend
      round: 12

# ###############################################
# Messages
# are always an array (use dashes)
messages:
    # Messages require a header (shown as the title of the message dialog window)
  - header: Victory
    # The text is shown in the message dialog. It uses markdown formatting. Use the pipe | as shown in the
    # example to preserve paragraphs
    text: |
      ## Victory

      The Second Sword of Light successfully broke through the line of the Davion defenses. They will not
      recover their Prince's body. This will be a heavy blow to their morale.

      Yorinaga Kurita commends your performance by not being displeased.
    # Optional: the image is shown to the left of the text. The size is not fixed, but ~ 350 x 350 tends
    # to look good.
    image: tosaveaprince_splash.png
    # The trigger controls when to show the message
    # Most messages should appear only once, so using the "once" modifier usually makes sense.
    # Victory/defeat messages should use the atend modifier to be shown only when the game has ended.
    # Note that the message itself does not end the game or control victory, it is just displayed.
    trigger:
      type: fledunits
      modify: [ atend, once ]
      units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
      atLeast: 6

# ###############################################
# Victory conditions; they always come as a list (use dashes). Listing them outside the factions can be
# done as an alternative or in addition to victory conditions listed with the factions. The exception
# is a draw condition that should directly end the game which cannot be given as part of the factions
# but must be given as described here
victory:
  # When a victory condition is a win, it must give the player (= Team) it applies to.
  - player: Player A
    trigger:
      type: fledunits
      modify: atend
      units: [ 101, 102, 103, 104, 105, 106 ]
      atLeast: 4
    # the onlyatend modifier means that this victory condition will not end the game by itself; instead
    # it will only be checked once the game has ended for any other reason, such as a game end trigger
    # (for example, a round count end)
    # sometimes, victory conditions and game end conditions are are easier to write when they are kept
    # apart; other times, this modifier can be omitted; then, this condition will end the game
    modify: onlyatend

  # When a victory condition does not give the player, it is a draw condition. In this case, it is
  # automatically game-ending (this is because, when the game is ended, it is automatically considered
  # a draw when no actual win condition is met. No explicit draw condition needs to be given.
  # Draw conditions are only needed when they are supposed to also end the game). So, it should not
  # use the onlyatend modifier
  - trigger:
      type: unitkilled
      unit: 104

# ###############################################
# Triggers
# are used to end the game, decide victory and show messages
# The following examples show all available triggers. Note that triggers by themselves (as given below)
# are not valid, they must always be attached to something else (message, end, victory) as shown above.

trigger:
  # The battlefieldcontrol condition is met when only live units of a single team remain on the battlefield,
  # disregarding MekWarriors, TeleMissiles, GunEmplacements, offboard and undeployed units.
  type: battlefieldcontrol

trigger:
  # The killedunits condition is met when the given number(s) of units have been destroyed (not fled)
  type: killedunits
  # Optional: limit the test to the player's units
  player: Player A
  # Optional: a list of units to limit the check to. This makes sense most of time to avoid counting MekWarriors
  # or other spawns; when giving unit IDs, the player limitation is redundant
  # It also makes sense to set fixed IDs for all units to make sure this works correctly
  units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
  # At least the given number of units must have been killed, can be alone or combined with atmost
  atLeast: 7
  # At most the given number of units must have been killed, can be alone or combined with atLeast
  atmost: 10
  # OR: the exact number of units must have been killed; this cannot be combined with atmost/atLeast
  count: 2

trigger:
  # The killedunit condition is met when the unit has been destroyed (not fled)
  type: killedunit
  # The unit ID to be checked
  unit: 201

trigger:
  # The activeunits condition is met when the given number(s) of units are live and well (their deployment
  # is not checked; fled units are not active)
  # see the killedunits trigger for additional data to be given
  type: activeunits
  units: [ 201, 202, 203, 204, 205, 206, 207 ]
  count: 3

trigger:
  # The fledunits condition is met when the given number(s) of units have fled the battlefield
  # see the killedunits trigger for additional data to be given
  type: fledunits
  units: [ 201, 202, 203, 204, 205, 206, 207 ]
  count: 1

trigger:
  # the gamestart trigger activates right at the start of the game, before deployment
  type: gamestart

trigger:
  # the phasestart trigger activates at the start of the given phase
  type: phasestart
  # the exact phase names are the enum constants of GamePhase.java in lowercase, e.g. deployment or pointblank_shot
  phase: movement

trigger:
  # This trigger is met at the end of one round or any round
  type: roundend
  # Optional: when the round number is given, only the end of that round meets the condition
  round: 12

trigger:
  # This trigger is met at the start of one round or any round
  type: roundstart
  # Optional: when the round number is given, only the start of that round meets the condition
  round: 12


# Trigger modifiers:
trigger:
  type: activeunits
  count: 1
  # Modifiers are
  #   once: make this trigger only ever fire once (useful e.g. for messages)
  #   not: invert this trigger, making it activate if and only if its conditions are not met
  #     with [not, once] the trigger will be inverted and fire only once; the order of these modifiers is irrelevant
  #   atend: this trigger will only fire when the game has ended (useful for victory/defeat messages)
  # The modifiers can be given as a single value or an array
  modify: once
  # or:
  modify: [ once, atend ]
  # or:
  modify:
    - not
    - once

# Technical "triggers"; these can be nested
trigger:
  # The AND trigger combines all (two or more) given sub-triggers so that all of them must be active for the
  # AND trigger itself to fire. Note that sub-triggers should not use the modifier "once" or it is possible
  # that one of the sub-triggers activates alone and is "used up", preventing the AND trigger from ever firing.
  type: and
  triggers:
    # here, the unit 201 must be killed AND it must be the start of the movement phase for this AND trigger
    # to activate
    - type: killedunit
      unit: 201
    - type: phasestart
      phase: movement

trigger:
  # The OR trigger combines all (two or more) given sub-triggers so that at least one of them must be active
  # for the OR trigger itself to fire.
  type: or
  triggers:
    # here, the unit 201 must be killed OR it must be the start of the movement phase for this OR trigger
    # to activate
    - type: killedunit
      unit: 201
    - type: phasestart
      phase: movement

trigger:
  # The positions condition is met when the given number(s) of units are in the given area
  type: positions
  area:
    border:
      edges: north
      maxdistance: 3
  # Optional: limit the test to the player's units
  player: Player A
  # Optional: a list of units to limit the check to. This makes sense most of time to avoid counting MekWarriors
  # or other spawns; when giving unit IDs, the player limitation is redundant
  # It also makes sense to set fixed IDs for all units to make sure this works correctly
  units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
  # At least the given number of units must be in the given area, can be alone or combined with atmost
  atLeast: 7
  # At most the given number of units must be in the given area, can be alone or combined with atLeast
  atmost: 10
  # OR: the exact number of units must be in the given area; this cannot be combined with atmost/atLeast
  count: 2

trigger:
  # This is a simpler way to write a position condition that is met when the unit is in the given area
  type: position
  area:
    border:
      edges: north
      maxdistance: 3
  # The unit ID to be checked
  unit: 201

# ###############################################
# Areas
# are used to define places on the map. They are either a single shape or a combination of shapes. They are
# never given as a list, only a single element that is either the shape or the combination type.
# Areas need not be contiguous
area:
  # Combinations are union, difference and intersection (as in "Constructive Solid Geometry")
  # Each combination requires the "first:" and "second:" area to be given. These are areas in turn, i.e.,
  # they are themselves either shapes or combinations. In other words, this can be nested to any depth.
  union:
    first:
      # A hex circle (or more like, hex-shape) is all hexes around the center at a distance of at most the
      # given radius (the circle is filled). To get only the hexes at the distance 7, use a difference
      # of two circles, the second of radius 6 can be used.
      circle:
        center: [ 10, 10 ]
        radius: 7
    # In union and intersection, it does not matter which area is first and second. In a difference, the second
    # area is subtracted from the first, so reversing the two changes the result.
    second:
      # A list is simply a list of hex coordinates
      list:
        - [2,2]
        - [5,5]

area:
  difference:
    first:
      # A rectangle is given by its corners. The order of the values does not matter, i.e. the corners can be
      # upper left and lower right or upper right and lower left in any order. The rectangle is filled and includes
      # its border
      rectangle:
        - [ 2, 2 ]
        - [ 5, 5 ]
    second:
      # Subtracting a smaller rectangle leaves the border of the first rectangle
      rectangle:
        - [ 4, 4 ]
        - [ 3, 3 ]

area:
  # There are two versions of halfplane
  # One is cartesian, i.e. vertical or horizontal, i.e. all hexes above, below, to left or to right of a
  # given coordinate value, including the coordinate (line) itself
  halfplane:
    coordinate: 4
    # The direction the halfplane extends to: above, below, left or right. A toleft halfplane includes all
    # hexes of x <= coordinate
    extends: above

  # The other is delimited by a hex row in one of the 3 directions N/S, NE/SW and NW/SE. The plane extends to
  # either the right or left of that (there is no above/below, as the hex row cannot be horizontal). The
  # directions are, as always N = 0, SE = 2 ...; opposite directions have the same result
  halfplane:
    point: [4,5]
    direction: 2
    # The direction the halfplane extends to: above, below, to_left or to_right. A toleft halfplane includes all
    # hexes of x <= coordinate
    extends: left

area:
  intersection:
    first:
      # A line along one of the hex row directions (N = 0, SE = 2; opposite directions have the same result)
      # through one hex; the line is infinite
      line:
        point: [ 0, 5 ]
        direction: 1
    second:
      union:
        first:
          # A ray along one of the hex row directions (N = 0, SE = 2) starting at a hex; the ray is similar to the
          # line with the same values but it is cut off at the hex (the ray includes the start hex)
          ray:
            point: [ 0, 5 ]
            direction: 1
        second:
          # This area is the north border of the board (all hexes with y = 0)
          border: north

area:
  # Two or more borders of the board can be given as a list.
  # The absolute hexes that these represent depend on
  # the rectangle that the area is applied to (e.g. the board size)
  # The east (left) border is all hexes at x = 0; south is all hexes at y = board height; west all hexes at
  # x = board width
  border: [ south, east, west ]

area:
  # for a thicker border or inset border, use the "edges:" node
  border:
    edges: [ east, north ]
    # optional: the minimum distance from the edge; 0 means start at the edge hexes
    mindistance: 2
    # optional: the maximum distance from the edge
    maxdistance: 3

area:
  # The empty area has no hexes. Can be used to prevent units from fleeing the board
  empty:

area:
  # The "all" area includes all hexes of any board
  all:

area:
  # the area can be given as a terrain type
  terrain:
    # required: the terrain type to include in the area
    type: woods
    # optional: the terrain level to include; when omitted, any terrain level is included
    level: 1
    # OR optional: a range of terrain levels to include
    minlevel: 1
    maxlevel: 2
    # optional: the minimum distance from any hex with the terrain; 0 means the hexes themselves
    mindistance: 2
    # optional: the maximum distance from any hex with the terrain
    # be careful with distances of more than 3 or so on big boards: this leads to exploding calculation times
    maxdistance: 3

area:
  # the area can be given as hex levels to include
  # either a single hex level
  hexlevel: 0

area:
  # OR a range
  hexlevel:
    minlevel: 1
    # optional: the maximum hex level
    maxlevel: 2
