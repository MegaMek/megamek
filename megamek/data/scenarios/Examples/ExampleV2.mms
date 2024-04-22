# 
#  A MegaMek Scenario file
#

MMSVersion: 2                               # Required to be recognized as a Scenario file of this format
name: Example Scenario V2                   # Required title of the scenario; displayed in the scenario chooser
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
  boardcolumns: 2                           # a 2x1 map
  boardrows: 1
  boards:
  - board1.board                            # all files are first searched relative to the scenario file, and
  - board2.board                            # if not found there, then relative to the appropriate data/... directory

# OR for a single board:
# board: theBoard.board

## Directories to choose random boards from
## RandomDirs=Map Set 2,Map Set 3,Map Set 4,Map Set 5,Map Set 6,Map Set 7
## Maps can be specified by name.  The order is left-to-right, top-to-bottom
## Any unspecified boards will be set to RANDOM
## Maps=RANDOM,RANDOM


# Game Options -------------------------------------------------------------------------------------------
options:                                    # default: MM's default options
  from: Example_options.xml
  fixed: no                                 # default: yes; in this case, the Game Options Dialog is
                                            # not shown before the scenario starts


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
    home: W                                   # default: Any; other values: N, NE, SE, S, SW, NW
    deploy: N                                 # default: same as the home edge
    minefields:                               # optional, availability depending on game type
    - conventional: 2
    - command: 0
    - vibra: 2
    camo: clans/wolf/Alpha Galaxy.jpg         # image file, relative to the scenario file, or in data/camos otherwise
                                              # use slashes
    units:
    - include: Annihilator ANH-13.mmu
      # at: [7, 4]                            # alternative way to indicate position
      x: 7                                    # position, indicates that the unit is deployed
      y: 4                                    # must have both x and y or neither
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

  - name: "Player B"
    home: "E"


triggers:
  - message:
      round: 0
      phase: before
      description: >
        The campaign on Bellatrix against Ajax's Avengers brought McCormack's Fusiliers instant fame.
        Arriving to find themselves outnumbered three-to-one, the Fusiliers quickly used their superior grasp of
        tactics and their ferocity to smash the defending unit.  The retreating Avengers tried deperately to shake the
        Highlanders as they fled east across the southern continent of Bellatrix, but to no avail. Exhausted and bogged
        down by bad weather, the Avengers had no choice but to stand and fight.
  - message:
      round: 2
      phase: before
      description: Reinforcements have arrived!
  - victory:
      alone: yes                            # no opposition left on the map
      description: The battlefield is yours! No opposition remains. Well done!

