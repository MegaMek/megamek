MMSVersion: 2
name: Test Setup no 2 for multiple boards
planet: None
description: uses a single board of ID != 0 (multiboard requires this to no longer break anywhere)
map:
  - file: unofficial/Cakefish/General/50x50 Grass QRF Airbase.board
    name: Airport
    id: 5

options:
  on:
    - friendly_fire
    - aero_ground_move
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
#  deploy:
#    area:
#      border:
#        edges: [ east, north ]
#        # optional: the minimum distance from the edge; 0 means start at the edge hexes
#        mindistance: 2
#        # optional: the maximum distance from the edge
#        maxdistance: 3

  units:
#  - fullname: Mobile Long Tom Artillery LT-MOB-25
#    offboard: NORTH
#    distance: 200
#    #makes no diff yet
#    board: 1
#    # offboard effective distance doesnt work yet
#
#  - fullname: Mobile Long Tom Artillery LT-MOB-25
#    at: [13,10]
#    board: 1
#    facing: 2

  - fullname: Bulldog Medium Tank
#    at: [ 13,10 ]
#    board: 1
#    facing: 2

#  - fullname: Locust LCT-1V
#    board: 0
#    at: [ 4,4 ]
#    status: hidden

#  - fullname: Atlas AS7-D
#    board: 1
#    at: [ 26,14 ]
#    facing: 5
##    status: hidden
#
#  - fullname: Brave BRV-1Y
#    board: 5
#    at: [ 35, 29 ]
#
#  - fullname: Cheetah IIC
#    at: [5, 6]
#    board: 2
#    facing: 2
#    altitude: 5
#
#  - fullname: Chippewa CHP-W7T
#    at: [ 20, 12 ]
#    board: 2
#    facing: 4
#    altitude: 5
#
#  - fullname: Cheetah IIC
#    at: [8, 12]
#    board: 0
#    facing: 0
#    altitude: 5
#
#  - fullname: Cheetah IIC
#    at: [ 8, 2 ]
#    board: 0
#    facing: 3
#    altitude: 5

  - fullname: Cheetah IIC
#    at: [ 8, 2 ]
#    board: 6
#    facing: 3

#  - fullname: AC/10 Turret (Dual)

#  - fullname: Mobile Long Tom Artillery LT-MOB-25
#    at: [ 30, 24 ]
#    board: 5
#    facing: 5
