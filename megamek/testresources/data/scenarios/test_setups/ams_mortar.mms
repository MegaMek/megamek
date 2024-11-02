MMSVersion: 2
name: AMS vs Mortars
planet: None
description: AMS may not engage mortars - test setup
map: Beginner Box/16x17 Grassland 1.board

options:
  #off:
   # - auto_ams

factions:
  - name: Test Player

    units:
      #AMS
      - fullname: Atlas AS7-K
        at: [ 9, 7 ]
        facing: 3
      #Mortar and SRM
      - fullname: Minsk 2
        at: [ 9, 14 ]
        crew:
          gunnery: 0

