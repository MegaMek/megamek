MMSVersion: 2
name: A2G and G2A from the atmo map
planet: None
description:
  For playing around with A2G and G2A, incl bombing and strafing from the atmo map
map:
  - type: sky
    width: 35
    height: 20
    name: Sky
    embed:
      - at: [ 24, 15 ]
        id: 5
    id: 2

  - file: unofficial/Cakefish/General/50x50 Grass QRF Airbase.board
    name: Airport
    id: 5


options:
  on:
    - friendly_fire
  off:
    - aero_ground_move
    - check_victory
    - stratops_ecm

factions:
  - name: P1

    units:
        - fullname: Atlas AS7-D
          at: [ 13,10 ]
          board: 5
          facing: 2

        - fullname: Cheetah IIC
          at: [ 21, 14 ]
          board: 2
          facing: 2
          altitude: 5
          bombs:
            internal:
              HE: 2
              LG: 2
            external:
              CLUSTER: 1

