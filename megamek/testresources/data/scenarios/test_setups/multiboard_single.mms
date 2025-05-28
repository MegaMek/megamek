MMSVersion: 2
name: Test Setup no 2 for multiple boards
planet: None
description: uses a single board of ID != 0 (multiboard requires this to no longer break anywhere)
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

#  - file: unofficial/Cakefish/General/50x50 Grass QRF Airbase.board
#    name: Airport
#    id: 5

options:
  on:
  off:
    - check_victory

factions:
- name: Hooman

  deploy:
    area:
      terrain:
        type: woods
        maxdistance: 1
        boards: 3

  units:
    - fullname: Grasshopper GHR-5N
#      at: [ 13, 12 ]
      board: 3
#      facing: 0

#    - fullname: Atlas AS7-D
#      at: [ 15, 13 ]
#      board: 3
#      facing: 0

    - fullname: Sai S-4
      at: [ 5, 5 ]
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
