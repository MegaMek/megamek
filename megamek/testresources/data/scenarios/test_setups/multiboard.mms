MMSVersion: 2
name: Test Setup no 1 for multiple boards
planet: None
description: uses several boards
map:
  - file: Beginner Box/16x17 Grassland 1.board
    name: Grassland
    id: 0

  - file: Battle of Tukayyid Pack/32x17 Pozoristu Mountains (CW).board
    name: Mountain Pass
    id: 1

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
      - at: [ 24, 15 ]
        id: 5
    id: 2

  - file: buildingsnobasement/Flughafen 6.board
    name: Hangar
    id: 3

  - file: unofficial/Cakefish/General/50x50 Grass QRF Airbase.board
    name: Airport
    id: 5
    postprocess:
      # make lift off less easy :)
      - type: addterrain
        terrain: woods
        level: 1
        area:
          list:
            - [ 14, 22 ]
            - [ 13, 26 ]
            - [ 20, 25 ]
      - type: addterrain
        terrain: foliage_elev
        level: 1
        area:
          list:
            - [ 14, 22 ]
            - [ 13, 26 ]
            - [ 20, 25 ]

      - type: hexlevel
        level: 2
        area:
          list:
            - [ 23, 26 ]

  - type: space
    width: 15
    height: 15
    name: Orbit
    embed:
      - at: [ 1, 7 ]
        id: 2
    id: 6


options:
  on:
    - friendly_fire
  off:
    - aero_ground_move
    - check_victory
    - stratops_ecm

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

#  - fullname: Bulldog Medium Tank
#    at: [ 13,10 ]
#    board: 0
#    facing: 2

#  - fullname: Bulldog Medium Tank
#    at: [ 11,11 ]
#    board: 0
#    facing: 0
#
#  - fullname: Sprint Scout Helicopter (C3i)
#    at: [ 12,10 ]
#    board: 0
#    facing: 0
#    elevation: 3

#  - fullname: Locust LCT-1V
#    board: 0
#    at: [ 4,4 ]
#    status: hidden
#  - fullname: Sai S-4
#    at: [ 15, 31 ]
#    board: 5
#    facing: 0
#    altitude: 0
#    status:
#      - shutdown doesnt work well on grounded Aero

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
#    board: 6
#    facing: 2
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
#
#  - fullname: Cheetah IIC
#    at: [ 8, 2 ]
#    board: 2
#    facing: 3
#    altitude: 1

#  - fullname: AC/10 Turret (Dual)

#  - fullname: Aurora
#    at: [24, 7]
#    board: 5
#    altitude: 0
#
#  - fullname: Zugvogel Omni Support Aircraft B
#    at: [ 24, 15 ]
#    board: 2
#    facing: 5
#    altitude: 1

    - fullname: Cheetah IIC
      at: [ 24, 15 ]
      board: 2
      facing: 3
      altitude: 1
#
#  - fullname: Skulker Wheeled Scout Tank C
#    at: [ 23, 18 ]
#    board: 5
#    facing: 0

#
#  - fullname: Union (3055)
#    at: [ 25, 13 ]
#    board: 5
#    facing: 4
#    altitude: 0

#  - fullname: Mobile Long Tom Artillery LT-MOB-25
#    at: [ 30, 24 ]
#    board: 5
#    facing: 5
