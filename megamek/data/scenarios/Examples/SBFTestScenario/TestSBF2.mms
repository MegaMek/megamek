MMSVersion: 2
gametype: SBF
name: SBF Single Side Test Scenario
planet: Braunschweig

description: >
  This test scenario is written in V2 YAML. Only one player

map: AGoAC Maps/16x17 Grassland 2.board

factions:
- name: Legion of Vega
  camo: Clans/Wolf/Alpha Galaxy/Alpha Galaxy.jpg

  units:
    - include: Noble's Company.mmu
      at: [7, 5]
      id: 3

    - include: "JumpCompany.mmu"
      at: [ 10, 4 ]

    - include: "Formation2.mmu"
      at: [ 8, 3 ]
