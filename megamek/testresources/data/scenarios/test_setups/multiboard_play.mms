# MegaMek Data (C) 2025 by The MegaMek Team is licensed under CC BY-NC-SA 4.0.
# To view a copy of this license, visit https://creativecommons.org/licenses/by-nc-sa/4.0/
#
# NOTICE: The MegaMek organization is a non-profit group of volunteers
# creating free software for the BattleTech community.
#
# MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
# of The Topps Company, Inc. All Rights Reserved.
#
# Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
# InMediaRes Productions, LLC.
#
# MechWarrior Copyright Microsoft Corporation. MegaMek Data was created under
# Microsoft's "Game Content Usage Rules"
# <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
# affiliated with Microsoft.
MMSVersion: 2
name: Playtest sandbox setup for multiple boards
planet: None
description:
  For playing around with multiple boards. Does not use aero on ground move. Orbital Bombardment limited 
  to Capital Laser Bays. Crossing between space and atmo not possible. Lift-off and landing possible. Strafing from atmo
  possible. xboard artillery fire possible. No Princess, will get confused atm. For testing, space units may act every 
  round.
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

  - type: highaltitude
    width: 20
    height: 20
    name: Orbit
    embed:
      - at: [ 1, 7 ]
        id: 2
    id: 6

#  - type: space
#    width: 25
#    height: 25
#    name: Interplanetary
#    id: 8


options:
  on:
    - friendly_fire
  off:
    - aero_ground_move
    - check_victory
    - stratops_ecm

#planetaryconditions:
#  pressure: very high

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
  - fullname: Mobile Long Tom Artillery LT-MOB-25
    at: [5,5]
    board: 1
    facing: 2

  - fullname: Atlas AS7-D #Bulldog Medium Tank
    at: [ 13,10 ]
    board: 5
    facing: 2

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

  - fullname: Sai S-4
    at: [ 15, 31 ]
    board: 5
    facing: 0
    altitude: 0
#    status:
#      - shutdown doesnt work well on grounded Aero

  - fullname: Bulldog Medium Tank #Atlas AS7-D
    board: 5
    at: [ 26,14 ]
    facing: 5
#    status: hidden
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
  - fullname: Cheetah IIC
    at: [8, 12]
    board: 2
    facing: 0
    altitude: 5

  - fullname: Cheetah IIC
    at: [ 8, 2 ]
    board: 2
    facing: 3
    altitude: 5
#
#  - fullname: Cheetah IIC
#    at: [ 24, 14 ]
#    board: 2
#    facing: 3
#    altitude: 2

#  - fullname: AC/10 Turret (Dual)

#  - fullname: Aurora
#    at: [10, 10]
#    board: 6
#    altitude: 0
#
#  - fullname: Zugvogel Omni Support Aircraft B
#    at: [ 24, 15 ]
#    board: 2
#    facing: 5
#    altitude: 1

#    - fullname: King Karnov Transport KC-6
#      at: [ 24, 15 ]
#      board: 2
#      facing: 3
#      altitude: 1

#  - fullname: Skulker Wheeled Scout Tank C
#    at: [ 23, 18 ]
#    board: 5
#    facing: 0

#
#  - fullname: Union (3055)
#    at: [ 25, 13 ]
#    board: 8
#    facing: 4
#
#  - fullname: Fox Corvette (3057)
#    at: [ 10, 12 ]
#    board: 8
#    facing: 5

  - fullname: Fox Corvette (3057)
    at: [ 10, 12 ]
    board: 6
    facing: 5
#    altitude: 0

#  - fullname: Mobile Long Tom Artillery LT-MOB-25
#    at: [ 30, 24 ]
#    board: 5
#    facing: 5
