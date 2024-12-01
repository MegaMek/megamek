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
      - fullname: Stinger STG-3R
        id: 110
        force: 2nd Sword of Light|21||Zakahashi's Zombies|22||Light Lance|25
        modifiers:
          RT:
            - slot: 4 # JJ
              modifiers:
                type: heat
                delta: 1
        crew:
          name: John Parthan
          callsign: Wrinkles
          piloting: 5
          gunnery: 3
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

