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

package megamek.common.units;

import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;

/**
 * The questions about automatic ejection that the deployment warning, the dialog that goes with it and the server all
 * have to answer the same way.
 * <p>
 * Only BattleMeks and aerospace units carry the setting. It has a master switch and, when the Conditional Ejection
 * game option is in play, a set of individual triggers; a unit ejects on its own only when the master switch is on
 * and, under that option, at least one trigger is armed as well.
 */
public final class AutomaticEjectionRules {

    private AutomaticEjectionRules() {
    }

    /**
     * Whether this unit has an ejection system at all, and so has a setting worth showing the player.
     *
     * @param entity the unit to check, or {@code null}
     *
     * @return {@code true} for BattleMeks and aerospace units
     */
    public static boolean hasEjectionSystem(@Nullable Entity entity) {
        return (entity instanceof Mek) || (entity instanceof Aero);
    }

    /**
     * Whether this unit is set to throw its crew out on its own initiative.
     * <p>
     * This mirrors the test the server makes before ejecting anyone, so the warning cannot promise an ejection the
     * server would not perform, or stay quiet about one it would.
     *
     * @param entity the unit to check, or {@code null}
     * @param game   the game whose options decide whether the individual triggers matter
     *
     * @return {@code true} if the unit would eject its crew automatically
     */
    public static boolean willEjectAutomatically(@Nullable Entity entity, Game game) {
        boolean isConditionalEjectionInPlay =
              game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION);
        if (entity instanceof Mek mek) {
            boolean isAnyTriggerArmed = mek.isCondEjectAmmo() || mek.isCondEjectEngine()
                  || mek.isCondEjectCTDest() || mek.isCondEjectHeadshot();
            return mek.isAutoEject() && (!isConditionalEjectionInPlay || isAnyTriggerArmed);
        }
        if (entity instanceof Aero aero) {
            boolean isAnyTriggerArmed = aero.isCondEjectAmmo() || aero.isCondEjectFuel()
                  || aero.isCondEjectSIDest();
            return aero.isAutoEject() && (!isConditionalEjectionInPlay || isAnyTriggerArmed);
        }
        return false;
    }

    /**
     * Turns this unit's master ejection switch on or off.
     *
     * @param entity      the unit to change, or {@code null}
     * @param shouldEject {@code true} to eject the crew automatically, {@code false} to ride the damage out
     *
     * @return {@code true} if the unit had a setting to change, {@code false} if it has no ejection system
     */
    public static boolean setAutomaticEjection(@Nullable Entity entity, boolean shouldEject) {
        if (entity instanceof Mek mek) {
            mek.setAutoEject(shouldEject);
            return true;
        }
        if (entity instanceof Aero aero) {
            aero.setAutoEject(shouldEject);
            return true;
        }
        return false;
    }
}
