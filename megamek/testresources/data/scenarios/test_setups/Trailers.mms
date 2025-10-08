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
name: Test Setup for Trailers
planet: None
description: Some small and large tractors and trailers to pick up and a Galleon to shoot at them
map: buildingsnobasement/dropport2.board

options:
  off:
    - check_victory

factions:
- name: Test Player

  units:
  - fullname: J-27 Ordnance Transport
    at: [ 9, 11 ]


  - fullname: J-27 Ordnance Transport (Trailer)
    at: [ 9, 8 ]

  - fullname: J-27 Ordnance Transport (Trailer)
    at: [ 9, 8 ]

  - fullname: J-27 Ordnance Transport (Trailer)
    at: [ 9, 8 ]


  - fullname: J-27 Ordnance Transport (Trailer)
    at: [ 9, 9 ]

  - fullname: J-27 Ordnance Transport (Trailer)
    at: [ 9, 9 ]

  - fullname: J-27 Ordnance Transport (Trailer)
    at: [ 9, 10 ]

  - fullname: Assassin ASN-21
    at: [ 13, 6 ]
    facing: 4

#  - fullname: Galaport Ground Tug
#    at: [ 11, 11 ]
#
#  - fullname: Galaport Ground Trailer
#    at: [ 11, 12 ]
