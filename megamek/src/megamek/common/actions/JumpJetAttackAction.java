/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2006-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.ToHitData;
import megamek.common.compute.ComputeSideTable;
import megamek.common.equipment.GunEmplacement;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;

/**
 * The attacker kicks the target.
 */
public class JumpJetAttackAction extends PhysicalAttackAction {
    @Serial
    private static final long serialVersionUID = 5068155731614378911L;
    public static final int BOTH = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;

    private int leg;

    public JumpJetAttackAction(int entityId, int targetId, int leg) {
        super(entityId, targetId);
        this.leg = leg;
    }

    public JumpJetAttackAction(int entityId, int targetType, int targetId,
          int leg) {
        super(entityId, targetType, targetId);
        this.leg = leg;
    }

    public int getLeg() {
        return leg;
    }

    public void setLeg(int leg) {
        this.leg = leg;
    }

    /**
     * Damage that the specified mek does with a JJ attack
     */
    public static int getDamageFor(Entity entity, int leg) {
        if (leg == BOTH) {
            return getDamageFor(entity, LEFT) + getDamageFor(entity, RIGHT);
        }

        int[] kickLegs = new int[2];
        if (entity.entityIsQuad() && !entity.isProne()) {
            kickLegs[0] = Mek.LOC_RIGHT_ARM;
            kickLegs[1] = Mek.LOC_LEFT_ARM;
        } else {
            kickLegs[0] = Mek.LOC_RIGHT_LEG;
            kickLegs[1] = Mek.LOC_LEFT_LEG;
        }

        final int legLoc = kickLegs[(leg == RIGHT) ? 0 : 1];

        // underwater damage is 0
        if (entity.getLocationStatus(legLoc) == ILocationExposureStatus.WET) {
            return 0;
        }

        int damage = 0;
        for (Mounted<?> m : entity.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_JUMP_JET) && m.isReady()
                  && m.getLocation() == legLoc) {
                damage += 3;
            }
        }

        return damage;
    }

    public ToHitData toHit(Game game) {
        return toHit(game, getEntityId(), game.getTarget(getTargetType(),
              getTargetId()), getLeg());
    }

    /**
     * To-hit number for the specified leg to kick
     *
     * @param game The current {@link Game}
     */
    public static ToHitData toHit(Game game, int attackerId, Targetable target, int leg) {
        final Entity ae = game.getEntity(attackerId);
        if (ae == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "You can't attack from a null entity!");
        }

        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_JUMP_JET_ATTACK)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "no Jump Jet attack");
        }

        String impossible = toHitIsImpossible(game, ae, target);
        if (impossible != null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "impossible");
        }

        // LAM AirMeks can only push when grounded.
        if ((ae instanceof LandAirMek) && (ae.getConversionMode() != LandAirMek.CONV_MODE_MEK)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Can only make Jump Jet attacks in mek mode");
        }

        Hex attHex = game.getBoard().getHex(ae.getPosition());
        Hex targHex = game.getBoard().getHex(target.getPosition());
        final int attackerElevation = ae.getElevation() + attHex.getLevel();
        final int attackerHeight = attackerElevation + ae.getHeight();
        final int targetElevation = target.getElevation()
              + targHex.getLevel();
        final int targetHeight = targetElevation + target.getHeight();

        int[] kickLegs = new int[2];
        if (ae.entityIsQuad() && !ae.isProne()) {
            kickLegs[0] = Mek.LOC_RIGHT_ARM;
            kickLegs[1] = Mek.LOC_LEFT_ARM;
        } else {
            kickLegs[0] = Mek.LOC_RIGHT_LEG;
            kickLegs[1] = Mek.LOC_LEFT_LEG;
        }

        ToHitData toHit;

        // arguments legal?
        if (leg != RIGHT && leg != LEFT && leg != BOTH) {
            throw new IllegalArgumentException("Leg must be LEFT or RIGHT");
        }

        // non-meks can't kick
        if (!(ae instanceof Mek)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Non-meks can't kick");
        }

        if (leg == BOTH && !ae.isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Only prone meks can attack with both legs");
        }

        // check if legs are present & working
        if ((ae.isLocationBad(kickLegs[0]) && (leg == BOTH || leg == LEFT))
              || (ae.isLocationBad(kickLegs[1]) && (leg == BOTH || leg == RIGHT))) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Leg missing");
        }

        // check if attacker even has jump jets!
        boolean hasJJ = false;
        for (Mounted<?> m : ae.getMisc()) {
            int loc = m.getLocation();
            if (m.getType().hasFlag(MiscType.F_JUMP_JET)
                  && m.isReady()
                  && ((loc == kickLegs[0] && (leg == BOTH || leg == LEFT))
                  || (loc == kickLegs[1] && (leg == BOTH || leg == RIGHT)))) {
                hasJJ = true;
                break;
            }
        }
        if (!hasJJ) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Jump jets missing or destroyed");
        }

        if (ae.moved == EntityMovementType.MOVE_JUMP) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Attacker jumped this turn");
        }

        // check if attacker has fired leg-mounted weapons
        for (Mounted<?> mounted : ae.getWeaponList()) {
            if (mounted.isUsedThisRound()) {
                int loc = mounted.getLocation();
                if (((leg == BOTH || leg == LEFT) && loc == kickLegs[0])
                      || ((leg == BOTH || leg == RIGHT) && loc == kickLegs[1])) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                          "Weapons fired from leg this turn");
                }
            }
        }

        // check range
        final int range = ae.getPosition().distance(target.getPosition());
        if (1 != range) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Enemy must be at range 1");
        }

        // check elevation
        if (!ae.isProne() && attackerHeight - targetHeight != 1) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Target elevation not in range");
        }
        if (ae.isProne()
              && (attackerHeight > targetHeight || attackerHeight < targetElevation)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Target elevation not in range");
        }

        // check facing
        if (!ae.isProne()) {
            if (!target.getPosition().equals(
                  ae.getPosition().translated(ae.getFacing()))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      "Target not directly ahead of feet");
            }
        } else {
            if (!target.getPosition().equals(
                  ae.getPosition().translated((3 + ae.getFacing()) % 6))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      "Target not directly behind of feet");
            }
        }

        // Attacks against adjacent buildings automatically hit.
        if (target.getTargetType() == Targetable.TYPE_BUILDING
              || target.getTargetType() == Targetable.TYPE_FUEL_TANK
              || target.isBuildingEntityOrGunEmplacement()) {
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                  "Targeting adjacent building.");
        }

        // Set the base BTH
        int base = ae.getCrew().getPiloting();

        // Start the To-Hit
        toHit = new ToHitData(base, "base");
        toHit.addModifier(+2, "Jump Jet");

        setCommonModifiers(toHit, game, ae, target);

        // +2 for prone
        if (ae.isProne()) {
            toHit.addModifier(2, "Attacker is prone");
        }

        // factor in target side
        toHit.setSideTable(ComputeSideTable.sideTable(ae, target));

        // done!
        return toHit;
    }
}
