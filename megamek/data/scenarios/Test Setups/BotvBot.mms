MMSVersion: 2
name: Bot vs Bot as Scenario
description: A test fight bot vs bot
map: AGoAC Maps/16x17 Grassland 2.board

factions:
- name: Observer

- name: Legion of Vega

  bot:
    herdmentality: 10
    selfpreservation: 10
    retreat: SOUTH

  units:
  - fullname: Atlas AS7-D
  - fullname: Locust LCT-1M

- name: 2nd Air Cavalry, Federated Suns
  units:
    - fullname: Atlas AS7-D
    - fullname: Locust LCT-1M


