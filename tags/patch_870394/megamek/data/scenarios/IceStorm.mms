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
Name=The Ice Storm

# Scenario description
Description=A Clan Ice Hellion Trinary faces a Binary of heavier units from Clan Fire Mandrill

# Size of the map in mapboards
BoardWidth=3
BoardHeight=2

# Maps can be specified by name.  The order is left-to-right, top-to-bottom
# Any unspecified boards will be set to RANDOM
Maps=RANDOM

# Faction list
# The player name used to log into the server MUST match this name to play as that faction
#
Factions=Hellion,Mandrill

# Faction location
# Determines deployment area
# Valid values are Any,N,NE,E,SE,S,SW,W,NW
Location_Hellion=W
Location_Mandrill=E


# Mechlist for each faction
#
# Units are constructed as Unit_<faction name>_<#>, where the faction name 
# matches the one listed in the Faction property and the # is a sequential 
# numbering starting at 1.  If there is a gap in the numbering, any units after
# the gap will be ignored.
#

# The format is MechRef,PilotName,PilotGunnery,PilotPiloting

Unit_Hellion_1=Ice Ferret D,X,3,3
Unit_Hellion_2=Timber Wolf D,X,3,4
Unit_Hellion_3=Summoner A,X,3,4
Unit_Hellion_4=Black Lanner Prime,X,3,4
Unit_Hellion_5=Linebacker Prime,X,3,4

Unit_Hellion_6=Mist Lynx A,X,3,4
Unit_Hellion_7=Stormcrow B,X,3,3
Unit_Hellion_8=Viper Prime,X,3,4
Unit_Hellion_9=Ice Ferret D,X,3,4
Unit_Hellion_10=Black Lanner Prime,X,3,4

Unit_Hellion_11=Phantom D,X,3,3
Unit_Hellion_12=Fire Moth Prime,X,3,4
Unit_Hellion_13=Adder C,X,4,4
Unit_Hellion_14=Hellion B,X,3,4
Unit_Hellion_15=Kit Fox D,X,3,4

Unit_Mandrill_1=Summoner Prime,X,3,4
Unit_Mandrill_2=Summoner Prime,X,3,4
Unit_Mandrill_3=Turkina A,X,3,3
Unit_Mandrill_4=Warhawk A,X,3,4
Unit_Mandrill_5=Warhawk A,X,3,4

Unit_Mandrill_6=Cauldron-Born B,X,3,3
Unit_Mandrill_7=Cauldron-Born B,X,3,4
Unit_Mandrill_8=Mad Dog Prime,X,3,4
Unit_Mandrill_9=Gargoyle A,X,3,4
Unit_Mandrill_10=Warhawk Prime,X,3,4

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
# Unit_Kurita_1_Damage=3




