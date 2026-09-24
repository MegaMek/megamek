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

package megamek.client.ui.dialogs;

import java.util.LinkedHashMap;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;

/**
 * Remembers, for the gamemaster's own screen, what a hex held before they first changed it.
 *
 * <p>This exists only so a dialog can say "this hex held X before you changed it" without asking the server. The
 * server keeps its own copy of the hex itself and is the one that actually puts it back; this is a description for
 * reading, nothing more, and the two are written at the same moment - the client records what it is about to change
 * just before sending, and the server records the hex as the edit arrives.</p>
 *
 * <p>It is deliberately not part of the game. Nothing here is sent anywhere, saved, or read by any rule; it lasts as
 * long as the client is running and is forgotten when a hex is put back.</p>
 */
public final class GameMasterEditMemory {

    /** What each hex held before a gamemaster first changed it, in words, keyed by hex. */
    private static final Map<Coords, String> DESCRIPTIONS_BEFORE_FIRST_EDIT = new LinkedHashMap<>();

    private GameMasterEditMemory() {
    }

    /**
     * Records what a hex held, if nothing has been recorded for it yet. Later edits are changes to a hex already
     * being worked on, so the first description is the one worth keeping.
     *
     * @param coords      The hex about to be changed
     * @param description What it holds now, in words
     */
    public static void rememberBeforeFirstEdit(Coords coords, String description) {
        DESCRIPTIONS_BEFORE_FIRST_EDIT.putIfAbsent(coords, description);
    }

    /**
     * @param coords The hex to ask about
     *
     * @return what the hex held before a gamemaster first changed it, or {@code null} when none has changed it
     */
    public static @Nullable String describeBeforeFirstEdit(Coords coords) {
        return DESCRIPTIONS_BEFORE_FIRST_EDIT.get(coords);
    }

    /** Forgets a hex, because it has been put back the way it was and there is nothing left to describe. */
    public static void forget(Coords coords) {
        DESCRIPTIONS_BEFORE_FIRST_EDIT.remove(coords);
    }
}
