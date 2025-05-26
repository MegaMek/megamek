MMSVersion: 2
name: Short Range Arty Fire
planet: None
description: Test setup for arty fire range less than 17
map: unofficial/Lucho/50x50  Lagomortis.board

options:
  on:
    - friendly_fire
  off:
    - check_victory


factions:
  - name: Test Player

    units:

      - fullname: Mobile Long Tom Artillery LT-MOB-25
        at: [ 26, 30 ]
        facing: 0

      - fullname: Bulldog Medium Tank
        at: [ 20, 27 ]

      - fullname: Bulldog Medium Tank
        at: [ 27, 24 ]

      - fullname: Bulldog Medium Tank
        at: [ 12, 12 ]

      - fullname: Bulldog Medium Tank
        at: [ 16, 11 ]

      - fullname: Bulldog Medium Tank
        at: [ 18, 25 ]

      - fullname: Bulldog Medium Tank
        at: [ 15, 18 ]
