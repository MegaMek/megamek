# 
#  A MegaMek Scenario file
#

MMSVersion: 2                               # Required to be recognized as a Scenario file of this format
name: Example Scenario                      # Required title of the scenario; displayed in the scenario chooser
gametype: SBF                               # default: TW; other values: AS, BF, SBF
planet: Bellatrix                           # default: show no planet info
description: >
  This scenario visits a fight of McCormack's Fusiliers in the campaign on Bellatrix against Ajax's
  Avengers. The retreating Avengers tried deperately to shake the Highlanders as they fled east across the southern continent of
  Bellatrix, but to no avail. Exhausted and bogged down by bad weather, the Avengers had no choice but to stand and fight.
                                            # Required longer description; maximum length 350 characters
                                            # Use YAML formatting > to ignore linebreaks in the text (indentation!)
                                            # or write the text in one line "description: This scenario visits..."

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
conditions:                                 # default: standard conditions
  fixed: yes                                # default: yes; in this case, the Planetary Conditions Dialog is
                                            # not shown before the scenario starts
  temperature: -14                          # default: 25; only integer values
  pressure: standard                        # default: standard; other values: vacuum, trace, thin, high, very high
  gravity: 1.12                             # default: 1
  light: dusk                               # default: daylight; other values: fullmoon, moonless, pitchblack
  weather: none                             # default: none; other values: light rain, moderate rain, heavy rain
                                            # gusting rain, downpour, light snow, moderate snow, heavy snow
                                            # snow flurries, sleet, blizzard, ice storm
  wind:
    minimum: light gale                     # default: no shifting strength; may be used instead of strength
    maximum: strong gale                    # default: no shifting strength; use together with minimum
    strength: none                          # default: none; other values: light gale, moderate gale, strong gale
                                            # storm, tornado, tornado f4
    direction: random                       # default: random; other values: N, NE, SE, S, SW, NW
    shifting: yes                           # default: no

  fog: none                                 # default: none; other values: light, heavy
  blowingsand: yes                          # default: no
  emi: yes                                  # default: no
  terrainchanges: yes                       # default: yes


# Forces -------------------------------------------------------------------------------------------

Forces:
  - name: Player A
    team: 1
    home: W                                   # default: none; other values: N, NE, SE, S, SW, NW
    deploy: N                                 # default: same as the home edge
    minefields:                               # optional, availability depending on game type
    - conventional: 2
    - command: 0
    - vibra: 2
    camo: clans/wolf/Alpha Galaxy.jpg         # image file, relative to the scenario file, or in data/camos otherwise
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
    team: 2

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

## Mechlist for each faction -------------------------------------------------
##
## Units are constructed as Unit_<faction name>_<#>, where the faction name
## matches the one listed in the Faction property and the # is a sequential
## numbering starting at 1.  If there is a gap in the numbering, any units after
## the gap will be ignored.
##
#
## The format is MechRef,PilotName,PilotGunnery,PilotPiloting,facing,x,y
## Facing and coordinates are optional. Facing is one of NW, N, NE, SE, S, SW
## Example: Unit_Irregulars_1=HGN-732,Col Rhonda Snord,2,1,N,01,32
#
#Unit_PlayerA_1=Archer ARC-2R,PilotA1,3,4
#Unit_PlayerA_2=Hunchback HBK-4G,PilotA2,4,3
#
#Unit_PlayerB_1=Atlas AS7-D,PilotB1,3,3
#Unit_PlayerC_1=Locust LCT-1M,PilotB2,4,5
#
## Additional advantages to add to pilots. Most of these require the 'MaxTech
## Level3 Pilot Advantages' game option to be turned on. The possible values
## are:
## dodge_maneuver, maneuvering_ace, melee_specialist, pain_resistance
## Multiple advantages for one pilot are seperated by spaces
#Unit_PlayerA_2_Advantages=melee_specialist pain_resistance
#Unit_PlayerB_2_Advantages=dodge_maneuver
#
##set autoeject, only for mechs
#Unit_PlayerA_2_AutoEject=false
#Unit_PlayerB_2_AutoEject=true
#
##set which units should be commanders (for commander killed VC)
#Unit_PlayerA_1_Commander=true
#Unit_PlayerB_1_Commander=true
#Unit_PlayerC_1_Commander=true
#
## Unit Camos
## Assigns a camo to a unit, overriding any player camo
## The directory and filename must be separated by a comma and the directory must end in a /
#Unit_PlayerA_1_Camo=Clans/Wolf/,Alpha Galaxy.jpg
#
## To initially damage units, you can use a unit armor property, which specifies
## armor and internal values.  Values above the unit's nominal value for that
## location will be ignored.
## Armor is specified in this order:
## H,CT,CTR,RT,RTR,LT,LTR,RA,LA,RL,LL,HI,CTI,RTI,LTI,RAI,LAI,RLI,LLI
## Here's an example:
##
## Unit_Kurita_1_Armor=0,30,19,24,20,24,10,24,24,33,33,1,25,17,17,13,13,17,17
##
## Alternately, if you want more random damage, and want to allow critical
## damage before the game starts, you can use a unit damage property, which
## specifies a number of blocks of 5 damage that will be randomly applied to
## the unit using the standard hit chart.  Any internal and critical hits will
## be resolved normally.
## Warning: this can result in the unit being destroyed before the game begins.
## Unit_PlayerB_1_Damage=5
#
## Advanced Dammage Modification
##
## Mech Locations
## 	HEAD=0,CT=1,RT=2,LT=3,RARM=4,LARM=5,RLEG=6,LLEG=7
## Example for Mechs:
##   N0:1 Means Normal Armor Location 0 Set to 1
##   I2:2 Means Internal Armor Location 2 Set to 2
##   R2:3 Means Rear Armor Location 2 Set to 3
##
## Tank Locations
## 	Body=0,FRONT=1,RIGHT=2,LEFT=3,REAR=4,TURRET=5
##
## Infantry Location
##	Men = 0 (Will set the number of men in the platoon)
##
## Battle Armor
##      Unit#=0(First Unit Number) to Armor
##      EG Unit_Kurita_3_DamageSpecific=N2:1,N3:0
##          Will set unit 3 to have 1 Armor Remaning
##          while unit 4 Destroyed
##
## Proto Mechs
##      Head=0,Torso =1,RARM=2,LARM=3,LEGS=4,Main Gun=5
##
#Unit_Kurita_1_DamageSpecific=I1:10,N2:2
#Unit_Kurita_2_DamageSpecific=N0:1
#Unit_Kurita_4_DamageSpecific=N2:1,N2:2
#Unit_Kurita_3_DamageSpecific=N2:1,N3:0
#Unit_Kurita_5_DamageSpecific=N4:1,N1:1
#
#
## Critical Hits
## eg Unit_Kurita_1_CritHit=1:8
## This does a crit hit on location 1 slot 8.
##
## Mech Crit Hits
## 	HEAD=0,CT=1,RT=2,LT=3,RARM=4,LARM=5,RLEG=6,LLEG=7
## 	Slots starting from 1 to the number of filled critical slots
##
## Vehicle Crit Hits
##       Location is zero.
##       Slots is one of the following:
## 	1 = Crew stunned for 3 turns
## 	2 = Main weapon jams for 1 turn
## 	3 = Engine Destroyed Immobile
## 	4 = Crew killed (tank dead)
## 	5 = Fuel Tank/Engine Shielding (tank dead)
## 	6 = Power Plant Hit (tank dead)
##
## Proto Mechs
## 	Head=0,Torso=1,RARM=2,LARM=3,LEGS=4,Main Gun=5
## 	Slots starting from 1 to the number of critical hit boxes
##
##       In addition, you can specify whether the Torso weapons
##       should be damaged by damaging the following torso "slots":
##               Torso Weapon A=5,Torso Weapon B=6
##
##
#Unit_Kurita_1_CritHit=1:3,1:2
#Unit_Kurita_4_CritHit=2:1,1:5
#Unit_Kurita_5_CritHit=0:3
#Unit_Kurita_6_CritHit=2:10
#
## Set Ammo Ammount(Only Works for Mechs)
##
## Note will not be able to specifiy a value larger then Inital Ammout
##
## Loc and Slots are the same as Crit Locations
##
## For a Mech this would
## Unit_Kurita_6_SetAmmoTo=2:11-3
## Would set Ammo at Slot 2:11 to 3 points
##
#Unit_Kurita_6_SetAmmoTo=2:11-1
#
## Set Ammo type (works only for 'Mechs, too)
##
## Loc and Slots work the same as for critical locations and ammo ammout
##
## Ammo name is the unique string used in the 'Mech files themselves.
## Errors will be logged to the normal log file and the ammo replaced
## by the standard defined for the 'Mech. The same will happen for ammo
## which is illegal according to the specified game rules.
#Unit_PlayerA_2_SetAmmoType=3:1-ISAC20 Flak Ammo
#
## Set Altitude (works only for aerospace fighters, conventional fighters,
## small craft and dropships)
##
## You can set the altitude of a flying unit at the start of the scenario
## to be anything between 0 and 10. Altitude 0 means the unit deploys landed.
##
## Unit_Kurita_666_Altitude=3
#
#