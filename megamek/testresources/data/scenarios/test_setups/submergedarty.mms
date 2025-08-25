MMSVersion: 2
name: Submerged Arty Fire
planet: None
description: Test setup for arty fire from a submarine
map: unofficial/Cakefish/General/40x40 Arid Boarding Call.board

options:
  on:
    - friendly_fire
  off:
    - check_victory


factions:
  - name: Test Player

    units:

      - fullname: Test Cruise Missile Sub
        at: [ 22, 37 ]
        elevation: -2

      - fullname: New Arrow Sub
        at: [ 24, 37 ]
        elevation: -2

      - fullname: Test Cruise Missile Sub
        at: [ 21, 36 ]
        elevation: 0

      - fullname: New Arrow Sub
        at: [ 23, 36 ]
        elevation: 0

