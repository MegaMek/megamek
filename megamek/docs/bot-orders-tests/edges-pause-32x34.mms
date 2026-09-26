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
name: Bot Orders Edges, Pause and Stop 32x34
description: Five Lyran units each get a different order - move to the west edge and hold, exit by the east edge, a route paused in round 2 and resumed in round 4, a route stopped in round 2, and a route with ordered facings. A Clan Wolf trio starts on the south edge. Orders in edges-pause-32x34.orders (needs the unit orders model).
map: unofficial/pokefan548/32x34 Farmland.board

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
        at: [12, 21]
        facing: 0
      - fullname: Jenner JR7-D
        id: 102
        at: [14, 21]
        facing: 0
      - fullname: Marauder MAD-3R
        id: 103
        at: [16, 21]
        facing: 0
      - fullname: Archer ARC-2R
        id: 104
        at: [18, 21]
        facing: 0
      - fullname: Phoenix Hawk PXH-1
        id: 105
        at: [20, 21]
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
        at: [12, 33]
        facing: 0
      - fullname: Fire Falcon A
        id: 202
        at: [16, 33]
        facing: 0
      - fullname: Black Hawk (Nova) Prime
        id: 203
        at: [20, 33]
        facing: 0
    bot:
      ai: princess
      forcedwithdraw: true
      withdrawto: south
      flee: false
      fleeto: none
