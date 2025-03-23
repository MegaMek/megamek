MMSVersion: 2
name: Test Setup for MMJB
planet: None
description: Units with MMJB and standard JJ
map: Map Pack Savannahs/16x17 River Delta-Drainage Basin 1 (Savannah).board

options:
  off:
    - check_victory
  on:
    - friendly_fire

factions:
- name: Test Player

  units:
  - fullname: Thunder Fox TFT-C3
    # only MMJB
    at: [ 9, 12 ]

  - fullname: Thunder Fox TFT-L8
    # standard JJ
    at: [ 9, 8 ]

  - fullname: Commando COM-MMJB
    # both JJ and MMJB
    # the mtf file must be moved so that it's part of the cache for this to work
    at: [ 10, 11 ]
