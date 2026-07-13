# MegaMek Data (C) 2026 by The MegaMek Team is licensed under CC BY-NC-SA 4.0.
# To view a copy of this license, visit https://creativecommons.org/licenses/by-nc-sa/4.0/
#
# NOTICE: The MegaMek organization is a non-profit group of volunteers
# creating free software for the BattleTech community.
#
# MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
# of The Topps Company, Inc. All Rights Reserved.
#
# Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
# InMediaRes Productions, LLC.
#
# MechWarrior Copyright Microsoft Corporation. MegaMek Data was created under
# Microsoft's "Game Content Usage Rules"
# <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
# affiliated with Microsoft.
MMSVersion: 2
name: Princess vs CASPAR
description: A Princess-versus-CASPAR match, for use with the AiMatchRunner evaluation harness. The bot faction's AI is chosen with the 'ai:' key inside its 'bot:' block.
map: AGoAC Maps/16x17 Grassland 2.board

factions:
  - name: Observer

  - name: Princess Team
    units:
      - fullname: Atlas AS7-D
        id: 101
      - fullname: Locust LCT-1M
        id: 102
    deploy: N
    bot:
      ai: princess

  - name: CASPAR Team
    units:
      - fullname: Atlas AS7-D
        id: 201
      - fullname: Locust LCT-1M
        id: 202
    deploy: S
    bot:
      ai: caspar
