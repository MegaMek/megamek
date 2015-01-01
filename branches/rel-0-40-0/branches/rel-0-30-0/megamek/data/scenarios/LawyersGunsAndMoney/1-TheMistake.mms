# 
#  A MegaMek Scenario file
#  Based on Lawyers, Guns & Money CBT scenario package
#  Scenario available at http://www.classicbattletech.com/pdf/Lawyers,_Guns,_and_Money.pdf
#  Converted for MegaMek by Steve "ScAvenger001" Simon (ScAvenger004@yahoo.com)

MMSVersion=1


Name=The Mistake


Description=LG&M first mission


BoardWidth=1
BoardHeight=2

Maps=battletech,battletech


Factions=RedfieldsRenegades,LoneStar

Location_RedfieldsRenegades=S
Location_LoneStar=N

#XO Command Lance, Mustafa's Marauders
Unit_RedfieldsRenegades_1=Cyclops CP-10-Z,Major Clyde Mustafa,3,3
Unit_RedfieldsRenegades_2=Zeus ZEU-6S,Nina Reinhart,3,4
Unit_RedfieldsRenegades_3=Grasshopper GHR-5H,Shani Rand,2,4
Unit_RedfieldsRenegades_4=Hunchback HBK-4G,Curtis Jenkins,2,4,N,8,18
#Fire Support Lance, Miguel's Archers
Unit_RedfieldsRenegades_5=Catapult CPLT-C1,Lt. Miguel Ramirez,3,4
Unit_RedfieldsRenegades_6=Whitworth WTH-1,Andrea Hallock,4,4
Unit_RedfieldsRenegades_7=Trebuchet TBT-5N,Adam Fike,3,4
Unit_RedfieldsRenegades_8=Dervish DV-6M,Farid Jajou,3,4

#Assault Lance, Vagnozzi's Vanguard
Unit_LoneStar_1=Atlas AS7-D,Ranger-Captain Steve Vagnozzi,3,3
Unit_LoneStar_2=Awesome AWS-8Q,Carl Porter,3,4
Unit_LoneStar_3=Banshee BNC-3E,Bill Underhill,3,4
Unit_LoneStar_4=Dragon DRG-1N,Melissa Lopez,3,2
#Strike Lance, Davis' Desperadoes
Unit_LoneStar_5=Assassin ASN-21,Deputy James Davis,3,3,S,8,16
Unit_LoneStar_6=Enforcer ENF-4R,Kasha Lowe,3,4
Unit_LoneStar_7=Clint CLNT-2-3T,Brian Kolowski,4,5
Unit_LoneStar_8=Jenner JR7-D,Andrew Thompson,3,4,S,5,10




Unit_LoneStar_5_DamageSpecific=N1:0,I1:4,R3:0,I3:8

Unit_RedfieldsRenegades_4_CritHit=0:2
Unit_RedfieldsRenegades_4_SetAmmoTo=3:1-4


