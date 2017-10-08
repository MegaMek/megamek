Document History:
Version 1 - Michael Hanson <hanson@cs.stanford.edu>
Version 2 - Steve Hawkins <hawkprime@users.sourceforge.net>
Version 3 - Jeff Wang <swang30@users.sourceforge.net>

Bot History:
Version 1 - Michael Hanson - Original implementation
Version 2 - Steve Hawkins - Utility model improved, Genetic Algorithm added
Version 3 - Sebastian Brocks <beerockxs@users.sourceforge.net> / Jeff Wang - General 
			speed improvements, gui-less client

Known issues:
The Bot does some dumb things, such as using MASC without being in danger, or 
	running on pavement when not necessary.
The Bot is slow, as compared to many games out there.  But still faster than a 
	human on tabletop games. :)
The Bot crashes sometimes. (Boo!)
The Bot can't handle alternative victory conditions.  It still tries to kill you, 
	no matter what.
The Bot can't handle Double-Blind. (It just sits there, having no target to attack.)

Usage:
The bot works with most units: mechs, tanks and infantry.  
Memory wise, normal MM needs 20 MB + .5 MB for each entity + 1 MB for each 
board. The bot will need an additional 15 MB + 2 MB for each entity.  There are a
few bugs that will cause it to stop responding, usually, there will be a stack
trace associated with the error in megamek.log, please save this log, and post it
as a bug on Github.  You can restart your game from the last save.

Using the gui-less client (preferred)-
	a) start your own game normally
	b) go to the players list and click add bot
	c) set up units, teams, etc. normally.
	d) hit done and play.
	 
Using the gui client (new or saved game) -
	a) start your own game normally, and get to the lobby, set up your units.
	b) start another MM instance (you can start multiple copies of MM from one install)
	c) click on Connect as Computer Player and enter the connection parameters you 
		specified in (a)
	d) get to the lobby, add your units and click done on both clients.

Starting from a saved game (gui-less client)-
	a) load your saved game normally
	b) on the chat line, enter "/replacePlayer <botname>" (No quotes, and replace 
		<botname> with the name of the bot.)
	c) play on normally.

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

  Bot Speed
    The Bot works better with a smaller amount of units.  4 gives decent response
    time, anything above 12 is highly questionable.  Multiple bots taking 4 each
    will work a lot faster than 1 bot taking 12 units, but of course the
    coordination between them will be about nil.
    The gui-less client is about 100x faster than the gui client.  Don't use the
    gui client unless you have to, or it's a small bot.
    Individual Initiative works better for The Bot, because it only needs to
    calculate what's the best move RIGHT NOW.
    Have the Bot use slower/un-jump-jetted units.  A 3/5/0 has lower number of
    calculations than a 7/11/0 which in turn has lower number of possibilities
    than a 3/5/3.
    
  Bot Vs. Bot
    Because the bot connects like a normal client, you can start two separate
    bots and they will fight against each other.  If you do not want to play, 
    you can start a dedicated server (see the readme) and connect two bots to it.
    This is not recommended however, since for small forces (4 on 4) the bots 
    will move too fast for the contest to be interesting.

  bot.properties
    This file contains settings for changing the behavior of the bot.
    At this time, you can change the difficulty to one of three levels.
      --The difference may not be all that great, but should be enough.
      
    You can also force the bot to consider one unit at a time (forcing
    the bot to consider a random unit each time it moves.)  This option
    is active only if individual initiative is NOT turned on.  Be
    warned that the bot will lack what limited unit cohesion it used to
    have.  

  Tips & Thoughts
    The bot has some smarts, but does no learning -- so don't expect it to adapt
    to your playing style.
    Its thinking is mostly limited to a single turn, provided it does understand
    to move mechs to good ranges.
    The movement is not yet hierarchal, so with every new unit there is a 
    somewhat linear increase in the time the bot thinks. Unless you have 
    patience, don't do something over 8 on 8. 
    The bot will preform well with most units on average terrain maps.  A lot or
    too little terrain will hurt its performance. It also does no real path
    planning, so limited mobility mechs can get lost.
 
Next Steps:

Reuse the new pathing code.
Intelligent deployment and mine placement.
More strategy: have the bots concentrate on controlling specified locations.
Attempt to be as good as an intermediate player on a 4x4 map in an 16 on 16 battle.