# 
#  A MegaMek Scenario file
#
# Future features for the scenario language
#    Alternate victory conditions
#    Staggered entry (reinforcements)
#    Specified critical slot damage
#

# Versionstamp required to be recognized as a Scenario file
MMSVersion=1

# Name of the scenario
Name=Example Scenario

# Scenario description
Description=This is an example scenario to show different scenario features

# Size of the map in mapboards
BoardWidth=2
BoardHeight=1

# Maps can be specified by name.  The order is left-to-right, top-to-bottom
# Any unspecified boards will be set to RANDOM
Maps=RANDOM,RANDOM

# Faction list
# The player name used to log into the server MUST match this name to play as
# that faction.  Player names can *not* include spaces.
#
Factions=PlayerA,PlayerB,PlayerC

# Faction location
# Determines deployment area
# Valid values are Any,N,NE,E,SE,S,SW,W,NW, and R (random)
Location_PlayerA=W
Location_PlayerB=E
Location_PlayerC=S

# Faction Teams
# Determines which players are on what teams.
# Valid values are any positive integer less than 2^31.
Team_PlayerA=1
Team_PlayerB=2
Team_PlayerC=2

# Faction minefields
# Gives the player minefields to deploy, the first number is conventional, the
# second command-detonated and the last is vibrabombs.
Minefields_PlayerA=2,0,2
Minefields_PlayerB=1,0,3

# Mechlist for each faction
#
# Units are constructed as Unit_<faction name>_<#>, where the faction name 
# matches the one listed in the Faction property and the # is a sequential 
# numbering starting at 1.  If there is a gap in the numbering, any units after
# the gap will be ignored.
#

# The format is MechRef,PilotName,PilotGunnery,PilotPiloting,facing,x,y
# Facing and coordinates are optional. Facing is one of NW, N, NE, SE, S, SW
# Example: Unit_Irregulars_1=HGN-732,Col Rhonda Snord,2,1,N,01,32

Unit_PlayerA_1=Archer ARC-2R,PilotA1,3,4
Unit_PlayerA_2=Hunchback HBK-4G,PilotA2,4,3

Unit_PlayerB_1=Atlas AS7-D,PilotB1,3,3
Unit_PlayerC_1=Locust LCT-1M,PilotB2,4,5

# Additional advantages to add to pilots. Most of these require the 'MaxTech
# Level3 Pilot Advantages' game option to be turned on. The possible values
# are:
# dodge_maneuver, maneuvering_ace, melee_specialist, pain_resistance
# Multiple advantages for one pilot are seperated by spaces
Unit_PlayerA_2_Advantages=melee_specialist pain_resistance
Unit_PlayerB_2_Advantages=dodge_maneuver

#set autoeject, only for mechs
Unit_PlayerA_2_AutoEject=false
Unit_PlayerB_2_AutoEject=true

#set which units should be commanders (for commander killed VC)
Unit_PlayerA_1_Commander=true
Unit_PlayerB_1_Commander=true
Unit_PlayerC_1_Commander=true

# To initially damage units, you can use a unit armor property, which specifies
# armor and internal values.  Values above the unit's nominal value for that
# location will be ignored.  
# Armor is specified in this order: 
# H,CT,CTR,RT,RTR,LT,LTR,RA,LA,RL,LL,HI,CTI,RTI,LTI,RAI,LAI,RLI,LLI
# Here's an example:
#
# Unit_Kurita_1_Armor=0,30,19,24,20,24,10,24,24,33,33,1,25,17,17,13,13,17,17
#
# Alternately, if you want more random damage, and want to allow critical
# damage before the game starts, you can use a unit damage property, which
# specifies a number of blocks of 5 damage that will be randomly applied to
# the unit using the standard hit chart.  Any internal and critical hits will
# be resolved normally.
# Warning: this can result in the unit being destroyed before the game begins.
# Unit_PlayerB_1_Damage=5

# Advanced Dammage Modification
# 
# Mech Locations
# 	HEAD=0,CT=1,RT=2,LT=3,RARM=4,LARM=5,RLEG=6,LLEG=7
# Example for Mechs:
#   N0:1 Means Normal Armor Location 0 Set to 1
#   I2:2 Means Internal Armor Location 2 Set to 2
#   R2:3 Means Rear Armor Location 2 Set to 3
#
# Tank Locations
# 	Body=0,FRONT=1,RIGHT=2,LEFT=3,REAR=4,TURRET=5
#
# Infantry Location
#	Men = 0 (Will set the number of men in the platoon)
#
# Battle Armor
#      Unit#=0(First Unit Number) to Armor 
#      EG Unit_Kurita_3_DamageSpecific=N2:1,N3:0
#          Will set unit 3 to have 1 Armor Remaning
#          while unit 4 Destroyed
#
# Proto Mechs
#      Head=0,Torso =1,RARM=2,LARM=3,LEGS=4,Main Gun=5
#      
Unit_Kurita_1_DamageSpecific=I1:10,N2:2
Unit_Kurita_2_DamageSpecific=N0:1
Unit_Kurita_4_DamageSpecific=N2:1,N2:2
Unit_Kurita_3_DamageSpecific=N2:1,N3:0
Unit_Kurita_5_DamageSpecific=N4:1,N1:1


# Critical Hits
# eg Unit_Kurita_1_CritHit=1:8
# This does a crit hit on location 1 slot 8.  
#
# Mech Crit Hits
# 	HEAD=0,CT=1,RT=2,LT=3,RARM=4,LARM=5,RLEG=6,LLEG=7
# 	Slots starting from 1 to the number of filled critical slots
#
# Vehicle Crit Hits
#       Location is zero.
#       Slots is one of the following:
# 	1 = Crew stunned for 3 turns
# 	2 = Main weapon jams for 1 turn
# 	3 = Engine Destroyed Immobile
# 	4 = Crew killed (tank dead)
# 	5 = Fuel Tank/Engine Shielding (tank dead) 
# 	6 = Power Plant Hit (tank dead)
#
# Proto Mechs
# 	Head=0,Torso=1,RARM=2,LARM=3,LEGS=4,Main Gun=5
# 	Slots starting from 1 to the number of critical hit boxes
#
#       In addition, you can specify whether the Torso weapons
#       should be damaged by damaging the following torso "slots":
#               Torso Weapon A=5,Torso Weapon B=6
#
#
Unit_Kurita_1_CritHit=1:3,1:2
Unit_Kurita_4_CritHit=2:1,1:5
Unit_Kurita_5_CritHit=0:3
Unit_Kurita_6_CritHit=2:10

# Set Ammo Ammount(Only Works for Mechs)
#
# Note will not be able to specifiy a value larger then Inital Ammout
# 
# Loc and Slots are the same as Crit Locations
#
# For a Mech this would
# Unit_Kurita_6_SetAmmoTo=2:11-3
# Would set Ammo at Slot 2:11 to 3 points
# 
Unit_Kurita_6_SetAmmoTo=2:11-1

# Set game options file
# This is an xml file which can be created by copying your
# mmconf/gameoptions.xml
# path is specified relative to the scenario file
# This is one way to set victory conditions
GameOptionsFile=Example_options.xml
