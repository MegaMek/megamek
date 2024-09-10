MMSVersion: 2
name: Test Setup for Carryables
planet: None
description: A few units on a small map with a few carryable objects for testing; most objects predeployed, some to deploy
map: buildingsnobasement/dropport2.board

factions:
- name: Test Player

  units:
  - fullname: Locust LCT-1M
    at: [ 9, 10 ]

  - fullname: Hunchback HBK-4G
    at: [ 10, 10 ]

  - fullname: Charger CGR-1A1
    at: [ 11, 10 ]

  - fullname: Atlas AS7-D
    at: [ 10, 11 ]

  objects:
    - name: Test Paperweight (invulnerable)
      at: [ 14, 11 ]
      weight: 0.02
      status: invulnerable

    - name: Crate (can be damaged)
      weight: 1
      at: [ 3, 11 ]

    - name: This is medium weight (invulnerable)
      weight: 50
      at: [ 2, 2 ]
      status: invulnerable

    - name: This is massive (can be damaged)
      weight: 150
      at: [ 8, 15 ]

    - name: Deploy this (can be damaged)
      weight: 20

    - name: Deploy this 2 (invulnerable)
      weight: 3
      status: invulnerable
