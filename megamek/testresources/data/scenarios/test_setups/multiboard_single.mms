MMSVersion: 2
name: Multiboard Playtest Scenario
planet: None
description: Your lone Grasshopper is attacked by two Grasshoppers. You have a Sai aerospace fighter to support your 
  mek.
map:
  - file: Beginner Box/16x17 Grassland 1.board
    name: Grassland
    id: 3

  - type: sky
    width: 35
    height: 20
    name: Sky
    embed:
      - at: [ 31, 11 ]
        id: 3
    id: 5

options:
  on:
  off:
    - check_victory

factions:
- name: Human

  deploy:
    area:
      terrain:
        type: woods
        maxdistance: 1
        boards: 3

  units:
    - fullname: Grasshopper GHR-5N
      board: 3

    - fullname: Sai S-4
      at: [ 30, 10 ]
      board: 5
      facing: 2
      altitude: 4


- name: Princess
  deploy: N
  units:
    - fullname: Grasshopper GHR-5N
      board: 3

    - fullname: Grasshopper GHR-5N
      board: 3
