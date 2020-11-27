Scenario-Readme file writen by Lee "T-Bolt" Smith
Modified by James "suvarov454" Damour
Modified by Chris Stadther

Format of a Scenario file: 
This document is to layout the file format of the .mms files. (Megamek Scenario Files). 

Any line beginning with the "#" is a comment and is not read by MegaMek when loading a Scenario. There are 13 sections to a .mms (MegaMek Scenario) file. Comprising of the following: 
1. The MegaMek Version 
2. Campaign Game Integration
3. Name of the Scenario 
4. Scenario description 
5. Board width, Board height
6. Map width, Map height 
7. Map(s) placement 
8. Faction list 
9. Faction location 
10. Faction minefields
11. Faction (A) Unit list 
12. Faction (B) Unit list 
13. Faction Deployment
14. Campaign Unit Integration
15. Advantages
16. Initial Damage to Units
17. Ammunition Options
18. Game options
19. Camo options

1. The MegaMek Version 
# Versionstamp required to be recognized as a Scenario file 
# This can NOT be modified
MMSVersion=1

2. Campaign Game Integration
# This field allows you to assign an external number to the current game. This number will do 
# nothing now but an addition after action report can be generated and that report can be 
# identify the game by this number.  NOTE: This is a number field.
ExternalId=120934


3. Name of the Scenario 
# Name of the scenario 
Name=A Few Crystals More. This is the name of the scenario you are creating 

4. Scenario description 
# This is a description of the Scenario you are creating 
Description=Rhonda's Irregulars Scenario Pack, A Few Crystals More. Page 44. 

5. Board width, Board height (optional)
# Size of the whole map board in mapsheets.
# This specifies how many BattleTech mapsheets that will be used. BoardWidth Specifies how many maps will be 
# laid out East and West (Width). BoardHeight specifies how many maps will be laid out North and South (Height). 
# If no BoardWidth or BoardHeight is specified, 1x1 is assumed.
BoardWidth=1
BoardHeight=2

6. Map width, Map height (optional)
# Size of each mapsheet in hexes.
# This specifies the size of each map that will be used. Default MegaMek maps are 16x17.
# If no MapWidth or MapHeight is specified, 16x17 is assumed.
MapWidth=16
MapHeight=17

#NOTE: The total play area in hexes is (BoardWidth * MapWidth) by (BoardHeight * MapHeight)

7. Map(s) placement 
# Maps can be specified by name. The order is left-to-right, top-to-bottom 
# Any unspecified boards will be set to RANDOM Maps=deserthills,mountainlake 
# If the map is in a subdirectory, the syntax is subdirectory/mapname:
# buildings/militarybase1, for example.
#
# Alternate Layout
# This lays out the specific maps for your scenario. Left to Right. If you use
# the "rotate:"  keyword, the board will appear rotated 180 degrees (North
# becomes South, East becomes West).
Maps=rotate:deserthills,rotate:mountainlake

8. Faction list 
# The player name used to log into the server MUST match this name to play as that faction 
# Factions=Irregulars,WacosRangers Faction List specifies the 2 Factions that will be battling in the scenario. 
# NOTE: When you connect, you MUST login as one of the Factions listed in this section. (Case is sensitive). 
# ALSO NOTE: you can **NOT** have spaces in these names.
Factions=Irregulars,WacosRangers

9. Faction location 
# Only used if the faction contains a unit without specified starting coordinates (not used in this scenario) 
# Valid values are N,NE,E,SE,S,SW,W,NW,C (center), and R (random) 
# Example: Location_Kurita=C This is currently the only working way of a "specified" placement. It will place 
# all the Units in this location on the map randomly. It also determines facing randomly. 
Location_Irregulars=W
Location_WacosRangers=E

10. Faction minefields
# Gives the player minefields to deploy, the first number is conventional, the second
# command-detonated and the last is vibrabombs.
Minefields_Irregulars=2,0,2
Minefields_WacosRangers=1,0,3

11. Faction (A) Unit list 
# The format is: MechRef,PilotName,PilotGunnery,PilotPiloting,facing,x,y 
# Facing and coordinates are optional. Facing is one of NW, N, NE, SE, S, SW 
# Example: Unit_Irregulars_1=HGN-732,Col Rhonda Snord,2,1,N,01,32 

# Irregulars 1st Command Lance: 
Unit_Irregulars_1=Archer ARC-2R,Col Cranston Snord,1,2,S,06,00 
Unit_Irregulars_2=Thunderbolt TDR-5S,Lt Deb H'Chu,2,1,S,07,00 
Unit_Irregulars_3=Marauder MAD-3R,David Rowsch,5,3,S,08,00 
Unit_Irregulars_4=Wasp WSP-1A,John Malvinson,4,3,S,09,00 

# Here is where you specify a "Unit" (be it Mech, Infantry, Vehicle or BattleArmor) for side A. 
# Format is as follows: 
# Unit name (Must match Faction List) Unit Number. Must be sequential, if a number is skipped, it will not place 
# the units after it. 
# Mech Type with variant Pilot Name Pilot,Gunnery skills Facing. 
# Here are the available options: N,NE,E,SE,S,SW,W,NW,C (center), and R (random) 
# Hex Placement. The first set of numbers specifies the Hex the unit is in, second set of numbers is the Hex it 
# is facing. (NOTE: The Hex Placement option is NOT used in the current version). 

12. Faction (B) Unit list 
# The format is: MechRef,PilotName,PilotGunnery,PilotPiloting,facing,x,y 
# Facing and coordinates are optional. Facing is one of NW, N, NE, SE, S, SW 
# Example: Unit_Irregulars_1=HGN-732,Col Rhonda Snord,2,1,N,01,32 

# WacosRangers, 
# Command Lance: 
Unit_WacosRangers_1=Battlemaster BLR-1G,Col Wayne Waco,1,2,N,07,12 
Unit_WacosRangers_2=Marauder MAD-3R,Cpt Akida Samsun,3,2,NE,09,13 
Unit_WacosRangers_3=Wasp WSP-1A,Cpt Reggie Randall,2,4,NE,09,14 
Unit_WacosRangers_4=Cyclops CP-10-Z,Sgt Lenny Markbright,3,3,N,08,12 

# Here is where you specify a "Unit" (be it Mech, Infantry, Vehicle or BattleArmor) for side A. 
# Format is as follows: 
# Unit name (Must match Faction List) Unit Number. Must be sequential, if a number is skipped, it will not place 
# the units after it. 
# Mech Type with variant Pilot Name Pilot,Gunnery skills Facing. 
# Here are the available options: N,NE,E,SE,S,SW,W,NW,C (center), and R (random) 
# Hex Placement. The first set of numbers specifies the Hex the unit is in, second set of numbers is the Hex it 
# is facing. (NOTE: The Hex Placement option is NOT used in the current version). 


13. Faction Deployment
# You may now set a turn for deployment of a unit to occur.  These units must have a deployment location 
# specified in the unit list.
#
# The unit below will deploy at its preset location on turn 3
Unit_Kurita_1_DeploymentRound=3

14. Campaign Unit Integration
# To help manage units over a long campaign you can now assign external id to each unit.  These ids will do 
# nothing now but an addition after action report can be generated and units within that report can be 
# identified by this number.  NOTE: This is a number field.
Unit_WacosRangers_1_ExternalID=1


15. Advantages:
# Additional advantages to add to pilots. Most of these require the 'MaxTech Level3 Pilot Advantages' game
# option to be turned on. The possible values are:
# dodge_maneuver, maneuvering_ace, melee_specialist, pain_resistance
# Multiple advantages for one pilot are separated by spaces
Unit_Irregulars_2_Advantages=melee_specialist pain_resistance
Unit_WacosRangers_2_Advantages=dodge_maneuver

16. Initial Damage to Units:
# There are multiple ways to initially damage units.  First you can specify a number of blocks of damage to a
# unit.  The damage will be grouped into blocks of 5 points and distributed randomly using that standard damage
# location charts.  Any internal and/or critical hits will be resolved normally. NOTE:  This can result in the
# unit being destroyed before the game begins.
Unit_WacosRangers_1_Damage=5

# Another way to apply damage to a unit is by specifying the armor/internal struction left for each location.
# Using this chart:
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
#          Will set unit 3 to have 1 Armor Remaining
#          while unit 4 Destroyed
#
# Proto Mechs
#      Head=0,Torso =1,RARM=2,LARM=3,LEGS=4,Main Gun=5
# 
# The unit below will start with 3 internal structure points left in the center torso and 2 armor points left
# in the right torso. 
Unit_WacosRangers_1_DamageSpecific=I1:3,N2:2

# Yet another way is by applying critical hits to specific locations within a unit.
# Using this chart, critical hits can be applied to any filled slot.
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
# The unit below has had critical hits to the center torso slots 3 and 2
Unit_WacosRangers_1_CritHit=1:3,1:2

# Lastly you can damage the mech pilot directly by assigning the pilot hits.
Unit_WacosRangers_1_PilotHits=1


17. Ammunition Setup
# Set Ammo Ammount(Only Works for Mechs)
#
# Note will not be able to specify a value larger then Inital Ammo
# 
# Loc and Slots are the same as Crit Locations
#
# The unit below Would have its ammo in right torso slot 11 set to 3
# 
Unit_WacosRangers_1_SetAmmoTo=2:11-3



18. Game options
# This is an xml file which can be created by copying your
# mmconf/gameoptions.xml
# path is specified relative to the scenario file
# This is one way to set victory conditions
GameOptionsFile=Example_options.xml



19. Camo options
# Set a camo for the player
# Replace Category and File with the appropriate names 
Camo_Faction=Category,File
# Set a unit specific individual camo
# Again, replace Category and File with the appropriate names
Unit_WacosRangers_1_Camo=Category,File
# Destroy the enemy!