
This readme has instructions for using the map editor.  It also contains 
information on the board file format, and the tileset file format.

  Introduction To The Map Editor
    You can access the map editor from the main menu.  When you start the
    editor, no map will be loaded.  You can use the buttons on the bottom right
    of the window to make a new, blank map, or open an existing map for editing.
    The default size for map boards in MegaMek is 16x17.  Most paper Battletech
    boards are about 15x17 hexes, but this does not allow adjacent boards to
    line up properly on the computer.

    Saving a map is accomplished with the Save or Save As buttons, right below
    the New and Load buttons.  The MegaMek server only checks files in the 
    data/boards directory, and with the .board extention, when determining which 
    boards to load.  This means you should add a .board extention to all your
    filenames when saving.
    
  Quick Reference:
  Left mouse button - paint terrain onto the board without changing elevation
  CTRL + left mouse - paint terrain onto the board and update elevation
  SHIFT + left mouse - add terrain from the work area to the board. (This is useful
                       to add roads to a map on top of existing terrain)
  ALT + left mouse - copy terrain from board to the work area

  The Hex Work Area
    Along the right side of the screen, above the save/load buttons, there are
    controls for working with a hex.  From top to bottom, there is a picture of
    your working hex, controls to adjust elevation, a list of terrain types
    present in the working hex, and controls to remove, add, or adjust terrain.

    The working hex exists in a zone outside the map.  To apply your hex to the
    map, hold the Ctrl key, then click the hex on the board you want to change.
    To load a hex from the board into the work area, hold the Alt key, then click
    the hex on the board you want to copy.  To make a small change to a single hex
    on the board, you will need to load the hex into the work area, edit it, and
    then place it back on the map.  For efficient use, use the working hex like 
    a paintbrush to change all the applicable terrain of one type before moving
    to another type of terrain.

  Editing A Hex
    Each hex has an elevation and may have terrain.  A clear hex has no terrain
    listed.  Most hexes with terrain features in them will only have one type
    of terrain feature, such as woods, or rough.  Some may have several, such 
    as woods with a road running through them, or a building on fire.

    The elevation controls are right under the picture of the current hex.  Use
    the buttons marked "U" or "D" or move the elevation up or down.  You may
    also just type the desired elevation in the text box.

    Under the elevation controls, there is a list of terrain features present
    in the working hex.  To remove a terrain feature, use the Remove Terrain
    button directly under the list.

    To add terrain, select the type of terrain you wish to add, the level
    associated with that type of terrain, and press the Add/Set Terrain button.
    The terrain will appear in the list in the format "terrain:level".  Each 
    individual type of terrain may only appear once in a hex.

    See the list below for details on the types of terrain and the levels that
    the game expects them at.

    The exits parameter is internally set by the game.  It is a 6-bit integer,
    with each bit representing a side of the hex, starting at the top and 
    proceeding clockwise around the edges.  A bit is set if the hex in that
    direction has the same type of terrain present.  There is usually no reason
    to change this value, but if you want to, check the Set Exits checkbox,
    and enter the appropriate value before you hit the Add/Set button.  You can
    also use the small button marked "A", which will pop up a dialog with a
    diagram of the exits and checkboxes for easy setting.  When you close this
    dialog, it automatically activates the Add/Set action.

    Normally, roads automatically exit onto all adjacent pavement hexes.  The
    designer of the board can override this by explicitly specifying the exits
    of the road hex (as described above).  On some boards, however, it is
    easier to specify the road hexes that *do* exit onto a pavement hex rather
    than saying which ones do *not* exit onto the pavement.  For those boards,
    uncheck the "Exit Roads to Pavement" checkbox.  Each time you check or un-
    check the "Exit Roads to Pavement" checkbox, the map refreshes itself to
    show you the results.  The if this checkbox is not checked when you save
    the board, the "exit_roads_to_pavement" option described below will be set
    to "false".  Please note that, when multiple boards are used in a game, if
    one board has the "exit_roads_to_pavement" option set to "false", *NO*
    roads will automatically exit onto a pavement hex, so use this option
    sparingly.

  Types Of Terrain Features
    The game expects the level for each type of terrain feature within a certain
    range.  Values not in this range may cause the terrain to appear on the map,
    but there will usually be no game effect, or, worse, undesirable effects.
    A lot of terrain is activated with a value of "1".

    Some types of terrain should only appear in combination with other types.
    The bldg_cf, bldg_elev, and bldg_basement should only appear in a hex that
    has a building level > 0.  The swamp terrain should only appear in a clear,
    rough, or woods hex.

    The "fluff" terrain type can be used to place special images on the map
    board that have no effect on play, but improve the overall appearance of
    the map.  The level of fluff in the terrain, coupled with the exits that
    are specified for the fluff, determine the image that appears.  The full
    list of fluff images available, and the level and exits needed to display
    them, can be seen in the file, "data/hexes/defaulthexset.txt".

    The terrain feature types and their expected values:

        woods: 1-2; 1 for light woods, 2 for heavy woods
        rough: 1
        rubble: 1-4; 1-4 corresponding to building types
        water: 0+; the hex elevation is the elevation for the surface of the 
            water, and the water level is the depth of the water
        pavement: 1
        road: 1
        fire: 1
        smoke: 1
        swamp: 1 
        building: 1-4; 1 = light ... 4 = hardened
        bldg_cf: 0-150; defaults to 15, 40, 90, or 120 if not specified
        bldg_elev: 1+; you must supply a number if a building is supplied.
        bldg_basement: 0-2; indicates depth, leaving this parameter out 
            indicates that the game should "roll" for the basement dynamically
        bridge: 1-4 (not functional in 0.26); 1 = light ... 4 = reinforced
        bridge_cf: 0-150; defaults to 15, 40, 90, or 120 if not specified
        bridge_elev: any; surface of the bridge, defaults to 0 if not present
        fluff: 1-9; see the notes above.
        arms: 1-2; blown off limbs
        legs: 1-2; blown off limbs
        ice: 1; combine with water to make "thin ice"
        fortified: 1
        geyser: 1-3; 1 is dormant, 2 is active, 3 is a magma vent
        magma: 1-2; 1 is crust, 2 is liquid
        jungle: 1-3; as woods
        mud: 1-2; normal/deep
        rapids: 1
        sand: 1
        snow: 1
        tundra: 1
        impassable: 1; this prevents units entering or deploying, e.g. for underground
        elevator: (any); each exit corresponds to a die roll it will move on, and the 
                         terrain level is the new elevation after it moves.
        
    Future additions may include ice, as well as some of the terrain present in
    the Maximum Tech sourcebook (jungle, magma, etc.)

The board file format is plain text.  You will probably not need to edit the
board files by hand.  This refrence is just for creating a different editor.

  The Board File Format
    As mentioned earlier, the server scans for files that have a .board
    extention, in the data/boards directory.

    Board files consist of a keyword, usually followed by several parameters.  
    The four keywords used are "size", "option", "hex", and "end".  Keywords
    should begin the line of text they are on.  Parameters are seperated by a
    space.  String parameters with a space in them, or empty string paramters
    can be quoted.  Comment lines are preceded by the hash (#) character.

  The "size" Keyword
    The size keyword is followed by the width and the height of the board, in
    hexes.  The default size is 16x17.  This would appear as "size 16 17".

  The "option" Keyword
    The option keyword is followed by the name of the option and the option's
    value for the board.  In v0.29, the only option recognized by MegaMek is
    exit_roads_to_pavement.  If this option is set to false, road hexes will
    not automatically exit into pavement hexes.  If a game's playing area uses
    more than one board, if any board sets this option to false, no road hexes
    on *any* board will automatically exit into pavement hexes.

  The "end" Keyword
    The end keyword indicates the end of data.  It has no parameters and appears
    on a line by itself.

  The "hex" Keyword    
    The board data is indicated by the hex keyword, which should appear between
    the size and end lines.  The format goes:
        hex <coordinates> <elevation> <terrain attributes> <theme>

    The coordinates should be as they appear on paper mapsheets: a four-digit 
    number starting with 0101 in the upper left-hand corner and going as high 
    as 1617 for a board of the default size.

    The elevation should be an integer, unquoted.

    All terrain attributes, if any, for the hex should be a single, quoted
    string.  Within this string, terran is specified as:
        <terrain>:<level>[:<exits>]

    The terrian should be a lower-case string of one of the terrain types
    listed above.  The level is seperated by the terrain by a colon (:).
    Optionally, the exits parameter is appended, seperated with a : again.

    Multiple terrain types should be in the same string, seperated with the
    semicolon (;) character.

    The theme of the hex is purely for cosmetic purposes.  It should be left
    blank for ordinary hexes by putting empty quotes in its place.  The theme
    is intended as a tag for the tileset file to indicate a special graphic for
    the hex.  Its uses might include indicating a specific graphic for a 
    building, or making rivers distinct from lakes.  In v0.29, only the "snow"
    theme is available.  Note, the "snow" theme does *not* implement the "ice"
    or "cold weather" rules from the BattleTech Master Rules; they just look
    like they should.

    Hex data can appear in any order, as long as there is only one hex at any
    specific coordinates.  Any coordinates with no data will be filled in with
    level 0, clear terrain.
    
The graphics for maps are stored in tileset files.  These are located in the
data/hexes directory.  The default tileset is called "defaulthexset.txt".  
Image files should be located in a subdirectory off of the data/hexes directory.
The tileset used by the game can be changed in the MegaMek.cfg file, in the
game base directory.

  Tileset File Format
    The tileset file format is similar to the board file format.  The two
    keywords used are "super" and "base".  Both take the same parameters.
    All super lines should appear before all base lines.  The order of lines,
    even within these groupings, is significant.

  The "super" Keyword
    Lines beginning with the super keyword indicate graphics that will be
    displayed on top of, superimposed over, a base graphic for the hex.

  The "base" Keyword
    Lines beginning with the base keyword indicate a graphic that will be used
    as the base graphic for the hex.

  Parameters
    The parameters are similar to the parameters for hex data in the board
    files.  The format is:
        super/base <elevation> <terrains> <theme> <image file>

    Elevation, terrains and theme are specified in the same manner as in the
    board file.  Elevation can use "*" as a wildcard to indicate that the
    graphic can be used for any elevation.

    The image file should be the path and name of the image, with the path
    originating in the data/hexes directory.  Thus, if you wanted to load
    "rubble.gif" out of the "myhexes" subdirectory, put "myhexes/rubble.gif".

  Matching Tileset Graphics With Actual Hexes
    First, let's talk about base hexes.  When trying to figure out a base
    graphic for a hex on the board, the program goes through all the base hex
    images in the tileset in the order that they are listed.  The first, best
    match is used.  That is, the first base hex image that best matches the hex 
    on the board.

    Super images are listed first so that they can be matched first.  They match
    a little differently than base hexes, in that they must have an exact match
    for any elevation, terrain, or theme listed for that image in the tileset.
    Any terrain matched exactly in this manner is then removed from 
    consideration for future matches.

    As an example of the matching process, take a hex on the board, at level 2,
    and with woods and road.  This hex would appear in the board file as:
        hex 0205 2 "woods:1;road:1" ""
    Let's also assume that this road is part of a larger road, running north to
    south, so the program internally determines the road's exits parameter to
    be 9.

    The relevant entries from the default tileset are:
        super * "road:1:09" "" "boring/road09.gif"
        base 2 "woods:1" "" "boring/light_forest_2.gif"
    Both are perfect matches when evaluated by the program.

    Some images require two or more terrains to be used in tandem.  Take, for
    example, the "light_woods_swamp_1.gif" image.  This is the image that would
    be used for the following hex:
        hex 0205 1 "woods:1;swamp:1" ""

    The relevant entry from the default tileset is:
        base 1 "woods:1;swamp:1" "" "swamp/light_forest_swamp_1.gif"

    For the super image, the elevation is a match, because it's a wildcard.  
    The hex being evaluated contains a perfect match for all terrain specified 
    by the super image, and the theme is a match because both are blank.
    After making this match, the road terrain in the hex being evaluated is
    removed from further evaluation.

    After this removal, the woods terrain is a perfect match.  All the relevant
    terrain, the elevation and the theme are matches.  The program will display
    the light_forest_2.gif, with the road09.gif superimposed on top of it, as
    the image for this hex in the game.

    
Author: Ben Mazur

  
    


