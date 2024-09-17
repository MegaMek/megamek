MMSVersion: 2
name: Space Battle Test
description: Aero Fighter Test Setup on a space map

map:
  type: space
  width: 36
  height: 19

factions:
  - name: Epsilon Galaxy Flight
    camo: Clans/Coyote/Epsilon Galaxy.jpg

    units:
      - fullname: Cheetah F-11
        #        at: [2, 12]
        #        facing: 1
        #        altitude: 6
        #        velocity: 3
        crew:
          name: Marianne O'Brien
          gunnery: 4
          piloting: 4
          portrait: Female/Aerospace Pilot/ASF_F_2.png

      - fullname: Cheetah F-11
        #        at: [3, 17]
        #        facing: 1
        #        velocity: 2
        crew:
          name: Giulia DeMarco
          gunnery: 3
          piloting: 5
          portrait: Female/Aerospace Pilot/ASF_F_3.png

  - name: OpFor
    camo: Corporations/Star Corps.png
    units:
      - fullname: Cheetah F-11
        at: [ 32, 8 ]
        facing: 4
      - fullname: Cheetah F-11
        at: [ 34, 8 ]
        facing: 4