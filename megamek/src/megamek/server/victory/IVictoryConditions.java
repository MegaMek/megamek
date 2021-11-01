/*
 * MegaMek - Copyright (C) 2007-2008 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.server.victory;

import java.util.Map;

import megamek.common.Game;

/**
 * Interface for classes judging whether a victory occurred or not. These
 * classes may not modify game state. Reporting must be done via the given
 * interface. classes also should not have any nasty internal state outside
 * methods this will guarantee them to act _only_ based on gamestate The only
 * case where this should be done is in cases where you have to count occurances
 * of some action or duration of something. In this case the information should
 * be stored in the context-object NOTE: if you delegate from one victory-class
 * instance to other, they must be created in similar fashion (ie. constructed
 * at the same time and destroyed at the same time) to guarantee proper working
 * also , if you delegate, you should delegate each time to guarantee that
 * victorypoint-counting and duration-counting implementations get access to
 * game state every round. NOTE2: calling victory 1 time or n times per round
 * should not make a difference! victories counting rounds must not assume that
 * they are called only once per round NOTE3: dont even think about making
 * different victoryclasses communicate straight or via context-object ;) NOTE4:
 * victories should take into account in their reporting (adding reports to the
 * result-object) that their results might be filtered. So the reports should be
 * mostly of the "what is the score" -type (player A occupies the victory
 * location) or fact-type (Player A has destroyed player B's commander)
 */
public interface IVictoryConditions {
    /**
     * @param game - the game (state) we are playing
     * @param context - a map Strings to simple serializable objects (preferably
     *            Integers , Strings ,Doubles etc) which are used to store state
     *            between executions if such feature is absolutely required.. as
     *            a key you should use something atleast class- specific to
     *            limit namespace collisions
     * @return a result with true if victory occured, false if not must not
     *         return null MUST NOT modify game state!
     */
    VictoryResult victory(Game game, Map<String, Object> context);
}
