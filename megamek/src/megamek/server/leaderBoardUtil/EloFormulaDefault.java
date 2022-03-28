/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.server.leaderBoardUtil;

import megamek.common.Player;
import megamek.server.LeaderBoard.LeaderBoard;
import megamek.server.LeaderBoard.LeaderBoardEntry;

public class EloFormulaDefault implements EloFormula {

        private final int BASE_POINT = 20;
        public int calcElo(LeaderBoard lb, Player winner, Player loser, double destWinner, double destLoser){

            LeaderBoardEntry winnerEntry = lb.get(winner);
            LeaderBoardEntry loserEntry = lb.get(loser);

            double eloFactor = winnerEntry.getElo() / loserEntry.getElo();
            double destFactor = destWinner / destLoser;
            double elo = ( BASE_POINT * eloFactor ) / destFactor;
            return (int)elo;
        }
}
