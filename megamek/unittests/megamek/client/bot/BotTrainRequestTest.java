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

package megamek.client.bot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Covers when a bot repeats a build-train request.
 *
 * <p>The server's reply to a build request arrives as another lobby update, which asks the planner again, so an
 * identical request has to be suppressed or the bot loops. A plan that has grown is a different request and must get
 * through, otherwise trailers handed to the bot after its first request stay loose for the whole game.</p>
 */
class BotTrainRequestTest {

    private static final int TRACTOR_ID = 7;

    @Test
    void theFirstRequestForATractorIsSent() {
        Map<Integer, List<Integer>> requested = new HashMap<>();

        assertTrue(BotClient.recordTrainRequest(requested, TRACTOR_ID, List.of(11)));
    }

    @Test
    void anIdenticalRequestIsNotRepeated() {
        Map<Integer, List<Integer>> requested = new HashMap<>();
        BotClient.recordTrainRequest(requested, TRACTOR_ID, List.of(11));

        assertFalse(BotClient.recordTrainRequest(requested, TRACTOR_ID, List.of(11)),
              "The reply to the first request must not trigger a second");
    }

    @Test
    void aTractorGivenMoreTrailersIsAskedAgain() {
        Map<Integer, List<Integer>> requested = new HashMap<>();
        BotClient.recordTrainRequest(requested, TRACTOR_ID, List.of(11));

        assertTrue(BotClient.recordTrainRequest(requested, TRACTOR_ID, List.of(11, 12, 13)),
              "Trailers assigned after the first request must still get hitched");
    }

    @Test
    void aDifferentTrailerOrderIsAskedAgain() {
        Map<Integer, List<Integer>> requested = new HashMap<>();
        BotClient.recordTrainRequest(requested, TRACTOR_ID, List.of(11, 12));

        assertTrue(BotClient.recordTrainRequest(requested, TRACTOR_ID, List.of(12, 11)),
              "Order sets the hitch chain, so a reordered plan is a different request");
    }

    @Test
    void eachTractorIsTrackedSeparately() {
        Map<Integer, List<Integer>> requested = new HashMap<>();
        BotClient.recordTrainRequest(requested, TRACTOR_ID, List.of(11));

        assertTrue(BotClient.recordTrainRequest(requested, TRACTOR_ID + 1, List.of(11)),
              "One tractor's request must not suppress another's");
    }

    @Test
    void theRecordedPlanIsNotAffectedByLaterChangesToTheCallersList() {
        Map<Integer, List<Integer>> requested = new HashMap<>();
        List<Integer> plan = new ArrayList<>(List.of(11));
        BotClient.recordTrainRequest(requested, TRACTOR_ID, plan);

        plan.add(12);

        assertTrue(BotClient.recordTrainRequest(requested, TRACTOR_ID, plan),
              "The record is a copy, so a mutated plan is still seen as new");
    }
}
