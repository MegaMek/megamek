
This is a guide to the bot that comes with MegaMek 0.27.  The bot was written
by Steve Hawkins <hawk0166@umn.edu> building off of the work of Michael Hanson 
<hanson@cs.stanford.edu> -- including this file.  
It is a work in progress.

The bot does not work in games with units other than mechs, such as tanks or
infantry.  The bot does not have double-blind support, and will disconnect any 
time it cannot see an opponent.

  Starting A Game Against The Bot
    To play against the bot, host a game, and then start another copy of MegaMek
    and press the "Connect as Bot..." button in the main menu.  Connect as 
    though you were playing a hotseat game (see the readme for details.)  You
    will need to pick mechs and a starting position for the bot and press ready
    when you are ready to start the game.  Starting with the initiative screen,
    the bot will begin to play.  You can play your side, but do not press any
    buttons for the bot.

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
    Because the bot connects like a normal client, you can connect two seperate
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
    Unlike the last bot, it will move without los.
    The movement is not yet hierarcial, so with every new unit there is a 
    somewhat linear increase in the time the bot thinks. Unless you have 
    patience, don't do something over 8 on 8. In fact, the bot has been 
    optimized for lance on lance action.
    The bot will preform will with most units on average terrain maps.  A lot or
    too little terrain will hurt its performance. It also does no real path
    planning, so limited mobility mechs can get lost.