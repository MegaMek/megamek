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
Maps=RANDOM,RANDOM,RANDOM

# Faction list
# The player name used to log into the server MUST match this name to play as that faction
#
Factions=PlayerA,PlayerB

# Faction location
# Determines deployment area
# Valid values are Any,N,NE,E,SE,S,SW,W,NW
Location_PlayerA=W
Location_PlayerB=E

# Faction minefields
# Gives the player minefields to deploy, the first number is conventional, the second
# command-detonated and the last is vibrabombs.
Minefields_PlayerA=2,0,2
Minefields_PlayerB=1,0,3

# Mechlist for each faction
#
# Units are constructed as Unit_<faction name>_<#>, where the faction name 
# matches the one listed in the Faction property and the # is a sequential 
# numbering starting at 1.  If there is a gap in the numbering, any units after
# the gap will be ignored.
#

# The format is MechRef,PilotName,PilotGunnery,PilotPiloting

Unit_PlayerA_1=Archer ARC-2R,PilotA1,3,4
Unit_PlayerA_2=Hunchback HBK-4G,PilotA2,4,3

Unit_PlayerB_1=Atlas AS7-D,PilotB1,3,3
Unit_PlayerB_2=Locust LCT-1M,PilotB2,4,5

# Additional advantages to add to pilots. Most of these require the 'MaxTech Level3 Pilot Advantages' game
# option to be turned on. The possible values are:
# dodge_maneuver, maneuvering_ace, melee_specialist, pain_resistance
# Multiple advantages for one pilot are seperated by spaces
Unit_PlayerA_2_Advantages=melee_specialist pain_resistance
Unit_PlayerB_2_Advantages=dodge_maneuver

# To initially damage units, you can use a unit armor property, which specifies
# armor and internal values.  Values above the unit's nominal value for that location
# will be ignored.  
# Armor is specified in this order: 
# H,CT,CTR,RT,RTR,LT,LTR,RA,LA,RL,LL,HI,CTI,RTI,LTI,RAI,LAI,RLI,LLI
# Here's an example:
#
# Unit_Kurita_1_Armor=0,30,19,24,20,24,10,24,24,33,33,1,25,17,17,13,13,17,17
#
# Alternately, if you want more random damage, and want to allow critical damage
# before the game starts, you can use a unit damage property, which specifies a
# number of blocks of 5 damage that will be randomly applied to the unit using
# the standard hit chart.  Any internal and critical hits will be resolved normally.
# Warning: this can result in the unit being destroyed before the game begins.
Unit_PlayerB_1_Damage=5




