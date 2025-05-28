MMSVersion: 2
name: Test Setup no 2 for multiple boards
planet: None
description: uses a single board of ID != 0 (multiboard requires this to no longer break anywhere)
map:
  - file: Beginner Box/16x17 Grassland 1.board
    name: Grassland
    id: 3

#  - file: unofficial/Cakefish/General/50x50 Grass QRF Airbase.board
#    name: Airport
#    id: 5

options:
  on:
  off:
    - check_victory

factions:
- name: Hooman
  deploy: S
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
    - fullname: Grasshopper GHR-5N
#      at: [ 13, 12 ]
      board: 3
#      facing: 0

#    - fullname: Atlas AS7-D
#      at: [ 15, 13 ]
#      board: 3
#      facing: 0


- name: Princess
  deploy: N
  units:
    - fullname: Grasshopper GHR-5N
#      at: [ 4, 4 ]
      board: 3
#      facing: 3

#    - fullname: Atlas AS7-D
#      at: [ 7, 3 ]
#      board: 3
#      facing: 3
