MMSVersion: 2
name: Lowering the Boom
planet: Castor
description: >
  Lyran intelligence has found illegal atomic weapons on the Marik world of Castor. Katrina Steiner 
  has authorized an attack to remove the weapons and provide the Kell Hounds with action.

map:
  boardrows: 2
  boards:
    - file: Map Set 2/16x17 BattleTech.board
    - file: Map Set 2/16x17 Lake Area.board
      modify: rotate

factions:
- name: Thirtieth Marik Militia
  camo: Free Worlds League/Marik Militia/Marik Militia.jpg

  units:
  - fullname: Thunderbolt TDR-5S
    at: [ 12, 29 ]
    crew:
      name: Col. Oliver Nage
      piloting: 5
      gunnery: 5


  - fullname: Griffin GRF-1N
    at: [ 9, 32 ]
    crew:
      name: Maj. Abraham Morrison
      piloting: 4
      gunnery: 4

  - fullname: Hunchback HBK-4G
    at: [ 16, 29 ]
    crew:
      name: Lt. Alicia Devon
      piloting: 4
      gunnery: 4

  - fullname: Centurion CN9-A
    at: [ 14, 31 ]
    crew:
      name: Sgt. Jonathan Taylor
      piloting: 4
      gunnery: 4

  - fullname: Hermes II HER-2S
    at: [ 5, 30 ]
    crew:
      name: Samantha Blaustein
      piloting: 4
      gunnery: 4

  - fullname: Javelin JVN-10N
    at: [ 2, 29 ]
    crew:
      name: Deborah Ryan
      piloting: 4
      gunnery: 4

- name: Kell Hounds, First Battalion
  camo: MERC - 1st Kell Hounds.gif
  deploy: S

  units:
    - fullname: Wolverine WVR-6R
      deploymentround: 2
      crew:
        name: Maj. Salome Ward
        piloting: 4
        gunnery: 3

    - fullname: Shadow Hawk SHD-2H
      crew:
        name: Lee Kennedy
        piloting: 4
        gunnery: 4

    - fullname: Dervish DV-6M
      deploymentround: 2
      crew:
        name: Brian Martell
        piloting: 4
        gunnery: 4

    - fullname: Trebuchet TBT-5N
      deploymentround: 2
      crew:
        name: Judith Nesmith
        piloting: 4
        gunnery: 4

    - fullname: Phoenix Hawk PXH-1
      crew:
        name: Nathan Mack
        piloting: 4
        gunnery: 4

    - fullname: Phoenix Hawk PXH-1
      crew:
        name: Stuart O'Grady
        piloting: 4
        gunnery: 4

    - fullname: Jenner JR7-D
      crew:
        name: Sarah Jette
        piloting: 4
        gunnery: 4

messages:
  - header: Situation
    text: |
      # Situation
      ## Castor 
      ## Free Worlds League
      ## 7 June 3011
      
      The plan for a Steiner raid on the Marik world of Castor began in the mind of Cranston Snord, famous 
      eccentric and one of Katrina Steiner's longstanding mercenary commanders.  Snord's contacts had informed 
      him that a priceless collection of Fabergé Eggs was on Castor, and he already had a place staked out for 
      them in his private museum on Clinton.
      
      When Morgan and Patrick Kell heard of Snord's intentions, they saw an opportunity to give their fledgling 
      unit its first test in battle.  Because Lyran intelligence had found a cache of illegal atomic weapons on 
      Castor, Katrina Steiner approved the raid to take out the weapons, get Snord his eggs, and give the Kell 
      Hounds some action.
    # portrait:
    image: loweringboom_splash.png
    trigger:
      type: gamestart






