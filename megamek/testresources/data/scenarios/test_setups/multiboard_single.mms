MMSVersion: 2
name: Test Setup no 2 for multiple boards
planet: None
description: uses a single board of ID != 0 (multiboard requires this to no longer break anywhere)
map:
  - file: unofficial/Cakefish/General/50x50 Grass QRF Airbase.board
    name: Airport
    id: 5
#    postprocess:
#      # addterrain adds terrain in a given area
#      - type: addterrain
#      # required: the terrain type to change, as used in board files
#        terrain: fire
#      # required: the terrain level
#        level: 1
#      # required: an area to apply it to, see "areas" section
#        area:
#          list:
#          - [ 9, 9 ]

options:
  on:
    - friendly_fire
    - tacops_start_fire
  off:
    - check_victory
    - aero_ground_move

#planetaryconditions:                        # default: standard conditions
#  wind:
#    strength: moderate gale                 # default: none; other values: light gale, moderate gale, strong gale
#    direction: NW                           # default: random; other values: N, NE, SE, S, SW, NW
#    shifting: no                            # default: no


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

#  - fullname: Black Knight BL-12-KNT
#    board: 5
#    at: [ 35, 29 ]


#  - fullname: Scorpion Light Tank (Armor)
#    at: [ 6,12 ]
#    board: 5
#    facing: 2
#    crew:
#      name: Eddie the Eagle
#      piloting: 8

#  - fullname: Zugvogel Omni Support Aircraft B
#    at: [ 8,20 ]
#    board: 5
#    altitude: 2

#  - fullname: Locust LCT-1V
#    board: 0
#    at: [ 4,4 ]
#    status: hidden

#  - fullname: Grasshopper GHR-5N
#    at: [ 27, 22 ]
#    board: 5
#    facing: 3

#  - fullname: Atlas AS7-D
#    at: [ 26, 28 ]
#    board: 5
#    facing: 3
#    status: hidden
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
    at: [ 21, 21 ]
    board: 5
    facing: 5
    altitude: 0

#  - fullname: FWL Advanced Laser Turret (3075)
#    at: [37,26]
#    elevation: 1
#    board: 5

#  - fullname: Mobile Long Tom Artillery LT-MOB-25
#    at: [ 30, 24 ]
#    board: 5
#    facing: 5
