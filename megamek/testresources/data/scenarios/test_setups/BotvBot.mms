# MegaMek Data (C) 2025 by The MegaMek Team is licensed under CC BY-NC-SA 4.0.
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
name: Bot vs Bot as Scenario
description: A test fight bot vs bot
map: AGoAC Maps/16x17 Grassland 2.board

factions:
  - name: Observer

  - name: Legion of Vega
    units:
      - fullname: Atlas AS7-D
        id: 101
      - fullname: Locust LCT-1M
        id: 102
    deploy: N

  - name: 2nd Air Cavalry, Federated Suns
    units:
      - fullname: Atlas AS7-D
        id: 201
      - fullname: Locust LCT-1M
        id: 202
    deploy: S

events:
  - type: princesssettings
    event:
      trigger:
        type: killedunit
        unit: 102
      flee: true
      destination: north
      player: Legion of Vega

  - type: message
    event:
      trigger:
        type: killedunit
        unit: 102
        modify: once
      header: oops
      text: Locust dead. I'm running to the north!

