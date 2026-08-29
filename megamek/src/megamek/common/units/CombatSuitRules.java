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
 * The optional MekWarrior Combat Suit rule: what the suit protects a crew from once they are outside their unit.
 * <p>
 * This is not a house rule. A Time of War p.294 gives the elite MekWarrior kit as a combat suit plus a combat
 * neurohelmet, and states the helmet "may be sealed in hostile environments" with its own air supply. The tabletop
 * has no equivalent - {@code MiscType} carries the suit with a damage divisor and nothing else - so this adapts the
 * roleplaying equipment to the battlefield, and does nothing unless {@link OptionsConstants#RPG_COMBAT_SUITS} is
 * switched on.
 * <p>
 * What the kit protects against follows from what it is. The helmet's own air covers anything that would otherwise
 * be breathed in, so a tainted or toxic atmosphere is survivable, and the suit is armoured cooling gear, so heat is
 * too. It covers only the torso, arms and legs and is not pressurized, so vacuum, a trace atmosphere and water are
 * left alone - holding pressure over a whole body is a different job from supplying air. Extreme cold and the
 * physical dangers of a storm or a tornado are untouched for the same reason.
 * <p>
 * Both the client and the server ask these questions, so they live here rather than in either one.
 */
public final class CombatSuitRules {

    private CombatSuitRules() {
    }

    /**
     * Whether this unit's crew can be issued a suit at all.
     * <p>
     * The suit is worn by a crew who might end up outside their unit, so it is offered to BattleMeks and
     * IndustrialMeks, to vehicle crews who abandon, and to aerospace crews who eject. Conventional infantry are
     * already covered by their own armor kits, and ProtoMeks have no ejection system to leave through.
     *
     * @param entity the unit whose crew is being configured, or {@code null}
     *
     * @return {@code true} if the crew may be issued a combat suit
     */
    public static boolean canWearCombatSuit(@Nullable Entity entity) {
        if ((entity == null) || (entity.defaultCrewType() == CrewType.NONE)) {
            return false;
        }
        return (entity instanceof Mek) || (entity instanceof Tank) || entity.isAero();
    }

    /**
     * Whether this unit's crew would leave it wearing a suit, with the optional rule in play.
     *
     * @param entity the unit the crew is aboard, or {@code null}
     * @param game   the game whose options say whether the rule is in force
     *
     * @return {@code true} if the crew are protected by a suit
     */
    public static boolean isCrewWearingCombatSuit(@Nullable Entity entity, @Nullable Game game) {
        if ((entity == null) || (game == null) || (entity.getCrew() == null)) {
            return false;
        }
        if (!game.getOptions().booleanOption(OptionsConstants.RPG_COMBAT_SUITS)) {
            return false;
        }
        return canWearCombatSuit(entity) && entity.getCrew().hasAnyCombatSuit();
    }

    /**
     * Whether a crew already outside their unit is wearing a suit.
     * <p>
     * An ejected crew carries the suit as a real armor kit, so this asks the ejected unit itself rather than looking
     * back at the ride they left.
     *
     * @param ejectedCrew the crew on the board, or {@code null}
     *
     * @return {@code true} if they are wearing a combat suit
     */
    public static boolean isWearingCombatSuit(@Nullable Entity ejectedCrew) {
        if (!(ejectedCrew instanceof ConvInfantry convInfantry)) {
            return false;
        }
        // The option is checked here rather than only where the suit is issued, because a platoon can be built
        // wearing one in MegaMekLab. Without this a game that never opted into the rule would still see the suit
        // change what its infantry survive.
        Game game = convInfantry.getGame();
        if ((game == null) || !game.getOptions().booleanOption(OptionsConstants.RPG_COMBAT_SUITS)) {
            return false;
        }
        return convInfantry.hasCombatSuit();
    }

    /**
     * Whether a suit keeps its wearer alive in air they would otherwise have to breathe.
     * <p>
     * This is the kit's own air supply doing the work, so it answers for a tainted atmosphere and a toxic one alike.
     * It says nothing about vacuum, which needs pressure held over the whole body rather than air to breathe.
     *
     * @param ejectedCrew the crew on the board, or {@code null}
     *
     * @return {@code true} if the suit protects them from the air itself
     */
    public static boolean protectsAgainstAtmosphericTaint(@Nullable Entity ejectedCrew) {
        return isWearingCombatSuit(ejectedCrew);
    }

    /**
     * Whether a suit keeps its wearer alive in extreme heat, which an armored cooling suit does. Extreme cold is a
     * different problem and the suit is not described as solving it.
     *
     * @param ejectedCrew the crew on the board, or {@code null}
     * @param temperature the temperature they are standing in
     *
     * @return {@code true} if the suit protects them from this temperature
     */
    public static boolean protectsAgainstTemperature(@Nullable Entity ejectedCrew, int temperature) {
        return (temperature > 0) && isWearingCombatSuit(ejectedCrew);
    }
}
