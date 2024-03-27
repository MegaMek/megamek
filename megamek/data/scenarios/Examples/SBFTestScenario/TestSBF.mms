MMSVersion: 2
gametype: SBF
name: SBF Test Scenario
planet: Braunschweig

description: >
  This test scenarion is written in V2 YAML

map: AGoAC Maps\16x17 Desert 2.board

planetaryconditions:
  light: dusk

factions:
- name: Legion of Vega
  deploy: N
  camo: Clans/Wolf/Alpha Galaxy/Alpha Galaxy.jpg

  units:
  - include: Noble's Company.mmu

- name: 1st Air Cavalry, Federated Suns
  deploy: "S"
  camo: Federated Suns\Arcadian Cuirassiers\Arcadian Cuirassiers.jpg
  units:
  - include: Romy's Company.mmu



