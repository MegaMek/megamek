/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.actions;

import java.io.Serial;

import megamek.common.Hex;
import megamek.common.enums.BuildingType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;
import megamek.common.units.Terrains;
import megamek.common.units.TripodMek;

/**
 * The entity tries to find a club.
 *
 * @author Ben
 * @since April 5, 2002, 4:00 PM
 */
public class FindClubAction extends AbstractEntityAction {
    @Serial
    private static final long serialVersionUID = -8948591442556777640L;

    public FindClubAction(int entityId) {
        super(entityId);
    }

    /**
     * @param game The current {@link Game}
     *
     * @return whether an entity can find a club in its current location
     */
    public static boolean canMekFindClub(Game game, int entityId) {
        final Entity entity = game.getEntity(entityId);
        // Only biped and tripod 'Meks qualify at all.
        if (!(entity instanceof BipedMek || entity instanceof TripodMek)) {
            return false;
        }

        if (!game.hasBoardLocation(entity.getBoardLocation()) || entity.isShutDown() || !entity.getCrew().isActive()) {
            return false;
        }

        // Prevent finding a club if sprinting
        if (entity.moved == EntityMovementType.MOVE_SPRINT) {
            return false;
        }
        // Check game options
        if (game.getOptions().booleanOption(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL)
              && entity.getCrew().isClanPilot()) {
            return false;
        }

        final Hex hex = game.getHex(entity.getBoardLocation());
        // The hex must contain woods or rubble from a medium, heavy, or hardened building, or a blown off limb
        if ((hex.terrainLevel(Terrains.WOODS) < 1)
              && (hex.terrainLevel(Terrains.JUNGLE) < 1)
              && (hex.terrainLevel(Terrains.RUBBLE) < BuildingType.MEDIUM.getTypeValue())
              && (hex.terrainLevel(Terrains.ARMS) < 1)
              && (hex.terrainLevel(Terrains.LEGS) < 1)) {
            return false;
        }

        // also, need shoulders and hands; Claws can substitute as hands --Torren
        if (!entity.hasWorkingSystem(Mek.ACTUATOR_SHOULDER, Mek.LOC_RIGHT_ARM)
              || !entity.hasWorkingSystem(Mek.ACTUATOR_SHOULDER, Mek.LOC_LEFT_ARM)
              || (!entity.hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RIGHT_ARM)
              && !((Mek) entity).hasClaw(Mek.LOC_RIGHT_ARM))
              || (!entity.hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LEFT_ARM)
              && !((Mek) entity).hasClaw(Mek.LOC_LEFT_ARM))) {
            return false;
        }

        // check for no/minimal arms quirk
        if (entity.hasQuirk(OptionsConstants.QUIRK_NEG_NO_ARMS)) {
            return false;
        }

        // and last, check if you already have a club, greedy
        return entity.getClubs().isEmpty();
    }
}
