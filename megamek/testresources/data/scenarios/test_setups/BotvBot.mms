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

