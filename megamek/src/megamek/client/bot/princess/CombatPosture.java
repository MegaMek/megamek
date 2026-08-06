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
package megamek.client.bot.princess;

import megamek.client.ui.Messages;
import megamek.common.annotations.Nullable;

/**
 * Whether the bot's force is attacking or defending.
 *
 * <p>Terrain obstacles - deep water above all - mean opposite things to the two postures, and no single cost
 * tuning produces both behaviors. An attacker treats a river as a cost on the way to the objective: it picks a
 * crossing, pays the risk, and seizes the far bank. A defender treats the same river as its best weapon: it
 * never crosses, holds positions covering the crossing points, and takes the fight to the enemy while the
 * enemy is slowed, split up, and half-disarmed in the water. A bot that prices water one way plays one of
 * those roles well and the other one badly.</p>
 *
 * <p>{@link #AUTO} leaves the call to the bot, which reads it from what it already knows each round - whether
 * it has somewhere to go, and whether the enemy is coming to it. See {@code PostureResolver}.</p>
 */
public enum CombatPosture {
    /** Take ground: obstacles are a cost to pay on the way to the enemy. */
    ATTACK(Messages.getString("BotConfigDialog.postureAttack")),
    /** Hold ground: obstacles are a weapon, and the enemy should be the one paying to cross them. */
    DEFEND(Messages.getString("BotConfigDialog.postureDefend")),
    /** Let the bot decide each round from its mission and the enemy's movement. */
    AUTO(Messages.getString("BotConfigDialog.postureAuto"));

    private final String displayName;

    CombatPosture(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Parses a posture from text, for XML round-trips and behavior scripts.
     *
     * @param text the posture name, case-insensitive
     *
     * @return the matching posture, or {@link #AUTO} when the text matches nothing
     */
    public static CombatPosture parse(@Nullable String text) {
        if (text == null) {
            return AUTO;
        }
        for (CombatPosture posture : values()) {
            if (posture.name().equalsIgnoreCase(text.trim())) {
                return posture;
            }
        }
        return AUTO;
    }
}
