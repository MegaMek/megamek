/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common.voting;

/**
 * How many yes votes a {@link Poll} needs to pass. Each threshold turns a running tally into a poll status, and
 * closes the poll as soon as the outcome can no longer change rather than waiting for the last ballot.
 */
public enum VoteThreshold {

    /** Every voter must vote yes; a single no closes the poll as failed. */
    UNANIMOUS {
        @Override
        public PollStatus evaluate(int yesVotes, int noVotes, int pendingVotes) {
            if (noVotes > 0) {
                return PollStatus.FAILED;
            }
            return (pendingVotes == 0) ? PollStatus.PASSED : PollStatus.OPEN;
        }
    },

    /** More yes than no votes pass the poll; a tie once everyone has voted fails it. */
    MAJORITY {
        @Override
        public PollStatus evaluate(int yesVotes, int noVotes, int pendingVotes) {
            // Decided as soon as the outstanding ballots can no longer change the outcome. With no ballots
            // outstanding these two conditions cover every tally, ties failing through the second.
            if (yesVotes > noVotes + pendingVotes) {
                return PollStatus.PASSED;
            }
            if (yesVotes + pendingVotes <= noVotes) {
                return PollStatus.FAILED;
            }
            return PollStatus.OPEN;
        }
    };

    /**
     * Judges a running tally against this threshold.
     *
     * @param yesVotes     the yes votes cast so far
     * @param noVotes      the no votes cast so far
     * @param pendingVotes the voters who have not voted yet
     *
     * @return the resulting poll status: still open, passed, or failed
     */
    public abstract PollStatus evaluate(int yesVotes, int noVotes, int pendingVotes);
}
