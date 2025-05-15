# Authors and Contributors

We are grateful for all the contributions we have received over the years and wanted to make sure we included all
possible ones.

## Original author

Ben Mazur <bmazur@sev.org>

## Current Maintainer

MegaMek GitHub Organization <https://github.com/MegaMek> with the main [MegaMek](https://megamek.org)

## How we generated this list

This list is taken from the API, filtered to just pull the login name and GitHub URL, sorted, then added here. The commands that were used to
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

## Contributors

Last updated: 2025-04-27

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
