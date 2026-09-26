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
name: Bot Orders Imperative vs Normal 32x34 - routes round a lake
description: Two pairs of identical Lyran units get the same waypoint on the far side of a lake, one Normal and one Imperative, while a Clan Wolf trio sits to the east. The direct line crosses deep water. Orders in imperative-vs-normal-32x34.orders (needs the unit orders model).
map: unofficial/SimonLandmine/32x34/32x34 European2.board

options:
  file: bot-orders-options.xml

factions:
  - name: Observer
    team: 9

  - name: Lyran
    team: 1
    units:
      - fullname: Wolverine WVR-6R
        id: 101
        at: [2, 33]
        facing: 0
      - fullname: Wolverine WVR-6R
        id: 102
        at: [3, 33]
        facing: 0
      - fullname: Shadow Hawk SHD-2H
        id: 103
        at: [4, 32]
        facing: 0
      - fullname: Shadow Hawk SHD-2H
        id: 104
        at: [5, 32]
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
      - fullname: Hunchback IIC 2
        id: 201
        at: [20, 24]
        facing: 4
      - fullname: Fire Falcon A
        id: 202
        at: [22, 26]
        facing: 4
      - fullname: Griffin IIC 3
        id: 203
        at: [24, 28]
        facing: 4
    bot:
      ai: princess
      forcedwithdraw: true
      withdrawto: east
      flee: false
      fleeto: none
