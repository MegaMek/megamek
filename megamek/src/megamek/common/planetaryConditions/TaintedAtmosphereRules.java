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

package megamek.common.planetaryConditions;

import java.util.ArrayList;
import java.util.List;

import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeArc;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Mek;
import megamek.common.units.Tank;

/**
 * The Tainted and Toxic Atmospheres rules, TO:AR 6th printing p.54, expressed as plain questions about an
 * {@link AtmosphericTaint}.
 * <p>
 * These live in a shared class rather than on the server so that the client asks exactly the same questions the
 * server enforces: the unit tooltip, the lobby warning and the resolution of a breach all read the same answers.
 */
public final class TaintedAtmosphereRules {

    /**
     * What happens to a vehicle crew when the vehicle's armor is breached, which differs by how the air is fouled.
     */
    public enum VehicleBreachEffect {
        /** The breach has no crew effect in this atmosphere. */
        NONE,
        /** The crew is stunned, as a Crew Stunned critical hit. */
        CREW_STUNNED,
        /** The crew is killed outright, which destroys the vehicle. */
        CREW_KILLED
    }

    /**
     * Turns a conventional infantry platoon can stay in the open in a radiological or poisonous tainted atmosphere
     * before the air starts killing it, TO:AR p.54.
     */
    public static final int INFANTRY_FIELD_EXPOSURE_LIMIT_TURNS = 30;

    /**
     * Turns a vehicle without Environmental Sealing can stay in the field in a radiological or poisonous tainted
     * atmosphere before its crew dies, TO:AR p.54.
     */
    public static final int VEHICLE_FIELD_EXPOSURE_LIMIT_TURNS = 90;

    /** Number of D6 of damage a platoon takes each round once it has been in the open past the exposure limit. */
    public static final int INFANTRY_EXPOSURE_DAMAGE_DICE = 1;

    /** 2D6 target at or above which a battle armor trooper whose suit was damaged is killed by the air. */
    public static final int BATTLE_ARMOR_SUIT_BREACH_TARGET = 9;

    /** The same target for a HarJel-equipped suit, which reseals itself and so needs a worse roll to fail. */
    public static final int BATTLE_ARMOR_SUIT_BREACH_TARGET_HARJEL = 10;

    /** Heat at or above which a heat-tracking unit may set fire to its own hex in a flammable atmosphere. */
    public static final int SPONTANEOUS_IGNITION_HEAT_THRESHOLD = 15;

    /** 2D6 target at or above which a hot unit sets fire to its own hex in a flammable atmosphere. */
    public static final int SPONTANEOUS_IGNITION_TARGET = 10;

    /** 2D6 target at or above which jump jets set fire to a liftoff or landing hex in a flammable tainted atmosphere. */
    public static final int JUMP_JET_IGNITION_TARGET = 7;

    /** The same target for infantry expending Jumping MP, who kick up far less flame. */
    public static final int INFANTRY_JUMP_IGNITION_TARGET = 9;

    /**
     * 2D6 target at or above which the exhaust of a jet-propelled craft taking off or landing sets its rear arc alight
     * in a flammable atmosphere.
     */
    public static final int EXHAUST_WASH_IGNITION_TARGET = 6;

    /** How many hexes behind a taking-off or landing craft its exhaust reaches. */
    public static final int EXHAUST_WASH_RANGE = 2;

    /**
     * How many rows better a weapon attack against conventional infantry counts on the Non-Infantry Weapon Damage
     * Against Infantry Table (TW p.217) in a flammable tainted atmosphere.
     */
    public static final int INFANTRY_DAMAGE_CLASS_SHIFT = 2;

    /**
     * The 2D6 roll at or below which a weapon attack in a flammable atmosphere gets as far as rolling for ignition at
     * all. This is MegaMek's standard accidental-fire threshold, the same one a shot that strays off its target uses.
     */
    public static final int ACCIDENTAL_FIRE_CHECK_TARGET = 3;

    private TaintedAtmosphereRules() {
    }

    /**
     * Whether this atmosphere attacks the people inside a unit rather than the ground under it. Caustic air burns them
     * and radiological or poisonous air poisons them; flammable air does neither, and only makes the battlefield
     * easier to set on fire (TO:AR p.54). Every rule about crews, troopers and hull breaches turns on this.
     *
     * @param atmosphericTaint the air being fought in
     *
     * @return {@code true} if the air itself is dangerous to people
     */
    public static boolean isHarmfulToPersonnel(AtmosphericTaint atmosphericTaint) {
        return atmosphericTaint.isCaustic() || atmosphericTaint.isRadiological();
    }

    /**
     * Whether every conventional infantry platoon in play must be XCT Troops equipped for this atmosphere, TO:AR p.54.
     * A flammable atmosphere makes no such demand: it burns the ground, not the troops.
     *
     * @param atmosphericTaint the air being fought in
     *
     * @return {@code true} if unequipped conventional infantry may not be fielded
     */
    public static boolean requiresXctInfantry(AtmosphericTaint atmosphericTaint) {
        return isHarmfulToPersonnel(atmosphericTaint);
    }

    /**
     * Whether this unit is a jet-propelled craft that may not take the field at all in this atmosphere.
     * <p>
     * TO:AR p.54, the flammable toxic row: <i>"Aerospace and other jet-propelled units may not launch in lower
     * atmosphere."</i>
     * <p>
     * Because of this, it was easier to prohibit aerospace units - fighters and DropShips - from the map completely,
     * rather than model a craft that may be present but can never move. One deployed on the ground could never
     * launch, and one that flew in and landed would be in the same position: a unit sitting there permanently
     * immobile, which reads to a player as a bug rather than as a rule. Keeping them off the field entirely says the
     * same thing more clearly.
     * <p>
     * The rules do carry an exception, and it is kept. Fixed-Wing Support vehicles built with the <b>Prop</b>,
     * <b>Ultra-Light</b> or <b>VSTOL</b> chassis modification may still fly, as may <b>VTOLs</b> - the latter are
     * vehicles rather than aerospace units, so they never reach this check at all.
     *
     * @param entity           the unit being fielded
     * @param atmosphericTaint the air it would be fielded in
     *
     * @return {@code true} if this unit may not be fielded
     */
    public static boolean barsJetPropelledCraft(Entity entity, AtmosphericTaint atmosphericTaint) {
        return prohibitsLaunching(atmosphericTaint) && isJetPropelled(entity);
    }

    /**
     * Whether a vehicle without the Environmental Sealing chassis modification may not be fielded at all, TO:AR p.54.
     * Only a toxic caustic or radiological atmosphere goes this far.
     *
     * @param atmosphericTaint the air being fought in
     *
     * @return {@code true} if unsealed vehicles are barred from the field
     */
    public static boolean barsUnsealedVehicles(AtmosphericTaint atmosphericTaint) {
        return atmosphericTaint.isToxic() && isHarmfulToPersonnel(atmosphericTaint);
    }

    /**
     * Whether a vehicle in this atmosphere makes hull breach checks at all. Breaches are only rolled where the rules
     * give them an effect: a caustic atmosphere at either strength, and a toxic radiological one. A radiological
     * tainted atmosphere kills vehicle crews through the 90-turn exposure limit instead, and a flammable atmosphere
     * says nothing about breaches at all.
     *
     * @param atmosphericTaint the air the vehicle is operating in
     *
     * @return {@code true} if breach checks should be made on this vehicle's locations
     */
    public static boolean causesVehicleBreaches(AtmosphericTaint atmosphericTaint) {
        return atmosphericTaint.isCaustic() || (atmosphericTaint.isRadiological() && atmosphericTaint.isToxic());
    }

    /**
     * Whether a BattleMek in this atmosphere makes a hull breach check on its head. Only cockpit breaches have any
     * effect on Meks (TO:AR p.54), and the only breach effect the table gives for a Mek is the toxic one, so no
     * check is made in a merely tainted atmosphere.
     *
     * @param atmosphericTaint the air the Mek is operating in
     *
     * @return {@code true} if a breach check should be made on the Mek's head
     */
    public static boolean causesMekCockpitBreaches(AtmosphericTaint atmosphericTaint) {
        return atmosphericTaint.isToxic() && isHarmfulToPersonnel(atmosphericTaint);
    }

    /**
     * Whether one location of one unit is open to the fouled air, and so should have breach checks made against it.
     * A vehicle exposes every location; a BattleMek exposes only its head, because TO:AR p.54 states that on Meks
     * only cockpit breaches have any effect. Every other unit type is sealed by construction and is never exposed.
     *
     * @param entity           the unit standing in the atmosphere
     * @param location         the location being checked
     * @param atmosphericTaint the air the unit is operating in
     *
     * @return {@code true} if this location should be treated as exposed to the atmosphere
     */
    public static boolean isLocationExposedToTaint(Entity entity, int location, AtmosphericTaint atmosphericTaint) {
        if (atmosphericTaint.isBreathable()) {
            return false;
        }
        if (entity instanceof Tank) {
            return causesVehicleBreaches(atmosphericTaint);
        }
        if (entity instanceof Mek) {
            return (location == Mek.LOC_HEAD) && causesMekCockpitBreaches(atmosphericTaint);
        }
        return false;
    }

    /**
     * What a breach of a vehicle's armor does to its crew, TO:AR p.54. Caustic air at the tainted level stuns them;
     * caustic or radiological air at the toxic level kills them outright.
     *
     * @param atmosphericTaint the air the vehicle is operating in
     *
     * @return the effect to apply to the crew
     */
    public static VehicleBreachEffect getVehicleBreachEffect(AtmosphericTaint atmosphericTaint) {
        if (atmosphericTaint.isToxic() && isHarmfulToPersonnel(atmosphericTaint)) {
            return VehicleBreachEffect.CREW_KILLED;
        }
        if (atmosphericTaint == AtmosphericTaint.CAUSTIC_TAINTED) {
            return VehicleBreachEffect.CREW_STUNNED;
        }
        return VehicleBreachEffect.NONE;
    }

    /**
     * Whether a MekWarrior or aerospace pilot takes one extra hit when the Cockpit or Crew location is damaged in
     * combat. Caustic air gets into the damaged cockpit and burns the pilot, TO:AR p.54 - printed on the tainted
     * row and carried up to the toxic one, which is the same taint at a worse level.
     *
     * @param atmosphericTaint the air the unit is operating in
     *
     * @return {@code true} if an extra crew hit should be applied
     */
    public static boolean causesExtraCockpitCrewHit(AtmosphericTaint atmosphericTaint) {
        return atmosphericTaint.isCaustic();
    }

    /**
     * How many D6 of extra damage a weapon attack does to a conventional infantry platoon, over and above the attack's
     * own damage. Caustic air adds 1D6, applied as though it came from another infantry unit so that the
     * Non-Infantry Weapon Damage Against Infantry Table does not reduce it (TO:AR p.54, TW p.216).
     * <p>
     * Printed on the tainted row and carried up to the toxic one, because toxic air is the same taint at a worse
     * level rather than a different taint: everything the tainted row does, the toxic row does too.
     *
     * @param atmosphericTaint the air the platoon is standing in
     *
     * @return the number of D6 to add, or {@code 0} when the air adds nothing
     */
    public static int getExtraInfantryAttackDamageDice(AtmosphericTaint atmosphericTaint) {
        return atmosphericTaint.isCaustic() ? 1 : 0;
    }

    /**
     * Whether conventional infantry take double damage from everything in this atmosphere, the way they already do in
     * a vacuum. Radiological or poisonous air does this, TO:AR p.54 - printed on the tainted row and carried up to
     * the toxic one, which is the same taint at a worse level.
     *
     * @param atmosphericTaint the air the platoon is standing in
     *
     * @return {@code true} if damage to conventional infantry should be doubled
     */
    public static boolean doublesInfantryDamage(AtmosphericTaint atmosphericTaint) {
        return atmosphericTaint.isRadiological();
    }

    /**
     * Whether a conventional infantry platoon is sheltered from the slow effects of the air. The rules let a platoon
     * wait out a tainted atmosphere inside a fortress or Castles Brian hex, or inside a vehicle with the Environmental
     * Sealing chassis modification, so the exposure clock only runs while the platoon is out in the open.
     *
     * @param entity the platoon to check
     *
     * @return {@code true} if the platoon is currently sheltered from the atmosphere
     */
    public static boolean isShelteredFromAtmosphere(Entity entity) {
        if (entity.getTransportId() != Entity.NONE) {
            Entity transport = entity.getGame().getEntity(entity.getTransportId());
            return (transport != null) && transport.hasEnvironmentalSealing();
        }
        return isInFortifiedBuilding(entity);
    }

    /**
     * Whether a unit is standing inside a building solid enough to keep the atmosphere out. The rules name "a fortress
     * or Castles Brian building hex" (TO:AR p.54); MegaMek's closest equivalent is a hardened building, so an ordinary
     * office block is no shelter from the air.
     *
     * @param entity the unit to check
     *
     * @return {@code true} if the unit is inside a hardened building
     */
    private static boolean isInFortifiedBuilding(Entity entity) {
        Game game = entity.getGame();
        Coords position = entity.getPosition();
        if ((game == null) || (position == null)) {
            return false;
        }
        if (!Compute.isInBuilding(game, entity)) {
            return false;
        }
        IBuilding building = game.getBuildingAt(position, entity.getBoardId()).orElse(null);
        return (building != null) && (building.getBuildingType() == BuildingType.HARDENED);
    }

    /**
     * How many rows better an attack on conventional infantry counts on the Non-Infantry Weapon Damage Against
     * Infantry Table. A flammable atmosphere spreads the attack out, moving it two rows down the table towards
     * area-effect (TO:AR p.54).
     * <p>
     * Printed on the tainted row and carried up to the toxic one. This is what keeps toxic air from being gentler
     * than tainted air: a cluster missile attack shifts up to area-effect at either strength, and an area-effect
     * attack is then exempt from the toxic row's infantry-origin rule, so it keeps its doubled damage.
     *
     * @param atmosphericTaint the air the platoon is standing in
     *
     * @return the number of rows to shift, or {@code 0} when the air changes nothing
     */
    public static int getInfantryDamageClassShift(AtmosphericTaint atmosphericTaint) {
        return atmosphericTaint.isFlammable() ? INFANTRY_DAMAGE_CLASS_SHIFT : 0;
    }

    /**
     * Whether a non-infantry weapon attack on conventional infantry is resolved as though another infantry unit had
     * made it, so that its damage is applied point for point instead of being read off the Non-Infantry Weapon Damage
     * Against Infantry Table. Flammable toxic air does this to everything except area-effect attacks (TO:AR p.54).
     *
     * @param atmosphericTaint the air the platoon is standing in
     *
     * @return {@code true} if the attack should be treated as infantry-on-infantry damage
     */
    public static boolean treatsAttacksOnInfantryAsInfantryDamage(AtmosphericTaint atmosphericTaint) {
        return atmosphericTaint == AtmosphericTaint.FLAMMABLE_TOXIC;
    }

    /**
     * Whether jump jets set the liftoff and landing hexes alight without a roll. Flammable toxic air ignites on
     * contact with jump jet exhaust (TO:AR p.54); flammable tainted air only risks it, on a 2D6 roll.
     *
     * @param atmosphericTaint the air the unit is jumping through
     *
     * @return {@code true} if a fire starts automatically
     */
    public static boolean jumpJetsAlwaysIgnite(AtmosphericTaint atmosphericTaint) {
        return atmosphericTaint == AtmosphericTaint.FLAMMABLE_TOXIC;
    }

    /**
     * The 2D6 target at or above which a jumping unit sets fire to the hex it lifts off from or lands in, in a
     * flammable tainted atmosphere. Infantry expending Jumping MP need a worse roll than a jump-jet unit.
     *
     * @param isConventionalInfantry {@code true} when the jumping unit is a conventional infantry platoon
     *
     * @return the target number for the ignition roll
     */
    public static int getJumpIgnitionTarget(boolean isConventionalInfantry) {
        return isConventionalInfantry ? INFANTRY_JUMP_IGNITION_TARGET : JUMP_JET_IGNITION_TARGET;
    }

    /**
     * Whether a heat-tracking unit standing still can set fire to the hex it occupies.
     * <p>
     * Like the exhaust-wash rule, TO:AR p.54 prints this on the flammable tainted row and the toxic row simply does
     * not mention it, which would leave the more flammable air setting less alight. It is read here as the same
     * omission and applies at both strengths, at the same target number.
     *
     * @param atmosphericTaint the air the unit is standing in
     *
     * @return {@code true} if hot units should roll to ignite their own hex
     */
    public static boolean causesSpontaneousIgnition(AtmosphericTaint atmosphericTaint) {
        return atmosphericTaint.isFlammable();
    }

    /**
     * Whether the exhaust of a jet-propelled craft taking off or landing can set the ground behind it alight.
     * <p>
     * TO:AR p.54 gives this rule on the flammable tainted row only. The flammable toxic row replaces it with a flat
     * ban on launching and then says nothing about landing, which would leave the worse atmosphere doing less: a craft
     * may still land in toxic air, and its exhaust is no cooler for the air being fouler. That is read here as an
     * oversight, so the rule applies at both strengths.
     *
     * @param atmosphericTaint the air the craft is taking off from or landing in
     *
     * @return {@code true} if the craft should roll for its exhaust wash
     */
    public static boolean causesExhaustWashIgnition(AtmosphericTaint atmosphericTaint) {
        return atmosphericTaint.isFlammable();
    }

    /**
     * The hexes a jet-propelled craft's exhaust washes over when it takes off or lands: everything in its rear arc out
     * to two hexes, TO:AR p.54. The craft's own hex is not included, and the result takes no account of the board, so
     * the caller must still drop any hex that is off the map or will not burn.
     *
     * @param origin the hex the craft takes off from or comes to rest in
     * @param facing the craft's facing, which decides where its rear arc lies
     *
     * @return the hexes behind the craft that its exhaust reaches
     */
    public static List<Coords> getExhaustWashCoords(Coords origin, int facing) {
        List<Coords> washedCoords = new ArrayList<>();
        for (Coords candidate : origin.allAtDistanceOrLess(EXHAUST_WASH_RANGE)) {
            boolean isTheCraftsOwnHex = candidate.equals(origin);
            boolean isBehindTheCraft = ComputeArc.isInArc(origin, facing, candidate, Compute.ARC_REAR);
            if (!isTheCraftsOwnHex && isBehindTheCraft) {
                washedCoords.add(candidate);
            }
        }
        return washedCoords;
    }

    /**
     * Whether fires started by inferno rounds and explosive ordnance spread to every adjacent hex the instant they are
     * lit. Flammable toxic air does this, TO:AR p.54.
     *
     * @param atmosphericTaint the air the fire is burning in
     *
     * @return {@code true} if such a fire spreads to all adjacent hexes immediately
     */
    public static boolean spreadsExplosiveFiresInstantly(AtmosphericTaint atmosphericTaint) {
        return atmosphericTaint == AtmosphericTaint.FLAMMABLE_TOXIC;
    }

    /**
     * Whether this unit gets into the air on jet thrust, which is what a flammable toxic atmosphere will not tolerate
     * (TO:AR p.54, "Aerospace and other jet-propelled units may not launch in lower atmosphere").
     * <p>
     * Aerospace units do by default. The exception is a Fixed-Wing Support vehicle built with one of three chassis
     * modifications - <b>Prop</b>, <b>Ultra-Light</b> or <b>VSTOL</b> - none of which raises the jet exhaust the rule
     * is concerned with.
     * <p>
     * VTOLs need no exemption here. They are vehicles rather than aerospace units, so they never reach this check and
     * their rotors are free to fly in air that would not tolerate a jet.
     *
     * @param entity the unit that wants to fly
     *
     * @return {@code true} if this unit launches on jet thrust
     */
    public static boolean isJetPropelled(Entity entity) {
        if (!entity.isAero()) {
            return false;
        }
        boolean isExemptFixedWingSupport = entity.hasMisc(EquipmentTypeLookup.PROP_CHASSIS_MOD)
              || entity.hasMisc(EquipmentTypeLookup.ULTRALIGHT_CHASSIS_MOD)
              || entity.hasMisc(EquipmentTypeLookup.VSTOL_CHASSIS_MOD);
        return !isExemptFixedWingSupport;
    }

    /**
     * Whether jet-propelled units are barred from launching in the lower atmosphere. Flammable toxic air would be set
     * alight by the exhaust, TO:AR p.54.
     *
     * @param atmosphericTaint the air the unit would launch into
     *
     * @return {@code true} if launching is prohibited
     */
    public static boolean prohibitsLaunching(AtmosphericTaint atmosphericTaint) {
        return atmosphericTaint == AtmosphericTaint.FLAMMABLE_TOXIC;
    }

    /**
     * The 2D6 target at or above which a battle armor trooper whose suit has been damaged is killed by the air,
     * TO:AR p.54. A HarJel-equipped suit reseals itself, so it takes a worse roll to kill the trooper inside it.
     *
     * @param hasHarJel {@code true} when the suit mounts a working HarJel system
     *
     * @return the target number for the roll
     */
    public static int getBattleArmorSuitBreachTarget(boolean hasHarJel) {
        return hasHarJel ? BATTLE_ARMOR_SUIT_BREACH_TARGET_HARJEL : BATTLE_ARMOR_SUIT_BREACH_TARGET;
    }

    /**
     * Whether a damaged battle armor suit exposes its trooper to air that kills them. Only a toxic atmosphere is
     * strong enough, and only the caustic and radiological kinds; a flammable atmosphere does nothing to a sealed
     * suit (TO:AR p.54).
     *
     * @param atmosphericTaint the air the trooper is fighting in
     *
     * @return {@code true} if a damaged suit should trigger a kill roll
     */
    public static boolean killsBattleArmorInDamagedSuits(AtmosphericTaint atmosphericTaint) {
        return atmosphericTaint.isToxic() && isHarmfulToPersonnel(atmosphericTaint);
    }
}
