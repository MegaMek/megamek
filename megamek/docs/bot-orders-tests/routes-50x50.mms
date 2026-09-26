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
name: Bot Orders Routes 50x50 - three lances a side
description: Three Lyran lances against three Clan Wolf groups on a 50x50 reservoir and dam map. Two Lyran lances get multi-waypoint routes (each unit offset by a column), the third has no orders. Round 5 cripples a unit on a route, round 8 clears one route. Orders in routes-50x50.orders.
map: unofficial/Cakefish/General/50x50 Grass A Dam'd Shame.board

options:
  file: bot-orders-options.xml

factions:
  - name: Observer
    team: 9

  - name: Lyran
    team: 1
    units:
      - fullname: Marauder MAD-3R
        id: 101
        at: [10, 47]
        facing: 0
      - fullname: Archer ARC-2R
        id: 102
        at: [11, 47]
        facing: 0
      - fullname: Warhammer WHM-6R
        id: 103
        at: [12, 47]
        facing: 0
      - fullname: Rifleman RFL-3N
        id: 104
        at: [13, 47]
        facing: 0
      - fullname: Wolverine WVR-6R
        id: 105
        at: [21, 48]
        facing: 0
      - fullname: Shadow Hawk SHD-2H
        id: 106
        at: [22, 48]
        facing: 0
      - fullname: Hunchback HBK-4G
        id: 107
        at: [23, 48]
        facing: 0
      - fullname: Enforcer ENF-4R
        id: 108
        at: [24, 48]
        facing: 0
      - fullname: Jenner JR7-D
        id: 109
        at: [31, 47]
        facing: 0
      - fullname: Commando COM-2D
        id: 110
        at: [32, 47]
        facing: 0
      - fullname: Phoenix Hawk PXH-1
        id: 111
        at: [33, 47]
        facing: 0
      - fullname: Stinger STG-3R
        id: 112
        at: [34, 47]
        facing: 0
    bot:
      ai: princess
      forcedwithdraw: true
      withdrawto: south
      flee: false
      fleeto: none

  - name: Clan Wolf
    team: 2
    units:
      - fullname: Black Hawk (Nova) Prime
        id: 201
        at: [10, 3]
        facing: 3
      - fullname: Hunchback IIC 2
        id: 202
        at: [11, 3]
        facing: 3
      - fullname: Fire Falcon A
        id: 203
        at: [12, 3]
        facing: 3
      - fullname: Grizzly
        id: 204
        at: [13, 3]
        facing: 3
      - fullname: Linebacker A
        id: 205
        at: [22, 3]
        facing: 3
      - fullname: Griffin IIC 3
        id: 206
        at: [23, 3]
        facing: 3
      - fullname: Shadow Hawk IIC 3
        id: 207
        at: [24, 3]
        facing: 3
      - fullname: Black Hawk (Nova) A
        id: 208
        at: [25, 3]
        facing: 3
      - fullname: Hunchback IIC 3
        id: 209
        at: [34, 3]
        facing: 3
      - fullname: Fire Falcon B
        id: 210
        at: [35, 3]
        facing: 3
      - fullname: Linebacker B
        id: 211
        at: [36, 3]
        facing: 3
      - fullname: Griffin IIC 4
        id: 212
        at: [37, 3]
        facing: 3
    bot:
      ai: princess
      forcedwithdraw: true
      withdrawto: north
      flee: false
      fleeto: none
