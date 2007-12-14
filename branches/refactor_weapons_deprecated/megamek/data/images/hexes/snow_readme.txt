Snow Theme v0.2 for MegaMek
###########################
By Kurt "Kobra" Kajal
(April 5th 2003)

This graphics pack adds snow-themed graphics to MegaMek 0.28.12 and later versions. Just unzip the file in the data/hexes folder and you're ready to go!

Currently, the replaced terrain types are:

Pavement --> Ice
Rough -----> Snow-covered rough
Woods -----> Snow-covered woods (light and heavy)

If snow-themed maps do not show up correctly, it's likely that your hexset file does not contain the snow theme entries (0.28.12 did include these, but there's no guarantee future versions will). If you are having trouble, open up the defaulthexset.txt file in your data/hexes folder and add, if it isn't there already, the following block to the end:

#------------------- BEGIN snow theme

base 0 "pavement:1" "snow" "themes/snow_0.gif"
base 1 "pavement:1" "snow" "themes/snow_1.gif"
base 2 "pavement:1" "snow" "themes/snow_2.gif"
base 3 "pavement:1" "snow" "themes/snow_3.gif"
base 4 "pavement:1" "snow" "themes/snow_4.gif"
base 5 "pavement:1" "snow" "themes/snow_5.gif"
base 6 "pavement:1" "snow" "themes/snow_6.gif"
base 7 "pavement:1" "snow" "themes/snow_7.gif"
base 8 "pavement:1" "snow" "themes/snow_8.gif"
base 9 "pavement:1" "snow" "themes/snow_9.gif"
base 10 "pavement:1" "snow" "themes/snow_10.gif"
base -1 "pavement:1" "snow" "themes/snow_-1.gif"
base -2 "pavement:1" "snow" "themes/snow_-2.gif"
base -3 "pavement:1" "snow" "themes/snow_-3.gif"
base -4 "pavement:1" "snow" "themes/snow_-4.gif"
base -5 "pavement:1" "snow" "themes/snow_-5.gif"
base -6 "pavement:1" "snow" "themes/snow_-6.gif"

base 0 "rough:1" "snow" "themes/snow_rough_0.gif"
base 1 "rough:1" "snow" "themes/snow_rough_1.gif"
base 3 "rough:1" "snow" "themes/snow_rough_3.gif"
base 5 "rough:1" "snow" "themes/snow_rough_5.gif"
base -1 "rough:1" "snow" "themes/snow_rough_-1.gif"
base -3 "rough:1" "snow" "themes/snow_rough_-3.gif"
base -5 "rough:1" "snow" "themes/snow_rough_-5.gif"

base 0 "woods:1" "snow" "themes/snow_light_forest_0.gif"
base 1 "woods:1" "snow" "themes/snow_light_forest_1.gif"
base 2 "woods:1" "snow" "themes/snow_light_forest_2.gif"
base 0 "woods:2" "snow" "themes/snow_heavy_forest_0.gif"
base 1 "woods:2" "snow" "themes/snow_heavy_forest_1.gif"

#------------------- END snow theme