/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */

package megamek.common.actions;

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Player;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;

/**
 * The prone attacker thrashes at the target.
 */
public class ThrashAttackAction extends AbstractAttackAction {

    /**
     * 
     */
    private static final long serialVersionUID = -1527653560370040648L;

    public ThrashAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }

    public ThrashAttackAction(int entityId, int targetType, int targetId) {
        super(entityId, targetType, targetId);
    }

    public ThrashAttackAction(int entityId, Targetable target) {
        super(entityId, target.getTargetType(), target.getTargetId());
    }

    /**
     * To-hit number for thrashing attack. This attack can only be made by a
     * prone Mek in a clear or pavement terrain hex that contains infantry. This
     * attack will force a PSR check for the prone Mek; if the PSR is missed,
     * the Mek takes normal falling damage.
     * 
     * @param game - the <code>IGame</code> object containing all entities.
     * @return the <code>ToHitData</code> containing the target roll.
     */
    public ToHitData toHit(IGame game) {
        final Entity ae = getEntity(game);
        final Targetable target = getTarget(game);
        // arguments legal?
        if (ae == null || target == null) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }

        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }
        
        if (!game.getOptions().booleanOption("friendly_fire")) {
            // a friendly unit can never be the target of a direct attack.
            if (target.getTargetType() == Targetable.TYPE_ENTITY
                    && (((Entity)target).getOwnerId() == ae.getOwnerId()
                            || (((Entity)target).getOwner().getTeam() != Player.TEAM_NONE
                                    && ae.getOwner().getTeam() != Player.TEAM_NONE
                                    && ae.getOwner().getTeam() == ((Entity)target).getOwner().getTeam())))
                return new ToHitData(TargetRoll.IMPOSSIBLE, "A friendly unit can never be the target of a direct attack.");
        }

        // Non-mechs can't thrash.
        if (!(ae instanceof Mech)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Only mechs can thrash at infantry");
        }

        // Mech must be prone.
        if (!ae.isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Only prone mechs can thrash at infantry");
        }

        // Can't thrash against non-infantry
        if (te == null || !(te instanceof Infantry)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Can only thrash at infantry");
        }

        // Can't thrash against swarming infantry.
        else if (Entity.NONE != te.getSwarmTargetId()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Can't thrash at swarming infantry");
        }

        // Check range.
        if (target.getPosition() == null
                || ae.getPosition().distance(target.getPosition()) > 0) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target not in same hex");
        }

        if (target.getElevation() != ae.getElevation()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target not at same elevation");
        }

        // Check terrain.
        IHex hex = game.getBoard().getHex(ae.getPosition());
        if (hex.containsTerrain(Terrains.WOODS)
                || hex.containsTerrain(Terrains.JUNGLE)
                || hex.containsTerrain(Terrains.ROUGH)
                || hex.containsTerrain(Terrains.RUBBLE)
                || hex.containsTerrain(Terrains.FUEL_TANK)
                || hex.containsTerrain(Terrains.BUILDING)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Not a clear or pavement hex.");
        }

        // Can't target woods or a building with a thrash attack.
        if (target.getTargetType() == Targetable.TYPE_BUILDING
                || target.getTargetType() == Targetable.TYPE_BLDG_IGNITE
                || target.getTargetType() == Targetable.TYPE_FUEL_TANK
                || target.getTargetType() == Targetable.TYPE_FUEL_TANK_IGNITE
                || target.getTargetType() == Targetable.TYPE_HEX_CLEAR
                || target.getTargetType() == Targetable.TYPE_HEX_IGNITE) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Invalid attack");
        }

        // The Mech can't have fired a weapon this round.
        for (int loop = 0; loop < ae.locations(); loop++) {
            if (ae.weaponFiredFrom(loop)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "Weapons fired from " + ae.getLocationName(loop)
                                + " this turn");
            }
        }

        // Mech must have at least one working arm or leg.
        if (ae.isLocationBad(Mech.LOC_RARM) && ae.isLocationBad(Mech.LOC_LARM)
                && ae.isLocationBad(Mech.LOC_RLEG)
                && ae.isLocationBad(Mech.LOC_LLEG)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Mech has no arms or legs to thrash");
        }

        // If the attack isn't impossible, it's automatically successful.
        return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                "thrash attacks always hit");
    }

    /**
     * Damage caused by a successfull thrashing attack.
     * 
     * @param entity - the <code>Entity</code> conducting the thrash attack.
     * @return The <code>int</code> amount of damage caused by this attack.
     */
    public static int getDamageFor(Entity entity) {
        int nDamage = Math.round(entity.getWeight() / 3.0f);
        return nDamage;
    }

}
