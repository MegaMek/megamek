MMSVersion: 2
name: Test Setup for Conventional Infantry
planet: None
description: Various conventional infantry on a board with varied terrain for testing digging in etc
map:
  - file: unofficial/Cakefish/General/50x50 Grass QRF Airbase.board

options:
  on:
    - friendly_fire
    - tacops_dig_in
    # apparently "hitting the deck" doesnt exist yet
  off:
    - check_victory

factions:
- name: Player

  units:
  - fullname: Foot Platoon (MG)
    at: [ 10, 12 ]

  - fullname: Mechanized Hover Platoon (Rifle)
    at: [ 17, 26 ]

  - fullname: Beast Infantry (Camel) (Auto-Rifle)
    at: [ 21, 20 ]

  - fullname: Jump Platoon (Flamer)
    at: [ 29, 23 ]

  - fullname: Motorized Squad (Rifle)
    at: [ 36, 18 ]

  - fullname: Clan Field Gun Point (Tracked AC2 Basic)
    at: [ 35, 24 ]

  - fullname: Field Artillery (Thumper)
    at: [ 20, 22 ]
