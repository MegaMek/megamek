/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.actions;

import megamek.common.BipedMech;
import megamek.common.Building;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Hex;
import megamek.common.Mech;
import megamek.common.Terrains;
import megamek.common.TripodMech;
import megamek.common.options.OptionsConstants;

/**
 * The entity tries to find a club.
 *
 * @author Ben
 * @since April 5, 2002, 4:00 PM
 */
public class FindClubAction extends AbstractEntityAction {
    private static final long serialVersionUID = -8948591442556777640L;

    /**
     * Creates new FindClubAction
     */
    public FindClubAction(int entityId) {
        super(entityId);
    }

    /**
     * @param game The current {@link Game}
     * @return whether an entity can find a club in its current location
     */
    public static boolean canMechFindClub(Game game, int entityId) {
        final Entity entity = game.getEntity(entityId);
        if ((null == entity) || null == (entity.getPosition())) {
            return false;
        }
        final Hex hex = game.getBoard().getHex(entity.getPosition());

        // Only biped and tripod 'Mechs qualify at all.
        if (!(entity instanceof BipedMech || entity instanceof TripodMech)) {
            return false;
        }

        // Is the entity active?
        if (entity.isShutDown() || !entity.getCrew().isActive()) {
            return false;
        }

        // Check game options
        if (game.getOptions().booleanOption(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL) && entity.isClan()) {
            return false;
        }

        // The hex must contain woods or rubble from
        // a medium, heavy, or hardened building,
        // or a blown off limb
        if ((hex.terrainLevel(Terrains.WOODS) < 1)
            && (hex.terrainLevel(Terrains.JUNGLE) < 1)
            && (hex.terrainLevel(Terrains.RUBBLE) < Building.MEDIUM)
            && (hex.terrainLevel(Terrains.ARMS) < 1)
            && (hex.terrainLevel(Terrains.LEGS) < 1)) {
            return false;
        }

        // also, need shoulders and hands
        // Claws can subtitue as hands --Torren
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_RARM)
                || !entity.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_LARM)
                || (!entity.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM) && !((Mech) entity).hasClaw(Mech.LOC_RARM))
                || (!entity.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM) && !((Mech) entity).hasClaw(Mech.LOC_LARM))) {
            return false;
        }

        // check for no/minimal arms quirk
        if (entity.hasQuirk(OptionsConstants.QUIRK_NEG_NO_ARMS)) {
            return false;
        }

        // and last, check if you already have a club, greedy
        if (!entity.getClubs().isEmpty()) {
            return false;
        }

        return true;
    }

}
