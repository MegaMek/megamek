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

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A vote among players, held by player id so it carries no game objects: who called it, who may vote, what each
 * voter chose, and whether it has passed its threshold. The poll only decides whether it passed; what a passed poll
 * means - granting the gamemaster role, say - is its caller's business.
 * <p>
 * The poll closes itself the moment its outcome can no longer change, and a closed poll no longer moves: votes,
 * voter changes and cancellation are ignored once it has resolved. Voters can join and leave an open poll, as
 * players do a game, and the poll re-judges itself after every change.
 * </p>
 */
public class Poll implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int requesterId;
    private final VoteThreshold threshold;
    /** Each eligible voter's choice, in the order the voters were added, so every client lists them the same way. */
    private final Map<Integer, VoteChoice> votes = new LinkedHashMap<>();
    private PollStatus status = PollStatus.OPEN;

    /**
     * Opens a poll. The requester is always a voter and starts having voted yes for what they asked for.
     *
     * @param requesterId the id of the player calling the vote
     * @param voterIds    the ids of the players who may vote; the requester is added if absent
     * @param threshold   how many yes votes the poll needs to pass
     */
    public Poll(int requesterId, Collection<Integer> voterIds, VoteThreshold threshold) {
        this.requesterId = requesterId;
        this.threshold = threshold;
        for (int voterId : voterIds) {
            votes.put(voterId, VoteChoice.PENDING);
        }
        votes.put(requesterId, VoteChoice.YES);
        judge();
    }

    /**
     * Casts a voter's ballot and re-judges the poll. Ignored for players who are not voters, after the poll has
     * closed, and for the requester, whose yes stands for as long as the poll does.
     *
     * @param voterId the voter casting the ballot
     * @param inFavor {@code true} to vote yes, {@code false} to vote no
     */
    public void castVote(int voterId, boolean inFavor) {
        if (status.isResolved() || !votes.containsKey(voterId) || (voterId == requesterId)) {
            return;
        }
        votes.put(voterId, inFavor ? VoteChoice.YES : VoteChoice.NO);
        judge();
    }

    /**
     * Adds a voter to an open poll, as when a player joins the game mid-vote. They start with their ballot pending.
     * Ignored once the poll has closed, and for players who are already voters.
     *
     * @param voterId the joining voter
     */
    public void addVoter(int voterId) {
        if (status.isResolved()) {
            return;
        }
        votes.putIfAbsent(voterId, VoteChoice.PENDING);
    }

    /**
     * Removes a voter from an open poll, as when a player leaves the game mid-vote, and re-judges it: the departure
     * may have been the last outstanding ballot. Ignored once the poll has closed. Removing the requester does not
     * cancel the poll; that is for the caller to decide through {@link #cancel()}.
     *
     * @param voterId the departing voter
     */
    public void removeVoter(int voterId) {
        if (status.isResolved()) {
            return;
        }
        votes.remove(voterId);
        judge();
    }

    /** Closes an open poll as cancelled, no matter the tally. Ignored once the poll has closed. */
    public void cancel() {
        if (!status.isResolved()) {
            status = PollStatus.CANCELLED;
        }
    }

    /** Re-judges an open poll against its threshold, closing it when the outcome can no longer change. */
    private void judge() {
        if (!status.isResolved()) {
            status = threshold.evaluate(countVotes(VoteChoice.YES),
                  countVotes(VoteChoice.NO),
                  countVotes(VoteChoice.PENDING));
        }
    }

    private int countVotes(VoteChoice choice) {
        int count = 0;
        for (VoteChoice vote : votes.values()) {
            if (vote == choice) {
                count++;
            }
        }
        return count;
    }

    /** @return the id of the player who called the vote */
    public int getRequesterId() {
        return requesterId;
    }

    /** @return how many yes votes the poll needs to pass */
    public VoteThreshold getThreshold() {
        return threshold;
    }

    /** @return where the poll stands: still collecting votes, or closed one of three ways */
    public PollStatus getStatus() {
        return status;
    }

    /** @return each voter's choice by player id, in the order the voters were added; read-only */
    public Map<Integer, VoteChoice> getVotes() {
        return Collections.unmodifiableMap(votes);
    }

    /** @return whether the given player is a voter in this poll */
    public boolean hasVoter(int voterId) {
        return votes.containsKey(voterId);
    }
}
