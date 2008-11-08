/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;

/**
 * The attacker brushes the target off.
 */
public class BrushOffAttackAction extends AbstractAttackAction {
    /**
     * 
     */
    private static final long serialVersionUID = -7455082808488032572L;
    public static final int BOTH = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;

    private int arm;

    public BrushOffAttackAction(int entityId, int targetType, int targetId,
            int arm) {
        super(entityId, targetType, targetId);
        this.arm = arm;
    }

    public int getArm() {
        return arm;
    }

    public void setArm(int arm) {
        this.arm = arm;
    }

    /**
     * Damage that the specified mech does with a brush off attack. This equals
     * the damage done by a punch from the same arm.
     * 
     * @param entity - the <code>Entity</code> brushing off the swarm.
     * @param arm - the <code>int</code> of the arm making the attack; this
     *            value must be <code>BrushOffAttackAction.RIGHT</code> or
     *            <code>BrushOffAttackAction.LEFT</code>.
     * @return the <code>int</code> amount of damage caused by the attack. If
     *         the attack hits, the swarming infantry takes the damage; if the
     *         attack misses, the entity deals the damage to themself.
     */
    public static int getDamageFor(Entity entity, int arm) {
        return PunchAttackAction.getDamageFor(entity, arm, false);
    }

    /**
     * To-hit number for the specified arm to brush off swarming infantry. If
     * this attack misses, the Mek will suffer punch damage. This same action is
     * used to remove iNARC pods.
     * 
     * @param game - the <code>IGame</code> object containing all entities.
     * @return the <code>ToHitData</code> containing the target roll.
     */
    public ToHitData toHit(IGame game) {
        return toHit(game, getEntityId(), game.getTarget(getTargetType(),
                getTargetId()), getArm());
    }

    /**
     * To-hit number for the specified arm to brush off swarming infantry. If
     * this attack misses, the Mek will suffer punch damage. This same action is
     * used to remove iNARC pods.
     * 
     * @param game - the <code>IGame</code> object containing all entities.
     * @param attackerId - the <code>int</code> ID of the attacking unit.
     * @param target - the <code>Targetable</code> object being targeted.
     * @param arm - the <code>int</code> of the arm making the attack; this
     *            value must be <code>BrushOffAttackAction.RIGHT</code> or
     *            <code>BrushOffAttackAction.LEFT</code>.
     * @return the <code>ToHitData</code> containing the target roll.
     */
    public static ToHitData toHit(IGame game, int attackerId,
            Targetable target, int arm) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity te = null;
        if (ae == null || target == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Attacker or target not valid");
        }
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }
        final int armLoc = (arm == BrushOffAttackAction.RIGHT) ? Mech.LOC_RARM
                : Mech.LOC_LARM;
        ToHitData toHit;

        // non-mechs can't BrushOff
        if (!(ae instanceof Mech)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Only mechs can brush off swarming infantry or iNarc Pods");
        }

        // arguments legal?
        if (arm != BrushOffAttackAction.RIGHT
                && arm != BrushOffAttackAction.LEFT) {
            throw new IllegalArgumentException("Arm must be LEFT or RIGHT");
        }
        if ((targetId != ae.getSwarmAttackerId() || te == null || !(te instanceof Infantry))
                && target.getTargetType() != Targetable.TYPE_INARC_POD) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Can only brush off swarming infantry or iNarc Pods");
        }

        // Quads can't brush off.
        if (ae.entityIsQuad()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is a quad");
        }

        // Can't brush off with flipped arms
        if (ae.getArmsFlipped()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Arms are flipped to the rear. Can not punch.");
        }

        // check if arm is present
        if (ae.isLocationBad(armLoc)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Arm missing");
        }

        // check if shoulder is functional
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, armLoc)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Shoulder destroyed");
        }

        // check if attacker has fired arm-mounted weapons
        if (ae.weaponFiredFrom(armLoc)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Weapons fired from arm this turn");
        }

        // can't physically attack mechs making dfa attacks
        if (te != null && te.isMakingDfa()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Target is making a DFA attack");
        }

        // Can't brush off while prone.
        if (ae.isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker is prone");
        }

        // Can't target woods or a building with a brush off attack.
        if (target.getTargetType() == Targetable.TYPE_BUILDING
                || target.getTargetType() == Targetable.TYPE_BLDG_IGNITE
                || target.getTargetType() == Targetable.TYPE_FUEL_TANK
                || target.getTargetType() == Targetable.TYPE_FUEL_TANK_IGNITE
                || target.getTargetType() == Targetable.TYPE_HEX_CLEAR
                || target.getTargetType() == Targetable.TYPE_HEX_IGNITE) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Invalid attack");
        }

        // okay, modifiers...
        toHit = new ToHitData(ae.getCrew().getPiloting(), "base PSR");
        toHit.addModifier(4, "brush off swarming infantry");

        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, armLoc)) {
            toHit.addModifier(2, "Upper arm actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, armLoc)) {
            toHit.addModifier(2, "Lower arm actuator missing or destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_HAND, armLoc)) {
            toHit.addModifier(1, "Hand actuator missing or destroyed");
        }

        // If it has a torso-mounted cockpit and two head sensor hits or three
        // sensor hits...
        // It gets a =4 penalty for being blind!
        if (((Mech) ae).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
            int sensorHits = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                    Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
            int sensorHits2 = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                    Mech.SYSTEM_SENSORS, Mech.LOC_CT);
            if ((sensorHits + sensorHits2) == 3) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "Sensors Completely Destroyed for Torso-Mounted Cockpit");
            } else if (sensorHits == 2) {
                toHit.addModifier(4,
                        "Head Sensors Destroyed for Torso-Mounted Cockpit");
            }
        }

        Compute.modifyPhysicalBTHForAdvantages(ae, te, toHit, game);

        // If the target has assault claws, give a 1 modifier.
        // We can stop looking when we find our first match.
        if (te != null) {
            for (Mounted mount : te.getMisc()) {
                EquipmentType equip = mount.getType();
                if (BattleArmor.ASSAULT_CLAW.equals(equip.getInternalName())) {
                    toHit.addModifier(1, "defender has assault claws");
                    break;
                }
            }
        }

        // done!
        return toHit;
    }

}
