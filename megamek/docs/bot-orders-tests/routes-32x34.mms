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
name: Bot Orders Routes 32x34 - lance routes around water and woods
description: A Lyran lance gets multi-waypoint routes on a mixed woods, hills and lake board; one route crosses shallow water, one detours around the lake. Round 5 clears one route, round 6 extends another. A light Clan Wolf pair starts on the north hills. Orders in routes-32x34.orders.
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
        at: [22, 31]
        facing: 0
      - fullname: Jenner JR7-D
        id: 102
        at: [24, 30]
        facing: 0
      - fullname: Marauder MAD-3R
        id: 103
        at: [20, 32]
        facing: 0
      - fullname: Archer ARC-2R
        id: 104
        at: [22, 33]
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
        at: [16, 1]
        facing: 3
      - fullname: Black Hawk (Nova) Prime
        id: 202
        at: [19, 1]
        facing: 3
    bot:
      ai: princess
      forcedwithdraw: true
      withdrawto: north
      flee: false
      fleeto: none
