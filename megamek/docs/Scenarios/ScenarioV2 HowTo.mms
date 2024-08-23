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
  boardcolumns: 2                           # a 2x1 map, default: 1
#  boardrows: 1                              # default: 1
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

# space map
map:
  type: space
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

# Optional: game options
# when not given, user-set options are used
options:                                    # default: MM's default options
  #from: Example_options.xml
  #fixed: no                                 # default: yes; in this case, the Game Options Dialog is
                                            # not shown before the scenario starts
  # Activate options by listing them; the values must be those from OptionsConstants, as used for the game type
  - double_blind


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

    minefields:                               # optional, availability depending on game type
    - conventional: 2
    - command: 0
    - vibra: 2
    camo: clans/wolf/Alpha Galaxy.jpg         # image file, relative to the scenario file, or in data/camos otherwise
                                              # use slashes

    # Victory conditions; they always come as a list (use dashes)
    victory:
      # a victory is mainly a trigger
      - trigger:
          type: fledunits
          modify: atend
          units: [ 101, 102, 103, 104, 105, 106 ]
          atleast: 4
        # the onlyatend modifier means that this victory condition will not end the game by itself; instead
        # it will only be checked once the game has ended for any other reason, such as a game end trigger
        # (for example, a round count end)
        # sometimes, victory conditions and game end conditions are are easier to write when they are kept
        # apart; other times, this modifier can be omitted; then, this condition will end the game
        modify: onlyatend

    units:
#    - include: Annihilator ANH-13.mmu
      - fullname: Atlas AS7-D
        # type: TW_UNIT                         # default: TW_UNIT other: ASElement
        # pre-deployed:
        at: [7, 4]                            # position 0704 (pre-deployed)
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

        offboard: N                             # default: not offboard; values: N, E, S, W
        crew:                                   # default: unnamed 4/5 pilot
          name: Cpt. Frederic Nguyen
          piloting: 4
          gunnery: 3

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
      atleast: 7

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
      atleast: 6

# ###############################################
# Triggers
# are used to end the game, decide victory and show messages
# The following examples show all available triggers. Note that triggers by themselves (as given below)
# are not valid, they must always be attached to something else (message, end, victory) as shown above.

trigger:
  # The battlefieldcontrol condition is met when only live units of a single team remain on the battlefield,
  # disregarding MechWarriors, TeleMissiles, GunEmplacements, offboard and undeployed units.
  type: battlefieldcontrol

trigger:
  # The killedunits condition is met when the given number(s) of units have been destroyed (not fled)
  type: killedunits
  # Optional: limit the test to the player's units
  player: Player A
  # Optional: a list of units to limit the check to. This makes sense most of time to avoid counting MechWarriors
  # or other spawns; when giving unit IDs, the player limitation is redundant
  # It also makes sense to set fixed IDs for all units to make sure this works correctly
  units: [ 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112 ]
  # At least the given number of units must have been killed, can be alone or combined with atmost
  atleast: 7
  # At most the given number of units must have been killed, can be alone or combined with atleast
  atmost: 10
  # OR: the exact number of units must have been killed; this cannot be combined with atmost/atleast
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
      units: 201
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
      units: 201
    - type: phasestart
      phase: movement
