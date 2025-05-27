# Authors and Contributors

We are grateful for all the contributions we have received over the years and wanted to make sure we included all
possible ones.

## Original author

Ben Mazur <bmazur@sev.org>

## Current Maintainer

MegaMek GitHub Organization <https://github.com/MegaMek> with the main [MegaMek](https://megamek.org)

## How we generated this list

This list is taken from the API, filtered to just pull the login name and GitHub URL, sorted, then added here. The
commands that were used to
generate this list are as follows:

```bash
gh api -H "Accept: application/vnd.github+json"  -H "X-GitHub-Api-Version: 2022-11-28" '/repos/megamek/megamek/stats/contributors' > contributors.json
```

From this list, we used `irb` (Interactive Ruby) to process and output that is below:

```ruby
contrib = JSON.parse(File.read('contributors.json'))
filter = contrib.filter_map { |record| [record['author']['login'], record['author']['html_url']] unless record == nil || record['author'] == nil }
filter.sort_by { |user, _| user }.each { |user_name, url| puts "- #{user_name} <#{url}>\n" }
```

## Contributors Per GitHub API

Last updated: 2025-05-19

- AaronGullickson <https://github.com/AaronGullickson>
- Akjosch <https://github.com/Akjosch>
- Alarantalara <https://github.com/Alarantalara>
- Algebro7 <https://github.com/Algebro7>
- Arachnight <https://github.com/Arachnight>
- BLOODWOLF333 <https://github.com/BLOODWOLF333>
- BLR-IIC <https://github.com/BLR-IIC>
- BlindGuyNW <https://github.com/BlindGuyNW>
- Bonepart <https://github.com/Bonepart>
- Bronzite <https://github.com/Bronzite>
- BuckshotChuck <https://github.com/BuckshotChuck>
- Cakefish11 <https://github.com/Cakefish11>
- ChaoticInsanity <https://github.com/ChaoticInsanity>
- Cmdr-Riker1of3 <https://github.com/Cmdr-Riker1of3>
- DM0000 <https://github.com/DM0000>
- Dark-Hobbit <https://github.com/Dark-Hobbit>
- Dirk-c-Walter <https://github.com/Dirk-c-Walter>
- Dylan-M <https://github.com/Dylan-M>
- FreekDS <https://github.com/FreekDS>
- Graysho <https://github.com/Graysho>
- HammerGS <https://github.com/HammerGS>
- HeckfyEx <https://github.com/HeckfyEx>
- HoneySkull <https://github.com/HoneySkull>
- IanBellomy <https://github.com/IanBellomy>
- IllianiBird <https://github.com/IllianiBird>
- Krashner <https://github.com/Krashner>
- Kurios <https://github.com/Kurios>
- Lu9us <https://github.com/Lu9us>
- McStarley <https://github.com/McStarley>
- MeadHall <https://github.com/MeadHall>
- NickAragua <https://github.com/NickAragua>
- ObviousTech <https://github.com/ObviousTech>
- Qwertronix <https://github.com/Qwertronix>
- RAldrich <https://github.com/RAldrich>
- RexPearce <https://github.com/RexPearce>
- SBBurzmali <https://github.com/SBBurzmali>
- SJuliez <https://github.com/SJuliez>
- Saklad5 <https://github.com/Saklad5>
- Scoppio <https://github.com/Scoppio>
- Setsul <https://github.com/Setsul>
- Sleet01 <https://github.com/Sleet01>
- Taharqa <https://github.com/Taharqa>
- TenkawaBC <https://github.com/TenkawaBC>
- Thom293 <https://github.com/Thom293>
- TorrenFG <https://github.com/TorrenFG>
- UlyssesSockdrawer <https://github.com/UlyssesSockdrawer>
- WeaverThree <https://github.com/WeaverThree>
- Windchild292 <https://github.com/Windchild292>
- actions-user <https://github.com/actions-user>
- arlith <https://github.com/arlith>
- beerockxs <https://github.com/beerockxs>
- binaryspica <https://github.com/binaryspica>
- blah2355 <https://github.com/blah2355>
- cweisenborn <https://github.com/cweisenborn>
- dependabot[bot] <https://github.com/apps/dependabot>
- dericpage <https://github.com/dericpage>
- duckmayr <https://github.com/duckmayr>
- elementx54 <https://github.com/elementx54>
- exeea <https://github.com/exeea>
- firefly2442 <https://github.com/firefly2442>
- fmoody <https://github.com/fmoody>
- gcoopercos <https://github.com/gcoopercos>
- giorgiga <https://github.com/giorgiga>
- iamtextbased <https://github.com/iamtextbased>
- jauby <https://github.com/jauby>
- juk0de <https://github.com/juk0de>
- kuronekochomusuke <https://github.com/kuronekochomusuke>
- luiges90 <https://github.com/luiges90>
- mangofeet <https://github.com/mangofeet>
- mchausse <https://github.com/mchausse>
- mhjacks <https://github.com/mhjacks>
- mjog <https://github.com/mjog>
- mkdillard <https://github.com/mkdillard>
- mkerensky <https://github.com/mkerensky>
- nderwin <https://github.com/nderwin>
- neoancient <https://github.com/neoancient>
- nutritiousemployee <https://github.com/nutritiousemployee>
- pakfront <https://github.com/pakfront>
- pavelbraginskiy <https://github.com/pavelbraginskiy>
- pheonixstorm <https://github.com/pheonixstorm>
- pokefan548 <https://github.com/pokefan548>
- psikomonkie <https://github.com/psikomonkie>
- ramgarden <https://github.com/ramgarden>
- repligator <https://github.com/repligator>
- rjhancock <https://github.com/rjhancock>
- sagnam <https://github.com/sagnam>
- savanik <https://github.com/savanik>
- sensualcoder <https://github.com/sensualcoder>
- simon987 <https://github.com/simon987>
- sixlettervariables <https://github.com/sixlettervariables>
- sldfgunslinger2766 <https://github.com/sldfgunslinger2766>
- stonewall072 <https://github.com/stonewall072>
- vizax <https://github.com/vizax>
- wildj79 <https://github.com/wildj79>

## Map Conversion

- Lars "Shockmilk" Kreilkamp <shockmilk@users.sourceforge.net>
- Pierre Savard <xsurrealx@users.sourceforge.net>
- shockmilk <shockmilk@users.sourceforge.net>
- Jan Ferdinand Rehse
- Thomas "DarkViper" Wehner
- Kolja "Flashhawk" Geldmacher
- Endreffy Janos <endrosz@freemail.hu>
- Blood Falcon <blood_falcon@users.sourceforge.net>
- Jayof9s
- DarkISI
- Rovnek
- Aokarasu
- Ratach
- Razorback
- Mike V
- Dervishx5 (Jihad MapPack and Derv MapPack)
- DancefloorLandmine
- arekP64
- Ask
- Keyl Seuhy
- Norman "Flynn" Zinner
- Kaji
- UlyssesSockdrawer
- Stonewall072
- Ashnod
- Pokefan548
- MadCartographer
- Jacob "Lanceman" Wall

## Mek, Vehicle, Protomek and Infantry Conversion and QA

- Hopalong
- Martis Truesight
- Taerkar
- coralbeach
- Robert Heath
- Alexander Schölling
- fastsammy
- Jacob "Lanceman" Wall
- Sni77
- Jellico
- Brock Peterman
- DarkISI
- Dave Nawton
- Phoenixstorm
- Greekfire
- SuperStucco

## Lead Sprite Artist

Alex "Deadborder" Fauth

## Various Images(including Sprites):

- Nehal Mistry
- Paolo D'Inca
- Ian Hamilton
- Charlie Brown <Cyric27@users.sourceforge.net>
- <rakkasan@users.sourceforge.net>
- Blazindizzy
- Kurt Aronnax Kajal<kurt_kajal@users.sourceforge.net>
- Martis Truesight <carpe_mortis@users.sourceforge.net>
- Philip <rancherion@users.sourceforge.net>
- Jeff Wrbelis <starcaptaintorc@users.sourceforge.net>
- Fritz Ullmann <brandfuchs@users.sourceforge.net>
- Brandon Robertson <majortom88@users.sourceforge.net>
- James Lacy <freakboy2571@users.sourceforge.net>
- Kolja Geldmacher <flashhawk2k@users.sourceforge.net>
- Carlo Sta. Romana <marwynn@users.sourceforge.net>
- Adam Lovatt <ekm@users.sourceforge.net>
- Jan Hansen <felsen@users.sourceforge.net>
- Brock Peterman <bh-21@users.sourceforge.net>
- R. Newton <anubaji@users.sourceforge.net>
- Jan Rzadkowski <st0ny@users.sourceforge.net>
- Santander
- Ploppy
- Alan Bell
- victor23
- beretboy <dp777@users.sourceforge.net>
- Walter Smith <walter_smith@users.sourceforge.net>
- Shatara
- Zoltan 'Nyrond' Kollar
- Adgar76 <adgar76@users.sourceforge.net>
- archonlomendil@hotmail.com
- Punakettu
- Michael "Bedwyr" Mullins
- ladob
- ruff
- DarkISI
- Diezle
- walter@nebulastation.net
- Julio Prina <jjj.ladob@gmail.com>
- N.|.B.
- Sebastian "Demos" Seiml
- Swiftsure
- Stingr4y
- RJM
- SteveRestless
- Dervishx5
- Adam Werner
- Michael "Spartan10590" Rehrig
- Scintilla
- Michael Rehrig
- Bloodwolf
- SirMegaV
- Saxarba
- Ahne
- AskAgain

## Camo Pack

- SeaVea
- Colonel Sanders Light (Folders noted with CSL)
- Teerayne

## Splash Screen

- Florian "SpOoKy777" Mellies (https://www.deviantart.com/spooky777)

## Random Assignment Tables

- Harold "BATTLEMASTER" N. <s_d@users.sourceforge.net>
- Nicholas "starbird7" Ristow
- Magnus Kerensky <mkerensky@users.sourceforge.net>
- Netzilla and Xotl (Classic Battletech Forums)
- Dave Nawton <hammer-gs@users.sourceforge.net>
- Greekfire.
- Bloodwolf
- SLDF_gunslinger

## Mek (Fluff) Data

From Sarna.net. A GNU FDL 1.2 Wiki, text compared to original TRO text for direct copy.

## Translations

- Jörg Harder <joergharder@t-online.de>
- Alexander Schölling <schoelling@users.sourceforge.net>
- Alexander "Twin_81" Manturov
- IsWas - Spanish

## MegaMek Developers, Emeritus

- James Lacy <freakboy2571@users.sourceforge.net>
- Sebastian Brocks <beerockxs@users.sourceforge.net>
- Nicholas Walczak <arlith@users.sourceforge.net>
- Deric "Netzilla" Page <dericpage@users.sourceforge.net>
- Dylan Myers <https://github.com/Dylan-M>
- Christopher Watford

# Icons and Images

- FatCow (http://www.fatcow.com), licensed under CC Attribution (http://creativecommons.org/licenses/by/3.0/)
    - data/images/hexes/note.png (modified for formatting, from http://www.iconspedia.com/icon/note-icon-21517.html)
      Wikimedia Commons, licensed under CC Attribution-Share Alike 2.5
      Generic (http://creativecommons.org/licenses/by-sa/2.5/deed.en)
    - data/images/widgets/hyades.jpg UI Materials licensed from EVILSYSTEM (http://www.evilsystem.eu/terms/).

- BigField (https://www.flickr.com/photos/wheatfields/557982013) Attribution-NonCommercial-ShareAlike 2.0 Generic (CC
  BY-NC-SA 2.0)
    - data/images/hexes/largeTextures/BigField.jpg Applied gimp make seamless filter, arrayed image into 4x4 tiles then
      shrunk to 800x600.
- BigMagma (https://sftextures.com/2015/05/27/lava-texture-with-small-red-stones/) Attribution-ShareAlike 4.0
  International License - data/images/hexes/largeTextures/BigMagma.jpg
- BigMud (https://commons.wikimedia.org/wiki/File:Mud_closeup.jpg) Creative Commons Attribution-Share Alike 3.0 Unported
  license. - data/images/hexes/largeTextures/BigMud.jpg
- BigSnow (https://pixabay.com/en/snow-texture-winter-background-1186174/) Creative Commons CC0 -
  data/images/hexes/largeTextures/BigSnow.jpg
- BigSpace (https://www.flickr.com/photos/mvannorden/8275081235) Creative Commons Attribution 2.0 Generic (CC BY
  2.0) https://creativecommons.org/licenses/by/2.0/ - Pixilated the image by three pixels then applied the Gimp "make
  seamless" map function for tiling, sized to 800x600 - data/images/hexes/largeTextures/BigSpace.jpg
- textureLunar (https://commons.wikimedia.org/wiki/File:M115693540_Jenner.png) This file is in the public domain in the
  United States because it was solely created by NASA. NASA copyright policy states that "NASA material is not protected
  by copyright unless noted." (See Template:PD-USGov, NASA copyright policy page or JPL Image Use Policy.) -
  data/images/hexes/largeTextures/textureLunar.jpg
- textureVolcano (https://pixabay.com/en/rock-pumus-volcanic-rock-1044181/) Creative Commons CC0 -
  data/images/hexes/largeTextures/textureVolcano.jpg
- BigGrass (https://www.flickr.com/photos/99624358@N00/4290929736) Creative Commons Attribution 2.0 Generic (CC BY
  2.0) https://creativecommons.org/licenses/by/2.0/ Applied the make seamless map function for tiling, arranged four of
  the images together then shrank to 800x600. - data/images/hexes/largeTextures/BigGrass.jpg
- BigWater (https://www.flickr.com/photos/g_kat26/4063560162) Creative Commons Attribution 2.0 Generic (CC BY
  2.0) https://creativecommons.org/licenses/by/2.0/ Copied image three times and arranged into a 1600x1200 image,
  pixelated by 3 px, applied the make seamless map function, and then shrank to 800x600 adjusting brightness and
  contrast for depths. - data/images/hexes/largeTextures/BigWater.jpg

## Megamek Portrait Pack

Base images purchased from Generated Photos (https://generated.photos/). Generated Photos carry no additional likeness
rights. All images have been generated without human intervention through machine learning processes. Any perceived
resemblance of our materials to an existing person is not intentional.

Base portraits modified by Saxarba (https://www.deviantart.com/quellion). Additional images used in portraits provided
with permission by SpOoKy777 (https://www.deviantart.com/spooky777/gallery)

## And Of Course...

FASA, WizKids, FanPro and Catalyst Game Labs for creating and publishing the original game of BattleTech, a source of
fun and inspiration to us all.

If I've missed anyone or details have changed, please accept apologies and contact megamekteam at gmail.com or any of
the lead developers
