The bot was written by Steve Hawkins <hawkprime@users.sourceforge.net> building off of 
the work of Michael Hanson <hanson@cs.stanford.edu>.
  
The bot works with most units: mechs, tanks and infantry.  There are a few bugs that will cause 
it to stop responding.  When this happens, start again from the last saved game.  

  Starting A Game Against The Bot
    To play against the bot, host a game.
    Go to the players list and click add bot.
    Selecting that bot from the players list will allow you to change its team or remove it. 
    Set the bot's starting position by selecting it from the starting 
    position list, then click change start.
    As you use the add unit dialog, just assign some mechs to the bot.
    Hit done and have fun!
	
    The bot will also work in most Scenarios.
    
    Alternatively, you can still launch the bot in a separate client like before.
        
    TODO: camo change ability
    TODO: have unit loading list work with bots
    
History:

.3.x Current
  A gui-less incarnation of the the .3 bot with support for random minefield placement.
I'm sure that this new setup will cause people trouble at first, but it should be worth it.
  
.3
  A refactoring of the bot that removed about 1/3 of the code and most of the original code.  
It also supports double-blind although not very well...  

.2.x 
  Various bug fixes and tweaks by MegaMek developers.  Support for infantry, 
vehicles and automatic deployment.

.2 TestBot - Steve Hawkins
  Spurred on by taking an AI class, the original bot was improved to have a more
robust utility model and a polynomial movement exploration algorithm.  The movement
algorithm was further enhanced to a hierarchical filtering process that included limited
look ahead and planning.  Entities go through a characterization phase to have fast approximations
available for the first stages of the filtering.  Also, firing determination and lance movement were 
changed to use genetic algorithms.  

.1 The Original - Michael Hanson
  Semi-utility based with an exponential movement exploration algorithm. No movement
planning or look ahead. Support for some physical attacks.

Other Possibilities:

  Bot Support
    The bot will understand if you put it on a team (and friendly fire is turned
    off) that it is friends with that team. Currently, it has very limited
    coordination with units that are not under its control. To help this 
    situation out, when other team members direct chat to the bot (for example,
    if you name the bot Fred, enter "Fred: ..." at the chat bar where ... is 
    your command) that will cause its behavior to change. Supported commands are:
    be aggressive -- gives it a preference for doing damage (wears off when the 
        bot is damaged)
    calm down -- gives it a preference for being safe (wears off if winning)
      --More to come!
    Example usage would be to have the bot provide fire support and handle close
    range fighting yourself.

  Bot Vs. Bot
    Because the bot connects like a normal client, you can start two seperate
    bots and they will fight against each other.  If you do not want to play, 
    you can start a dedicated server (see the readme) and connect two bots to it.
    This is not recommended however, since for small forces (4 on 4) the bots 
    will move too fast for the contest to be interesting.

  bot.properties
    This file contains settings for changing the behavior of the bot.
    At this time, you can change the difficulty to one of three levels.
      --The difference may not be all that great, but should be enough.

  Tips & Thoughts
    The bot has some smarts, but does no learning -- so don't expect it to adapt
    to your playing style.
    Its thinking is mostly limited to a single turn, provided it does understand
    to move mechs to good ranges.
    The movement is not yet hierarcial, so with every new unit there is a 
    somewhat linear increase in the time the bot thinks. Unless you have 
    patience, don't do something over 8 on 8. 
    The bot will preform well with most units on average terrain maps.  A lot or
    too little terrain will hurt its performance. It also does no real path
    planning, so limited mobility mechs can get lost.
 
Tech Note:

Be careful about the memory allocated to you JVM when playing against the bot.
MegaMek by itself currently needs about 20MB + .5MB for each entity.  Some number of bots 
need about 15MB + 2MB for each entity.  It is quite possible for a lance on lance battle
to generate an OutOfMemory error after several rounds.  We are working on reducing
this overhead, but until then make sure to give the JVM enough heap space. 

Next Steps:

Reuse the new pathing code.
Intellegent deployment and mine placement.
More strategy: have the bots concentrate on controlling specified locations.
Attempt to be as good as an intermidiate player on a 4x4 map in an 16 on 16 battle.