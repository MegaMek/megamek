Windchild's Guide to the User Data Folder
Written 21-Feb-2020 for Version 0.47.5


As far as I know this isn't documented anywhere, and I'm not sure where the best place would be to document it, but if there is a folder named "userdata" MM/MML/MHQ will look there first. So campaigns would be in userdata/campaigns, settings would be in userdata/mmconf, and customs would be in userdata/data/mechfiles. This allows user customized data to be transferred easily between installations. It does not support setting a specific directory outside the installation, but I don't see a reason to do it that way unless you're going to share the same data between different installations, which is probably not a great idea (assuming they are different versions).


/campaigns/
/data/boards/unofficial/
/data/forcegenerator/
/data/images/awards/
/data/images/camo/
/data/images/fluff/
/data/images/force/
/data/images/portraits/
/data/images/units/
/data/mechfiles/customunits/
/data/names/
/data/rat/
/data/scenarios/
/data/universe/
/data/universe/awards/
/data/universe/ratdata/
/savegames/
/mmconf/
/plugins/

Awards:
Camo:
Portraits:
Mech Files:
Fluff Images:
Force Images:
Unit Images:

Bug: Awards doesn't work properly
Bug: unitQuicksOverride.xml in mmconf doesn't work