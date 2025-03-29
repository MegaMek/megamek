MMSVersion: 2
name: Test Setup for multiple boards
planet: None
description: uses two boards
map:
  - file: Beginner Box/16x17 Grassland 1.board
    name: Grassland
  - file: Battle of Tukayyid Pack/32x17 Pozoristu Mountains (CW).board
    name: Pozoristu Mountains
  - type: sky
    width: 35
    height: 20
    embed:
      - at: [ 5, 5 ]
        id: 0
      - at: [ 8, 9 ]
        id: 1
  - file: buildingsnobasement/Flughafen 6.board


options:
  off:
    - check_victory

factions:
- name: P1
#  deploy:
#    area:
#      terrain:
#        type: pavement
#        maxdistance: 1
#        boards: 1

  units:
  - fullname: Mobile Long Tom Artillery LT-MOB-25
    offboard: NORTH
    distance: 200

  - fullname: Locust LCT-1V

  - fullname: Atlas AS7-D
#    at: [8, 9]

