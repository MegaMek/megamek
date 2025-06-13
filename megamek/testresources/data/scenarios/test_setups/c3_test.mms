MMSVersion: 2
name: C3 Test
planet: None
description: Some units with a C3 setup
map:
  - file: Beginner Box/16x17 Grassland 1.board
    name: Grassland
    id: 3

  - type: space
    width: 35
    height: 20
    name: Space
    id: 5

options:
  on:
  off:
    - check_victory

factions:
- name: Human

  units:
    - fullname: Phoenix Hawk LAM PHX-HK1RB
      board: 3
      at: [ 4, 5 ]
      id: 101

    - fullname: Phoenix Hawk LAM PHX-HK1RB
      board: 3
      at: [ 4, 6 ]
      id: 102

    - fullname: Phoenix Hawk LAM PHX-HK1RB
      board: 3
      at: [ 5, 8 ]
      id: 103

    - fullname: Phoenix Hawk LAM PHX-HK1RB
      board: 3
      at: [ 10, 10 ]
      id: 104

    - fullname: Achilles (3088)
      board: 5
      at: [ 4, 5 ]
      id: 111

    - fullname: Achilles (3088)
      board: 5
      at: [ 4, 6 ]
      id: 112

    - fullname: Achilles (3088)
      board: 5
      at: [ 5, 8 ]
      id: 113

    - fullname: BattleMaster BLR-K3
      board: 3
      at: [ 10, 11 ]
      id: 201

    - fullname: Beowulf BEO-14
      board: 3
      at: [ 10, 12 ]
      id: 202

    - fullname: Beowulf BEO-14
      board: 3
      at: [ 10, 13 ]
      id: 203

    - fullname: Beowulf BEO-14
      board: 3
      at: [ 10, 14 ]
      id: 204

    - fullname: Beowulf BEO-14
      board: 3
      at: [ 10, 15 ]
      id: 205

    - fullname: BattleMaster BLR-K3
      board: 3
      at: [ 11, 12 ]
      id: 208


    - fullname: BattleMaster BLR-K3
      board: 3
      at: [ 13, 12 ]
      id: 301

    - fullname: BattleMaster BLR-K3
      board: 3
      at: [ 13, 14 ]
      id: 302

c3:
  - [ 101,102,103 ]
  - [ 111,112,113 ]
  - c3m: 208
    connected: [ 202, 203 ]
  - c3m: 301
    connected: [ 302, 208 ]
