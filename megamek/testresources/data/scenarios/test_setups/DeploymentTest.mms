MMSVersion: 2
name: Choosing Deployment Elevation
planet: None
description: Different units to deploy on a map with some iced-over water and other features to test deployment
map: testiceonwater.board

options:
  on:
    - stratops_quirks

factions:
  - name: Test Player

    units:
#      - fullname: Intruder (3056)
#      - fullname: Ares Assault Craft Mk.III
#      - fullname: Phoenix Hawk LAM PHX-HK2M
      - fullname: Blade BLD-XS
        modifiers:
          RA:
            - slot: 3
              modifiers:
                type: jam
                on: [ 2,3,4,5,6,7,8,9]
            - slot: 10
              modifiers:
                # the modifiers can be a list or a single entry
                # misfire
                - type: misfire
                  # The roll results to misfire on, must always be a list, not a single number
                  on: [ 2, 3 ]
                # heat adds the given delta to the weapon heat
                - type: heat
                  delta: 2
                # damage obviously adds the given delta to the weapon's damage
                - type: damage
                  delta: -1
                # tohit changes the tohit target number by the given delta
                - type: tohit
                  delta: 1
#      - fullname: Vedette Medium Tank
#      - fullname: Mauna Kea Command Vessel
#      - fullname: Moray Heavy Attack Submarine
#      - fullname: Cavalry Attack Helicopter
#      - fullname: Ahab AHB-443
#      - fullname: Cephalus U
#      - fullname: Undine Battle Armor (Sqd5)
#      - fullname: Frogmen Blue Water Marine Response Teams (Frogmen)
#      - fullname: Silverback Coastal Cutter
#      - fullname: Bandit Hovercraft G
#      - fullname: Invader Jumpship (2631) (LF)
#      - fullname: Fensalir Combat WiGE
#      - fullname: Foot Platoon (MG)

