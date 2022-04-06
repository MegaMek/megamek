# User Data Folder
Written 11-JUN-2021
MekHQ version 0.49.2

This is a list of all supported files in the userdata directory. Any file not listed here has not been checked to ensure it works properly, and thus the userdata folder is not supported for them. Directories are especially likely to not work if you place them here.

The default file implementation is the userdata file overriding the core data file, while the default directory implementation is merge implemented with the userdata version overriding any duplicated files.

## General Suite Directories/Files:
### data/names/
factions/: This subdirectory holds faction-specific name generation files. The individual faction files are line-based merge implemented, so that you may override any line of the default file by writing the historical ethnic code in the userdata folder.
callsigns.csv: This file contains weighted callsigns used in the random callsign generator. This file is merge implemented, with callsigns duplicated in the userdata folder having the userdata weight instead of the default weight. The first line of this file must be the standard header of "Callsign,Weight".
femaleGivenNames.csv: This file contains weighted historical ethnic code organized name used in the random name generator. This file is merge implemented, with names duplicated in the userdata folder having the userdata weight instead of the default weight. The first line of this file must be the standard header of "Ethnic Code,Name,Weight".
historicalEthnicity.csv: This file contains historical ethnic codes and their names. This file is merge implemented, with duplicated codes overwritten by the value in the userdata file.
maleGivenNames.csv: This file contains weighted historical ethnic code organized names used in the random name generator. This file is merge implemented, with names duplicated in the userdata folder having the userdata weight instead of the default weight. The first line of this file must be the standard header of "Ethnic Code,Name,Weight".
surnames.csv: This file contains weighted historical ethnic code organized surnames used in the random name generator. This file is merge implemented, with names duplicated in the userdata folder having the userdata weight instead of the default weight. The first line of this file must be the standard header of "Ethnic Code,Name,Weight".

## MegaMek-specific Folders/Files:

## MegaMekLab-specific Directories/Files:

## MekHQ-specific Directories/Files:
### data/universe/
ranks.xml: This file contains custom rank systems. This is merge implemented, so that each rank system will be handled akin to the default MekHQ rank systems, with the default rank systems taking primary key priority on load.
