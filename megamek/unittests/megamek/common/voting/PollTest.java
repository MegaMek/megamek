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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class PollTest {

    private static final int REQUESTER = 0;
    private static final int SECOND_VOTER = 1;
    private static final int THIRD_VOTER = 2;
    private static final int FOURTH_VOTER = 3;

    private Poll unanimousPoll() {
        return new Poll(REQUESTER, List.of(REQUESTER, SECOND_VOTER, THIRD_VOTER), VoteThreshold.UNANIMOUS);
    }

    private Poll majorityPoll() {
        return new Poll(REQUESTER, List.of(REQUESTER, SECOND_VOTER, THIRD_VOTER), VoteThreshold.MAJORITY);
    }

    @Test
    void requesterStartsHavingVotedYes() {
        Poll poll = unanimousPoll();
        assertEquals(VoteChoice.YES, poll.getVotes().get(REQUESTER));
        assertEquals(PollStatus.OPEN, poll.getStatus());
    }

    @Test
    void soleVoterPassesImmediately() {
        Poll poll = new Poll(REQUESTER, List.of(REQUESTER), VoteThreshold.UNANIMOUS);
        assertEquals(PollStatus.PASSED, poll.getStatus());
    }

    @Test
    void unanimousPassesWhenAllVoteYes() {
        Poll poll = unanimousPoll();
        poll.castVote(SECOND_VOTER, true);
        assertEquals(PollStatus.OPEN, poll.getStatus());
        poll.castVote(THIRD_VOTER, true);
        assertEquals(PollStatus.PASSED, poll.getStatus());
    }

    @Test
    void unanimousFailsOnTheFirstNo() {
        Poll poll = unanimousPoll();
        poll.castVote(SECOND_VOTER, false);
        assertEquals(PollStatus.FAILED, poll.getStatus());
    }

    @Test
    void majorityPassesAsSoonAsItIsDecided() {
        // four voters: requester yes + one more yes = 2 yes, 0 no, 2 pending - not decided yet
        Poll poll = new Poll(REQUESTER, List.of(REQUESTER, SECOND_VOTER, THIRD_VOTER, FOURTH_VOTER),
              VoteThreshold.MAJORITY);
        poll.castVote(SECOND_VOTER, true);
        assertEquals(PollStatus.OPEN, poll.getStatus());
        // a third yes cannot be caught by the one outstanding no
        poll.castVote(THIRD_VOTER, true);
        assertEquals(PollStatus.PASSED, poll.getStatus());
    }

    @Test
    void majorityFailsAsSoonAsItCannotBeWon() {
        // three voters: requester's one yes against two no votes
        Poll poll = majorityPoll();
        poll.castVote(SECOND_VOTER, false);
        assertEquals(PollStatus.OPEN, poll.getStatus());
        poll.castVote(THIRD_VOTER, false);
        assertEquals(PollStatus.FAILED, poll.getStatus());
    }

    @Test
    void majorityTieFails() {
        // four voters, 2 yes and 2 no
        Poll poll = new Poll(REQUESTER, List.of(REQUESTER, SECOND_VOTER, THIRD_VOTER, FOURTH_VOTER),
              VoteThreshold.MAJORITY);
        poll.castVote(SECOND_VOTER, true);
        poll.castVote(THIRD_VOTER, false);
        assertEquals(PollStatus.OPEN, poll.getStatus());
        poll.castVote(FOURTH_VOTER, false);
        assertEquals(PollStatus.FAILED, poll.getStatus());
    }

    @Test
    void aJoiningVoterHoldsAnAlmostDecidedPollOpen() {
        Poll poll = unanimousPoll();
        poll.castVote(SECOND_VOTER, true);
        poll.addVoter(FOURTH_VOTER);
        // the last original voter agrees, but the joiner's ballot is still out
        poll.castVote(THIRD_VOTER, true);
        assertEquals(PollStatus.OPEN, poll.getStatus());
        poll.castVote(FOURTH_VOTER, true);
        assertEquals(PollStatus.PASSED, poll.getStatus());
    }

    @Test
    void aLeavingVoterCanResolveThePoll() {
        Poll poll = unanimousPoll();
        poll.castVote(SECOND_VOTER, true);
        // the third voter never votes and leaves the game
        poll.removeVoter(THIRD_VOTER);
        assertEquals(PollStatus.PASSED, poll.getStatus());
    }

    @Test
    void cancellingAnOpenPollClosesIt() {
        Poll poll = unanimousPoll();
        poll.cancel();
        assertEquals(PollStatus.CANCELLED, poll.getStatus());
    }

    @Test
    void aResolvedPollNoLongerMoves() {
        Poll poll = unanimousPoll();
        poll.castVote(SECOND_VOTER, false);
        assertEquals(PollStatus.FAILED, poll.getStatus());

        poll.castVote(THIRD_VOTER, true);
        poll.addVoter(FOURTH_VOTER);
        poll.removeVoter(SECOND_VOTER);
        poll.cancel();

        assertEquals(PollStatus.FAILED, poll.getStatus());
        assertFalse(poll.hasVoter(FOURTH_VOTER), "a closed poll accepted a new voter");
        assertTrue(poll.hasVoter(SECOND_VOTER), "a closed poll lost a voter");
    }

    @Test
    void theRequesterCannotVoteThemselvesDown() {
        Poll poll = unanimousPoll();
        poll.castVote(REQUESTER, false);
        assertEquals(VoteChoice.YES, poll.getVotes().get(REQUESTER));
        assertEquals(PollStatus.OPEN, poll.getStatus());
    }

    @Test
    void aNonVoterCannotVote() {
        Poll poll = unanimousPoll();
        poll.castVote(99, false);
        assertEquals(PollStatus.OPEN, poll.getStatus());
        assertFalse(poll.hasVoter(99));
    }

    @Test
    void changingAVoteRejudgesThePoll() {
        Poll poll = majorityPoll();
        poll.castVote(SECOND_VOTER, false);
        assertEquals(PollStatus.OPEN, poll.getStatus());
        // changing their mind to yes decides the majority in favor
        poll.castVote(SECOND_VOTER, true);
        assertEquals(PollStatus.PASSED, poll.getStatus());
    }
}
