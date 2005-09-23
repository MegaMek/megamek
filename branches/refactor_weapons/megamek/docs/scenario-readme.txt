Scenario-Readme file writen by Lee "T-Bolt" Smith
Modified by James "suvarov454" Damour

Format of a Scenario file: 
This document is to layout the file format of the .mms files. (Megamek Scenario Files). 

Any line begining with the "#" is a comment and is not read by MegaMek when loading a Scenario. There are 9 sections to a .mms (MegaMek Scenario) file. Comprising of the following: 
1. The MegaMek Version 
2. Name of the Scenario 
3. Scenario description 
4. Map width, Map height 
5. Map(s) placement 
6. Faction list 
7. Faction location 
8. Faction minefields
9. Faction (A) Unit list 
10. Faction (B) Unit list 
11 Advantages
12 Initial Damage to Units

Here is a breakdown of each section: 
1. The MegaMek Version 
# Versionstamp required to be recognized as a Scenario file 
# This can NOT be modified
MMSVersion=1

2. Name of the Scenario 
# Name of the scenario Name=A Few Crystals More. This is the name of the scenario you are creating 

3. Scenario description 
# This is a description of the Scenario you are creating 
# Scenario description Description=Rhonda's Irregulars Scenario Pack, A Few Crystals More. Page 44. 
4. Map width, Map height 
# Size of the map in mapboards BoardWidth=1 BoardHeight=2 This specifies how many STANDARD sized BattleTech maps that will be used. BoardWidth Specifies how many maps will be laid out East and West (Width). BoardHeight specifies how many maps will be laid out North and South (Height). 

5. Map(s) placement 
# Maps can be specified by name. The order is left-to-right, top-to-bottom 
# Any unspecified boards will be set to RANDOM Maps=deserthills,mountainlake 
# If the map is in a subdirectory, the syntax is subdirectory/mapname:
# buildings/militarybase1, for example.
#
# Alternate Layout
# This laysout the specific maps for your scenario. Left to Right. If you use
# the "rotate:"  keyword, the board will appear rotated 180 degrees (North
# becomes South, East becomes West).
Maps=rotate:deserthills,rotate:mountainlake


6. Faction list 
# The player name used to log into the server MUST match this name to play as that faction 
# Factions=Irregulars,WacosRangers Faction List specifies the 2 Factions that will be battling in the scenario. NOTE: When you connect, you MUST login as one of the Factions listed in this section. (Case is sensitive). ALSO NOTE: you can **NOT** have spaces in these names.
Factions=Irregulars,WacosRangers

7. Faction location 
# Only used if the faction contains a unit without specified starting coordinates (not used in this scenario) 
# Valid values are N,NE,E,SE,S,SW,W,NW,C (center), and R (random) 
# Example: Location_Kurita=C This is currently the only working way of a "specified" placement. It will place all the Units in this location on the map randomly. It also determains facing randomly. 
Location_Irregulars=W
Location_WacosRangers=E

8. Faction minefields
# Gives the player minefields to deploy, the first number is conventional, the second
# command-detonated and the last is vibrabombs.
Minefields_Irregulars=2,0,2
Minefields_WacosRangers=1,0,3

9. Faction (A) Unit list 
# The format is: MechRef,PilotName,PilotGunnery,PilotPiloting,facing,x,y 
# Facing and coordinates are optional. Facing is one of NW, N, NE, SE, S, SW 
# Example: Unit_Irregulars_1=HGN-732,Col Rhonda Snord,2,1,N,01,32 

# Irregulars 1st Command Lance: 
Unit_Irregulars_1=Archer ARC-2R,Col Cranston Snord,1,2,S,06,00 
Unit_Irregulars_2=Thunderbolt TDR-5S,Lt Deb H'Chu,2,1,S,07,00 
Unit_Irregulars_3=Marauder MAD-3R,David Rowsch,5,3,S,08,00 
Unit_Irregulars_4=Wasp WSP-1A,John Malvinson,4,3,S,09,00 

# Irregulars 2nd Company (Shorty's Scavengers) Command Lance: 
Unit_Irregulars_5=Rifleman RFL-3N,Cpt Samual "Shorty" Sneede,1,1,N,14,33 
Unit_Irregulars_6=Warhammer WHM-6R,Lt Jake Walmar,2,2,N,13,33 
Unit_Irregulars_7=Clint CLNT-2-3T,Marleen Danules,5,5,N,05,33 
Unit_Irregulars_8=Phoenix Hawk PXH-1,Carter Malvinson,6,5,N,06,33 

# Irregulars 3rd Company (Rhonda Snords Irregulars) Command Lance: 
Unit_Irregulars_9=Shadow Hawk SHD-2H,Cpt Rhonda Snord,2,2,W,00,14 
Unit_Irregulars_10=Phoenix Hawk PXH-1,Lt Terry Malvinson,3,3,NE,00,16 
Unit_Irregulars_11=Griffin GRF-1N,Walker Roche,4,6,W,00,13 
Unit_Irregulars_12=Awesome AWS-8Q,Victoria Rose,4,4,NE,00,17 

# Irregulars 3rd Company, Fire Lance: 
Unit_Irregulars_13=Rifleman RFL-3N,Lt Bright Thomlinson,2,1,NW,15,13 
Unit_Irregulars_14=Wolverine WVR-6R,Solomon Storm,2,3,NW,15,14 
Unit_Irregulars_15=Scorpion SCP-1N,Linda Thomlinson,6,3,E,15,15 
Unit_Irregulars_16=Warhammer WHM-6R,Blade Windall,4,4,E,15,16 

Here is where you specify a "Unit" (be it Mech, Infantry, Vehicle or BattleArmor) for side A. 
Format is as follows: 
Unit name (Must match Faction List) Unit Number. Must be sequential, if a number is skipped, it will not place the units after it. 
Mech Type with variant Pilot Name Pilot,Gunnery skills Facing. 
Here are the availible options: N,NE,E,SE,S,SW,W,NW,C (center), and R (random) 
Hex Placement. The first set of numbers specifies the Hex the unit is in, second set of numbers is the Hex it is facing. (NOTE: The Hex Placement option is NOT used in the current version). 

10. Faction (B) Unit list 
# The format is: MechRef,PilotName,PilotGunnery,PilotPiloting,facing,x,y 
# Facing and coordinates are optional. Facing is one of NW, N, NE, SE, S, SW 
# Example: Unit_Irregulars_1=HGN-732,Col Rhonda Snord,2,1,N,01,32 

# WacosRangers, 
# Command Lance: 
Unit_WacosRangers_1=Battlemaster BLR-1G,Col Wayne Waco,1,2,N,07,12 
Unit_WacosRangers_2=Marauder MAD-3R,Cpt Akida Samsun,3,2,NE,09,13 
Unit_WacosRangers_3=Wasp WSP-1A,Cpt Reggie Randall,2,4,NE,09,14 
Unit_WacosRangers_4=Cyclops CP-10-Z,Sgt Lenny Markbright,3,3,N,08,12 

# WacosRangers, Noblle's Command Lance: 
Unit_WacosRangers_5=Banshee BNC-3E,Mjr Paulus Noble,1,2,SW,05,14 
Unit_WacosRangers_6=Cyclops CP-10-Z,Cpt Jorge Delphinus,2,3,SW,05,12 
Unit_WacosRangers_7=Quickdraw QKD-4G,Lt Daverius Bunkerara,4,3,SW,06,14 
Unit_WacosRangers_8=Warhammer WHM-6R,Lt Marcus Aeolus Wernke,1,3,SW,06,13 

# WacosRangers, Sanchuie's Scout Lance: 
Unit_WacosRangers_9=Javelin JVN-10N,Lt Troy Sanchuie,1,4,NE,10,16 
Unit_WacosRangers_10=Javelin JVN-10N,Sgt Eric Long,4,4,NE,11,16 
Unit_WacosRangers_11=Locust LCT-1V,Jolly Jim Smith,3,4,S,10,17 
Unit_WacosRangers_12=Locust LCT-1V,Anita Michei,2,5,S,09,17 

Here is where you specify a "Unit" (be it Mech, Infantry, Vehicle or BattleArmor) for side B. Format is as follows: 
Unit name (Must match Faction List) Unit Number. Must be sequential, if a number is skipped, it will not place the units after it. 
Mech Type with variant Pilot Name Pilot,Gunnery skills Facing. 
Here are the availible options: N,NE,E,SE,S,SW,W,NW,C (center), and R (random) 
Hex Placement. The first set of numbers specifies the Hex the unit is in, second set of numbers is the Hex it is facing. (NOTE: The Hex Placement option is NOT used in the current version). 

11 Advantages:
# Additional advantages to add to pilots. Most of these require the 'MaxTech Level3 Pilot Advantages' game
# option to be turned on. The possible values are:
# dodge_maneuver, maneuvering_ace, melee_specialist, pain_resistance
# Multiple advantages for one pilot are seperated by spaces
Unit_Irregulars_2_Advantages=melee_specialist pain_resistance
Unit_WacosRangers_2_Advantages=dodge_maneuver

12 Initial Damage to Units:
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
Unit_WacosRangers_1_Damage=5

Here is the sample file I used to create this readme file. 
#  Irregulars-6
#  A MegaMek Scenario file
#  Created by Lee "T-Bolt" Smith (tbolt_irc@yahoo.com)

# Versionstamp required to be recognized as a Scenario file
MMSVersion=1

# Name of the scenario
Name=A Few Crystals More.

# Scenario description
Description=Rhonda's Irregulars Scenario Pack, A Few Crystals More.  Page 44.
# Destroy the enemy.

# Size of the map in mapboards
BoardWidth=1
BoardHeight=2

# Maps can be specified by name.  The order is left-to-right, top-to-bottom
# Any unspecified boards will be set to RANDOM
Maps=deserthills,mountainlake

# Faction list
# The player name used to log into the server MUST match this name to play as that faction
#
Factions=Irregulars,WacosRangers

# Faction location
# Only used if the faction contains a unit without specified starting coordinates (not used in this scenario)
# Valid values are N,NE,E,SE,S,SW,W,NW,C (center), and R (random)
# Example:  Location_Kurita=C

# Mechlist for each faction
#
# Units are constructed as Unit_<faction name>_<#>, where the faction name 
# matches the one listed in the Faction property and the # is a sequential 
# numbering starting at 1. If there is a gap in the numbering, any units after 
# the gap will be ignored. 
# 

# The format is: MechRef,PilotName,PilotGunnery,PilotPiloting,facing,x,y 
# Facing and coordinates are optional. Facing is one of NW, N, NE, SE, S, SW 
# Example: Unit_Irregulars_1=HGN-732,Col Rhonda Snord,2,1,N,01,32 

# Irregulars 1st Command Lance: 
Unit_Irregulars_1=Archer ARC-2R,Col Cranston Snord,1,2,S,06,00 
Unit_Irregulars_2=Thunderbolt TDR-5S,Lt Deb H'Chu,2,1,S,07,00 
Unit_Irregulars_3=Marauder MAD-3R,David Rowsch,5,3,S,08,00 
Unit_Irregulars_4=Wasp WSP-1A,John Malvinson,4,3,S,09,00 
# Irregulars 2nd Company (Shorty's Scavengers) Command Lance: 
Unit_Irregulars_5=Rifleman RFL-3N,Cpt Samual "Shorty" Sneede,1,1,N,14,33 
Unit_Irregulars_6=Warhammer WHM-6R,Lt Jake Walmar,2,2,N,13,33 
Unit_Irregulars_7=Clint CLNT-2-3T,Marleen Danules,5,5,N,05,33 
Unit_Irregulars_8=Phoenix Hawk PXH-1,Carter Malvinson,6,5,N,06,33 
# Irregulars 3rd Company (Rhonda Snords Irregulars) Command Lance: 
Unit_Irregulars_9=Shadow Hawk SHD-2H,Cpt Rhonda Snord,2,2,W,00,14 
Unit_Irregulars_10=Phoenix Hawk PXH-1,Lt Terry Malvinson,3,3,NE,00,16 
Unit_Irregulars_11=Griffin GRF-1N,Walker Roche,4,6,W,00,13 
Unit_Irregulars_12=Awesome AWS-8Q,Victoria Rose,4,4,NE,00,17 
# Irregulars 3rd Company, Fire Lance: 
Unit_Irregulars_13=Rifleman RFL-3N,Lt Bright Thomlinson,2,1,NW,15,13 
Unit_Irregulars_14=Wolverine WVR-6R,Solomon Storm,2,3,NW,15,14 
Unit_Irregulars_15=Scorpion SCP-1N,Linda Thomlinson,6,3,E,15,15 
Unit_Irregulars_16=Warhammer WHM-6R,Blade Windall,4,4,E,15,16 

#WacosRangers, Command Lance: 
Unit_WacosRangers_1=Battlemaster BLR-1G,Col Wayne Waco,1,2,N,07,12 
Unit_WacosRangers_2=Marauder MAD-3R,Cpt Akida Samsun,3,2,NE,09,13 
Unit_WacosRangers_3=Wasp WSP-1A,Cpt Reggie Randall,2,4,NE,09,14 
Unit_WacosRangers_4=Cyclops CP-10-Z,Sgt Lenny Markbright,3,3,N,08,12 
#WacosRangers, Noblle's Command Lance: 
Unit_WacosRangers_5=Banshee BNC-3E,Mjr Paulus Noble,1,2,SW,05,14 
Unit_WacosRangers_6=Cyclops CP-10-Z,Cpt Jorge Delphinus,2,3,SW,05,12 
Unit_WacosRangers_7=Quickdraw QKD-4G,Lt Daverius Bunkerara,4,3,SW,06,14 
Unit_WacosRangers_8=Warhammer WHM-6R,Lt Marcus Aeolus Wernke,1,3,SW,06,13 
#WacosRangers, Sanchuie's Scout Lance: 
Unit_WacosRangers_9=Javelin JVN-10N,Lt Troy Sanchuie,1,4,NE,10,16 
Unit_WacosRangers_10=Javelin JVN-10N,Sgt Eric Long,4,4,NE,11,16 
Unit_WacosRangers_11=Locust LCT-1V,Jolly Jim Smith,3,4,S,10,17 
Unit_WacosRangers_12=Locust LCT-1V,Anita Michei,2,5,S,09,17 

# Destroy the enemy!