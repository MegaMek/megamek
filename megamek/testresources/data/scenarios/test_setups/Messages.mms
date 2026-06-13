# MegaMek Data (C) 2025 by The MegaMek Team is licensed under CC BY-NC-SA 4.0.
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
name: Test Setup for Messages and Triggers
planet: None
description: Various messages are shown via triggers
map: AGoAC Maps/16x17 Grassland 2.board

factions:
- name: Test Player

  units:
  - fullname: Hunchback HBK-4G
    at: [ 10, 10 ]

  - fullname: Atlas AS7-D
    at: [ 10, 11 ]
    elevation: -1

end:
  - trigger:
      type: roundend
      round: 4

messages:
  - header: Scenario Messages 1
    text: |
      In this test setup scenario, several messages are shown at various points of the game.

      This is supposed to be a test for the message system and the trigger system.
    trigger:
      type: gamestart

  - header: Scenario Messages 2
    text: Second test message
    trigger:
      type: gamestart

  - header: Scenario Messages 3
    text: Second test message
    trigger:
      type: gamestart

  - header: Round 2!
    text: This is a test message for the start of round 2.
    trigger:
      type: roundstart
      round: 2
      modify: once

  - header: Round 3 end!
    text: This is a test message for the end of round 3
    trigger:
      type: roundend
      round: 3
      modify: once
