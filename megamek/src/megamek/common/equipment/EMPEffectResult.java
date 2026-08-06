/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.equipment;

import megamek.MMConstants;

/**
 * Represents the result of an EMP mine effect roll for a unit.
 * <p>
 * Based on Tactical Operations: Advanced Rules (Experimental).
 * </p>
 *
 * @param effect        The effect type: {@link MMConstants#EMP_EFFECT_NONE},
 *                      {@link MMConstants#EMP_EFFECT_INTERFERENCE}, or {@link MMConstants#EMP_EFFECT_SHUTDOWN}
 * @param durationTurns Number of turns the effect lasts (1D6)
 * @param rollValue     The 2D6 roll value for the effects table
 * @param modifier      Modifier applied to the roll (+2 for drones)
 */
public record EMPEffectResult(
      int effect,
      int durationTurns,
      int rollValue,
      int modifier
) {

    /**
     * Creates an EMPEffectResult indicating no effect.
     *
     * @param rollValue The roll value that resulted in no effect
     * @param modifier  The modifier that was applied
     *
     * @return An EMPEffectResult with no effect
     */
    public static EMPEffectResult noEffect(int rollValue, int modifier) {
        return new EMPEffectResult(MMConstants.EMP_EFFECT_NONE, 0, rollValue, modifier);
    }

    /**
     * Creates an EMPEffectResult indicating interference effect.
     *
     * @param durationTurns Number of turns the interference lasts
     * @param rollValue     The roll value that resulted in interference
     * @param modifier      The modifier that was applied
     *
     * @return An EMPEffectResult with interference effect
     */
    public static EMPEffectResult interference(int durationTurns, int rollValue, int modifier) {
        return new EMPEffectResult(MMConstants.EMP_EFFECT_INTERFERENCE, durationTurns, rollValue, modifier);
    }

    /**
     * Creates an EMPEffectResult indicating shutdown effect.
     *
     * @param durationTurns Number of turns the shutdown lasts
     * @param rollValue     The roll value that resulted in shutdown
     * @param modifier      The modifier that was applied
     *
     * @return An EMPEffectResult with shutdown effect
     */
    public static EMPEffectResult shutdown(int durationTurns, int rollValue, int modifier) {
        return new EMPEffectResult(MMConstants.EMP_EFFECT_SHUTDOWN, durationTurns, rollValue, modifier);
    }

    /**
     * @return true if this result indicates no effect
     */
    public boolean isNoEffect() {
        return effect == MMConstants.EMP_EFFECT_NONE;
    }

    /**
     * @return true if this result indicates interference effect
     */
    public boolean isInterference() {
        return effect == MMConstants.EMP_EFFECT_INTERFERENCE;
    }

    /**
     * @return true if this result indicates shutdown effect
     */
    public boolean isShutdown() {
        return effect == MMConstants.EMP_EFFECT_SHUTDOWN;
    }
}
