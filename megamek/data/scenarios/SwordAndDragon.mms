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
Name=The Sword and The Dragon

# Scenario description
Description=A DCMS Support Lance is engaged by FedCom defenders during a Planetary Assault

# Size of the map in mapboards
BoardWidth=2
BoardHeight=2

# Maps can be specified by name.  The order is left-to-right, top-to-bottom
# Any unspecified boards will be set to RANDOM
Maps=RANDOM,RANDOM,RANDOM,RANDOM

# Faction list
# The player name used to log into the server MUST match this name to play as that faction
#
Factions=Kurita,Davion

# Faction location
# Only used if the faction contains a unit without specified starting coordinates
# Valid values are N,NE,E,SE,S,SW,W,NW,C (center), and R (random)
Location_Kurita=C
Location_Davion=R


# Mechlist for each faction
#
# Units are constructed as Unit_<faction name>_<#>, where the faction name 
# matches the one listed in the Faction property and the # is a sequential 
# numbering starting at 1.  If there is a gap in the numbering, any units after
# the gap will be ignored.
#

# The format is MechRef,PilotName,PilotGunnery,PilotPiloting,facing,x,y
# Facing and coordinates are optional.  Facing is one of NW, N, NE, SE, S, SW

Unit_Kurita_1=AWS-8Q,Kaneda Smythe,3,3
Unit_Kurita_2=DRG-1C,Elliot Marlin,4,4
Unit_Kurita_3=PNT-9R,Hideyoshi Watanabe,4,4
Unit_Kurita_4=PNT-9R,Susan Takeda,4,3

Unit_Davion_1=GRF-1N,Peter Fizer,3,3
Unit_Davion_2=CRD-3D,Maverick,4,4
Unit_Davion_3=HBK-4J,Candice Coyle,4,5,SE,1,1
Unit_Davion_4=WVR-6K,Gordon Lopez,4,4,SE,1,3

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
# Here's an example that would result in 15 points of damage:
#
# Unit_Davion_2_Damage=3




