# MegaMek Data (C) 2026 by The MegaMek Team is licensed under CC BY-NC-SA 4.0.
# To view a copy of this license, visit https://creativecommons.org/licenses/by-nc-sa/4.0/
#
# NOTICE: The MegaMek organization is a non-profit group of volunteers
# creating free software for the BattleTech community.
#
# MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
# of The Topps Company, Inc. All Rights Reserved.
#
# Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
# InMediaRes Productions, LLC.
#
# MechWarrior Copyright Microsoft Corporation. MegaMek Data was created under
# Microsoft's "Game Content Usage Rules"
# <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
# affiliated with Microsoft.
MMSVersion: 2
MMSVersion: 2
name: Bot Orders 9038 - crippled Champion with waypoint
description: Issue 9038 repro. Two Lyran Champions get waypoints north in round 2 while one is crippled, then a bot-wide flee NORTH in round 4. Before the fix the crippled Champion withdraws to the nearest edge (south, into Clan Wolf). Orders in 9038-champion-waypoints.orders.
map: unofficial/pokefan548/32x34 Farmland.board

# every game option pinned: headless games otherwise inherit mmconf/gameoptions.xml
options:
  file: bot-orders-options.xml

factions:
  - name: Observer
    team: 9

  - name: Lyran
    team: 1
    units:
      - fullname: Champion CHP-2N
        id: 101
        at: [14, 22]
        facing: 0
      - fullname: Champion CHP-2N
        id: 102
        at: [16, 22]
        facing: 0
    bot:
      ai: princess
      forcedwithdraw: true
      withdrawto: nearest
      flee: false
      fleeto: none

  - name: Clan Wolf
    team: 2
    units:
      - fullname: Black Hawk (Nova) Prime
        id: 201
        at: [12, 33]
        facing: 0
      - fullname: Hunchback IIC 2
        id: 202
        at: [16, 33]
        facing: 0
      - fullname: Fire Falcon A
        id: 203
        at: [20, 33]
        facing: 0
    bot:
      ai: princess
      forcedwithdraw: true
      withdrawto: south
      flee: false
      fleeto: none
