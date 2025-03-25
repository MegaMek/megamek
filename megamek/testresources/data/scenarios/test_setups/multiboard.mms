MMSVersion: 2
name: Test Setup for multiple boards
planet: None
description: uses two boards
map:
  - file: Beginner Box/16x17 Grassland 1.board
  - file: Battle of Tukayyid Pack/32x17 Pozoristu Mountains (CW).board

options:
  off:
    - check_victory

factions:
- name: P1

  units:
  - fullname: Cyclops CP-10-Z
    at: [ 9, 12 ]
    facing: 0
    crew:
      gunnery: 0

