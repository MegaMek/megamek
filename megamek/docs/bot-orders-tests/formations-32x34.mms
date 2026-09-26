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
name: Bot Orders Formations 32x34 - Wedge and Echelon Right across a river
description: Two Lyran lances routed north across the river band. Command lance in a Wedge (spacing 2, contact HOLD) through deep water, Battle lance in an Echelon Right (spacing 2, contact BREAK). The river should make followers fold to a Column. A Clan Wolf pair waits in the north corners. Orders in formations-32x34.orders.
map: unofficial/pokefan548/32x34 Farmland.board

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
        at: [8, 30]
        facing: 0
      - fullname: Archer ARC-2R
        id: 102
        at: [9, 30]
        facing: 0
      - fullname: Warhammer WHM-6R
        id: 103
        at: [10, 30]
        facing: 0
      - fullname: Rifleman RFL-3N
        id: 104
        at: [11, 30]
        facing: 0
      - fullname: Wolverine WVR-6R
        id: 105
        at: [20, 30]
        facing: 0
      - fullname: Shadow Hawk SHD-2H
        id: 106
        at: [21, 30]
        facing: 0
      - fullname: Hunchback HBK-4G
        id: 107
        at: [22, 30]
        facing: 0
      - fullname: Enforcer ENF-4R
        id: 108
        at: [23, 30]
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
      - fullname: Fire Falcon A
        id: 201
        at: [3, 2]
        facing: 3
      - fullname: Hunchback IIC 2
        id: 202
        at: [29, 2]
        facing: 3
    bot:
      ai: princess
      forcedwithdraw: true
      withdrawto: north
      flee: false
      fleeto: none
