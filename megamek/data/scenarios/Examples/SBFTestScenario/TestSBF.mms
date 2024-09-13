MMSVersion: 2
gametype: SBF
name: SBF Test Scenario
planet: Braunschweig

description: >
  This test scenario is written in V2 YAML

map: AGoAC Maps/16x17 Desert 2.board

options:
  #- base_recon
  #- base_team_vision

planetaryconditions:
  light: dusk

factions:
- name: Legion of Vega
  camo: Clans/Wolf/Alpha Galaxy/Alpha Galaxy.jpg

  units:
    - include: Noble's Company.mmu
      at: [7, 5]
      id: 3

    - include: "Example SBF Formation.mmu"
      at: [ 10, 4 ]

    - include: "Formation2.mmu"
      at: [ 8, 3 ]

    - include: Noble's Company.mmu
      at: [8, 5]
      id: 3

    - include: "Example SBF Formation.mmu"
      at: [ 4, 4 ]

    - include: Noble's Company.mmu
      at: [ 3, 3 ]



- name: OpFor
  camo: Draconis Combine/Alshain Avengers/11th Alshain Avengers.jpg
  units:
    - include: Romy's Company.mmu
      at: [8, 9]
      id: 7

    - include: "Example SBF Formation.mmu"
      at: [ 12, 3 ]

    - include: Noble's Company.mmu
      at: [ 11, 15 ]
