MMSVersion: 2
name: Test Setup for multiple boards
planet: None
description: uses two boards
map:
  - file: Beginner Box/16x17 Grassland 1.board
    name: Grassland
#    id: 6
  - file: Battle of Tukayyid Pack/32x17 Pozoristu Mountains (CW).board
    name: Mountain Pass
#    id: 8
  - type: sky
    width: 35
    height: 20
    name: Sky
    embed:
      - at: [ 5, 5 ]
        id: 0
      - at: [ 8, 9 ]
        id: 1
      - at: [ 31, 11 ]
        id: 3
#    id: 12
  - file: buildingsnobasement/Flughafen 6.board
    name: Airport


options:
  off:
    - check_victory

factions:
- name: P1
#  deploy: W
#  deploy:
#    area:
#      terrain:
#        type: woods
#        maxdistance: 1
#        boards: 1
  deploy:
    area:
      border:
        edges: [ east, north ]
        # optional: the minimum distance from the edge; 0 means start at the edge hexes
        mindistance: 2
        # optional: the maximum distance from the edge
        maxdistance: 3

  units:
  - fullname: Mobile Long Tom Artillery LT-MOB-25
    offboard: NORTH
    distance: 200
    board: 1 #makes no diff yet
    # offboard effective distance doesnt work yet

  - fullname: Mobile Long Tom Artillery LT-MOB-25
    at: [13,10]
    board: 1
    facing: 2

  - fullname: Locust LCT-1V
    board: 0
    at: [ 4,4 ]

#  - fullname: Atlas AS7-D

#  - fullname: Cheetah IIC
#    at: [12, 9]
#    board: 12
#    facing: 2
#    altitude: 3
