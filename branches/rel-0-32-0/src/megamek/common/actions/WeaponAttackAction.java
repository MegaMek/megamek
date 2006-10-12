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

import java.util.Enumeration;
import java.util.ArrayList;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.GunEmplacement;
import megamek.common.HexTarget;
import megamek.common.IAimingModes;
import megamek.common.IEntityMovementMode;
import megamek.common.IEntityMovementType;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.ILocationExposureStatus;
import megamek.common.INarcPod;
import megamek.common.Infantry;
import megamek.common.LosEffects;
import megamek.common.Mech;
import megamek.common.MechWarrior;
import megamek.common.MinefieldTarget;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.RangeType;
import megamek.common.Tank;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.VTOL;
import megamek.common.MiscType;

/**
 * Represents intention to fire a weapon at the target.
 */
public class WeaponAttackAction extends AbstractAttackAction {
    private int weaponId;
    private int ammoId = -1;
    private int aimedLocation = Mech.LOC_NONE;
    private int aimMode = IAimingModes.AIM_MODE_NONE;
    private int otherAttackInfo = -1;   // 
    private boolean nemesisConfused;
    private boolean swarmingMissiles;
    private int oldTargetId = -1;
    
    // equipment that affects this attack (AMS, ECM?, etc)
    // only used server-side
    private transient ArrayList vCounterEquipment;
    
    // default to attacking an entity
    public WeaponAttackAction(int entityId, int targetId, int weaponId) {
        super(entityId, targetId);
        this.weaponId = weaponId;
    }
    
    public WeaponAttackAction(int entityId, int targetType, int targetId, int weaponId) {
        super(entityId, targetType, targetId);
        this.weaponId = weaponId;
    }
    
    public int getWeaponId() {
        return weaponId;
    }
    
    public int getAmmoId() {
        return ammoId;
    }
    
    public int getAimedLocation() {
        return aimedLocation;
    }
    
    public int getAimingMode() {
        return aimMode;
    }
    
    public ArrayList getCounterEquipment() {
        return vCounterEquipment;
    }
    
    public void setWeaponId(int weaponId) {
        this.weaponId = weaponId;
    }
    
    public void setAmmoId(int ammoId) {
        this.ammoId = ammoId;
    }
    
    public void setAimedLocation(int aimedLocation) {
        this.aimedLocation = aimedLocation;
    }
    
    public void setAimingMode(int aimMode) {
        this.aimMode = aimMode;
    }
    
    public void addCounterEquipment(Mounted m) {
        if (vCounterEquipment == null) {
            vCounterEquipment = new ArrayList();
        }
        vCounterEquipment.add(m);
    }

    public void setOtherAttackInfo(int newInfo) {
        otherAttackInfo = newInfo;
    }

    public int getOtherAttackInfo() {
        return otherAttackInfo;
    }

    public ToHitData toHit(IGame game) {
        return WeaponAttackAction.toHit(game, getEntityId(),
                           game.getTarget(getTargetType(), getTargetId()),
                           getWeaponId(),
                           getAimedLocation(),
                           getAimingMode(), nemesisConfused, swarmingMissiles, game.getEntity(oldTargetId));
    }

    public static ToHitData toHit(IGame game, int attackerId, Targetable target, int weaponId) {
        return WeaponAttackAction.toHit(game, attackerId, target, weaponId, Mech.LOC_NONE, IAimingModes.AIM_MODE_NONE, false, false, null);
      }
    
    public static ToHitData toHit(IGame game, int attackerId, Targetable target, int weaponId, int aimingAt, int aimingMode) {
        return toHit(game, attackerId, target, weaponId, aimingAt, aimingMode, false, false, null);
    }

    /**
     * To-hit number for attacker firing a weapon at the target.
     */
    private static ToHitData toHit(IGame game, int attackerId, Targetable target, int weaponId, int aimingAt, int aimingMode, boolean isNemesisConfused, boolean exchangeSwarmTarget, Entity oldTarget) {
        final Entity ae = game.getEntity(attackerId);
        final Mounted weapon = ae.getEquipment(weaponId);
        final WeaponType wtype = (WeaponType)weapon.getType();
        if (exchangeSwarmTarget) {
            // Quick check, is the new target out of range for the weapon?
            if(RangeType.rangeBracket(ae.getPosition().distance(target.getPosition()), wtype.getRanges(),
                    game.getOptions().booleanOption("maxtech_range")) == RangeType.RANGE_OUT) {
                return new ToHitData(ToHitData.AUTOMATIC_FAIL, "swarm target out of range");
            }
            // this is a swarm attack against a new target
            // first, exchange old and new targets to get all mods
            // as if firing against old target.
            // at the end of this function, we remove target terrain
            // and movement mods, and add those for the new target
            Targetable tempTarget = target;
            target = oldTarget;
            oldTarget = (Entity)tempTarget;
        }
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }
        boolean isAttackerInfantry = ae instanceof Infantry;
        boolean isWeaponInfantry = wtype.hasFlag(WeaponType.F_INFANTRY);
        // 2003-01-02 BattleArmor MG and Small Lasers have unlimited ammo.
        // 2002-09-16 Infantry weapons have unlimited ammo.
        final boolean usesAmmo = wtype.getAmmoType() != AmmoType.T_NA &&
            wtype.getAmmoType() != AmmoType.T_BA_MG &&
            wtype.getAmmoType() != AmmoType.T_BA_SMALL_LASER &&
            !isWeaponInfantry;
        final Mounted ammo = usesAmmo ? weapon.getLinked() : null;
        final AmmoType atype = ammo == null ? null : (AmmoType)ammo.getType();
        final boolean targetInBuilding = Compute.isInBuilding( game, te );
        boolean isIndirect = wtype.hasModes()
            && weapon.curMode().equals("Indirect");
        boolean isInferno =
            atype != null && (atype.getAmmoType() == AmmoType.T_SRM || atype.getAmmoType() == AmmoType.T_BA_INFERNO) && atype.getMunitionType() == AmmoType.M_INFERNO ||
                isWeaponInfantry && wtype.hasFlag(WeaponType.F_INFERNO);
        boolean isArtilleryDirect= wtype.hasFlag(WeaponType.F_ARTILLERY) && game.getPhase() == IGame.PHASE_FIRING;
        boolean isArtilleryIndirect = wtype.hasFlag(WeaponType.F_ARTILLERY) && (game.getPhase() == IGame.PHASE_TARGETING || game.getPhase() == IGame.PHASE_OFFBOARD);//hack, otherwise when actually resolves shot labeled impossible.
        boolean isArtilleryFLAK = isArtilleryDirect &&
                                  target.getTargetType() == Targetable.TYPE_ENTITY &&
                                  te.getMovementMode() == IEntityMovementMode.VTOL &&
                                  te.getElevation() > 0 &&
                                  (usesAmmo && atype.getMunitionType() == AmmoType.M_STANDARD);
        boolean isHaywireINarced = ae.isINarcedWith(INarcPod.HAYWIRE);
        boolean isINarcGuided = false;
        boolean isECMAffected = Compute.isAffectedByECM(ae, ae.getPosition(), target.getPosition());
        boolean isTAG = wtype.hasFlag(WeaponType.F_TAG);
        boolean isHoming = false;
        if (te != null) {
            if (te.isINarcedBy(ae.getOwner().getTeam()) &&
                atype != null &&
                (atype.getAmmoType() == AmmoType.T_LRM || atype.getAmmoType() == AmmoType.T_SRM) &&
                atype.getMunitionType() == AmmoType.M_NARC_CAPABLE &&
                (wtype.getAmmoType() == AmmoType.T_LRM ||
                 wtype.getAmmoType() == AmmoType.T_SRM)) {
                isINarcGuided = true;
            }
        }
        int toSubtract = 0;
        final int ttype = target.getTargetType();
        
        ToHitData toHit = null;
        String reason=null;
        
        reason = toHitIsImpossible (game, ae, target, weapon, atype,
                                    wtype, ttype, exchangeSwarmTarget,
                                    usesAmmo, te, isTAG, isInferno, 
                                    isAttackerInfantry, isIndirect,
                                    attackerId, weaponId, isArtilleryIndirect,
                                    ammo, isArtilleryFLAK, targetInBuilding,
                                    isArtilleryDirect);
        if (reason!=null) {
            return new ToHitData(ToHitData.IMPOSSIBLE, reason);
        }
        long munition = AmmoType.M_STANDARD;
        if (atype != null) {
            munition = atype.getMunitionType();
        }
        if (munition == AmmoType.M_HOMING) {
            //target type checked later because its different for direct/indirect (BMRr p77 on board arrow IV)
            isHoming = true;
        } 
        int targEl;
    
        if (te == null) {
            targEl = game.getBoard().getHex(target.getPosition()).floor();
        } else {
            targEl = te.absHeight();
        }
    
        //TODO: mech making DFA could be higher if DFA target hex is higher
        //      BMRr pg. 43, "attacking unit is considered to be in the air
        //      above the hex, standing on an elevation 1 level higher than
        //      the target hex or the elevation of the hex the attacker is
        //      in, whichever is higher."
    
        // if we're doing indirect fire, find a spotter
        Entity spotter = null;
        if (isIndirect) {
            spotter = Compute.findSpotter(game, ae, target);
        }
    
        // EI system
        // 0 if no EI (or switched off)
        // 1 if no intervening light woods
        // 2 if intervening light woods (because target in woods + intervening woods is only +1 total)
        int eistatus=0;

        // check LOS (indirect LOS is from the spotter)
        LosEffects los;
        ToHitData losMods;
        if (!isIndirect) {
            los = LosEffects.calculateLos(game, attackerId, target);

            if(ae.hasActiveEiCockpit()) {
                if(los.getLightWoods() > 0)
                    eistatus = 2;
                else
                    eistatus = 1;
            }
            
            losMods = los.losModifiers(game, eistatus);
            if ((atype != null)
                    && ((atype.getAmmoType() == AmmoType.T_LRM_TORPEDO)
                    || (atype.getAmmoType() == AmmoType.T_SRM_TORPEDO))
                    && (los.getMinimumWaterDepth() < 1)) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Torpedos must follow water their entire LOS");
            }
        } else {
            los = LosEffects.calculateLos(game, spotter.getId(), target);
            // do not count attacker partial cover in indirect fire
            los.setAttackerCover(LosEffects.COVER_NONE);

            if(spotter.hasActiveEiCockpit()) {
                if(los.getLightWoods() > 0)
                    eistatus = 2;
                else
                    eistatus = 1;
            }

            losMods = los.losModifiers(game);
        }
        // Leg attacks, Swarm attacks, and
        // Mine Launchers don't use gunnery.
        if ( Infantry.LEG_ATTACK.equals( wtype.getInternalName() ) ) {
            toHit = Compute.getLegAttackBaseToHit( ae, te );
    
            // If the attacker has Assault claws, give a -1 modifier.
            // We can stop looking when we find our first match.
            for (Mounted mount : ae.getMisc()) {
                EquipmentType equip = mount.getType();
                if ( BattleArmor.ASSAULT_CLAW.equals
                     (equip.getInternalName()) ) {
                    toHit.addModifier( -1, "attacker has assault claws" );
                    break;
                }
            }
        } else if ( Infantry.SWARM_MEK.equals( wtype.getInternalName() ) ) {
            toHit = Compute.getSwarmMekBaseToHit( ae, te );

            // If the attacker has Assault claws, give a -1 modifier.
            // We can stop looking when we find our first match.
            for (Mounted mount : ae.getMisc()) {
                EquipmentType equip = mount.getType();
                if ( BattleArmor.ASSAULT_CLAW.equals
                     (equip.getInternalName()) ) {
                    toHit.addModifier( -1, "attacker has assault claws" );
                    break;
                }
            }
        } else if ( Infantry.STOP_SWARM.equals( wtype.getInternalName() ) ) {
            // Can't stop if we're not swarming, otherwise automatic.
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS,
                                      "End swarm attack." );
        }
        else if ( BattleArmor.MINE_LAUNCHER.equals(wtype.getInternalName()) ) {
            // Mine launchers can not hit infantry.
            toHit = new ToHitData(8, "magnetic mine attack");
        }
        // Swarming infantry always hit their target, but
        // they can only target the Mek they're swarming.
        else if ( te != null && ae.getSwarmTargetId() == te.getId() ) {
            // Only certain weapons can be used in a swarm attack.
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS,
                                      "Attack during swarm.",
                                      ToHitData.HIT_SWARM,
                                      ToHitData.SIDE_FRONT );
        }
        else if (isArtilleryFLAK) {
            toHit = new ToHitData(9, "artillery FLAK");
        } else {
            toHit = new ToHitData(ae.crew.getGunnery(), "gunnery skill");
        }

        //Engineer's fire extinguisher has fixed to hit number,
        //Note that coolant trucks make a regular attack.
        if(wtype.hasFlag(WeaponType.F_EXTINGUISHER)) {
            toHit = new ToHitData(8, "fire extinguisher");
            if(target instanceof Entity && ((Entity)target).infernos.isStillBurning()
                    || target instanceof Tank && ((Tank)target).isInfernoFire()) {
                toHit.addModifier(2, "inferno fire");
            }
            if(Targetable.TYPE_HEX_EXTINGUISH == target.getTargetType()
               && game.getBoard().isInfernoBurning(target.getPosition())) {
                    toHit.addModifier(2, "inferno fire");
            }
            return toHit;
        }

        // If a unit is suffering from electromagnetic interference, they get a blanket +2.
        // Sucks to be them.
        if (ae.isSufferingEMI())
            toHit.addModifier(+2, "electromagnetic interference");

        if ( ae.hasShield() ){
            //active shield has already been checked as it makes shots impossible
            //time to check passive defense and no defense

            if ( ae.hasPassiveShield(weapon.getLocation(),weapon.isRearMounted()))
                toHit.addModifier(+2, "weapon hampered by passive shield");
            else if ( ae.hasNoDefenseShield(weapon.getLocation()))
                toHit.addModifier(+1, "weapon hampered by shield");
        }
        // if we have BAP with MaxTech rules, and there are woods in the 
        // way, and we are within BAP range, we reduce the BTH by 1
        if (game.getOptions().booleanOption("maxtech_bap") &&
                !isIndirect &&
                te != null && ae.hasBAP() &&
                ae.getBAPRange() >= Compute.effectiveDistance(game, ae, te) &&
                !Compute.isAffectedByECM(ae,ae.getPosition(),te.getPosition()) &&
                (game.getBoard().getHex(te.getPosition())
                    .containsTerrain(Terrains.WOODS) ||
                 game.getBoard().getHex(te.getPosition())
                    .containsTerrain(Terrains.JUNGLE) ||
                 los.getLightWoods() > 0 ||
                 los.getHeavyWoods() > 0 ||
                 los.getUltraWoods() > 0) ) {

            toHit.addModifier(-1, "target in/behind woods and attacker has BAP");
        }

        // Is the pilot a weapon specialist?
        if (ae.crew.getOptions().stringOption("weapon_specialist").equals(wtype.getName())) {
            toHit.addModifier( -2, "weapon specialist" );
        }
        
        // Has the pilot the appropriate gunnery skill?
        if (ae.crew.getOptions().booleanOption("gunnery_laser") && wtype.hasFlag(WeaponType.F_ENERGY) ) {
            toHit.addModifier ( -1, "Gunnery/Laser" );
        }
              
        if (ae.crew.getOptions().booleanOption("gunnery_ballistic") && wtype.hasFlag(WeaponType.F_BALLISTIC) ) {
            toHit.addModifier ( -1, "Gunnery/Ballistic" );
        }
    
        if (ae.crew.getOptions().booleanOption("gunnery_missile") && wtype.hasFlag(WeaponType.F_MISSILE) ) {
            toHit.addModifier ( -1, "Gunnery/Missile" );
        }

        // If it has a torso-mounted cockpit and two head sensor hits or three sensor hits...
        // It gets a =4 penalty for being blind!
        if (ae instanceof Mech && ((Mech)ae).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
            int sensorHits = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
            if (sensorHits == 2) {
                toHit.addModifier(4, "Head Sensors Destroyed for Torso-Mounted Cockpit");
            }
        }
        
        // Do we use Listen-Kill ammo from War of 3039 sourcebook?
        if (!isECMAffected && atype != null
                && (atype.getAmmoType() == AmmoType.T_LRM || atype.getAmmoType() == AmmoType.T_SRM)
                && atype.getMunitionType() == AmmoType.M_LISTEN_KILL
                && !(te != null && te.isClan())) {
            toHit.addModifier(-1, "Listen-Kill ammo");            
        }

        // determine some more variables
        int aElev = ae.getElevation();
        int tElev = target.getElevation();
        int distance = Compute.effectiveDistance(game, ae, target);
    
        toHit.append(nightModifiers(game, target, atype, ae));
    
        // Handle direct artillery attacks.
        if (isArtilleryDirect) {
            if (!isArtilleryFLAK) {
                toHit.addModifier(5, "direct artillery modifer");
            }
            toHit.append(Compute.getAttackerMovementModifier(game, attackerId));
            toHit.append(losMods);
            toHit.append(Compute.getSecondaryTargetMod(game, ae, target));
            // actuator & sensor damage to attacker
            toHit.append(Compute.getDamageWeaponMods(ae, weapon));
            // heat
            if (ae.getHeatFiringModifier() != 0) {
                toHit.addModifier(ae.getHeatFiringModifier(), "heat");
            }
            // weapon to-hit modifier
            if (wtype.getToHitModifier() != 0) {
                toHit.addModifier(wtype.getToHitModifier(), "weapon to-hit modifier");
            }
    
            // ammo to-hit modifier
            if (usesAmmo && atype.getToHitModifier() != 0) {
                 toHit.addModifier(atype.getToHitModifier(),
                              "ammunition to-hit modifier");
            }
            if (isHoming) {
                return new ToHitData(4, "Homing shot");
            }

            if (game.getEntity(attackerId).getOwner().getArtyAutoHitHexes().contains(target.getPosition()) && !isArtilleryFLAK) {
                return new ToHitData(ToHitData.AUTOMATIC_SUCCESS, "Artillery firing at designated artillery target.");
            }
            return toHit;
        }
        if (isArtilleryIndirect) {
            if (isHoming) {
                return new ToHitData(4, "Homing shot (will miss if TAG misses)");
            }

            if (game.getEntity(attackerId).getOwner().getArtyAutoHitHexes().contains(target.getPosition())) {
                return new ToHitData(ToHitData.AUTOMATIC_SUCCESS, "Artillery firing at designated artillery target.");
            }
            toHit.addModifier(7, "indirect artillery modifier");
            int adjust = ae.aTracker.getModifier(weapon,target.getPosition());
            if (adjust==ToHitData.AUTOMATIC_SUCCESS) {
                return new ToHitData(ToHitData.AUTOMATIC_SUCCESS, "Artillery firing at target that's been hit before.");
            } else if (adjust!=0) {
                toHit.addModifier(adjust, "adjusted fire");
            }
            return toHit;
    
        }
    
    
        // Attacks against adjacent buildings automatically hit.
        if (distance == 1
                && (target.getTargetType() == Targetable.TYPE_BUILDING
                    || target.getTargetType() == Targetable.TYPE_BLDG_IGNITE
                    || target.getTargetType() == Targetable.TYPE_FUEL_TANK
                    || target.getTargetType() == Targetable.TYPE_FUEL_TANK_IGNITE
                    || target instanceof GunEmplacement)) {
            return new ToHitData(ToHitData.AUTOMATIC_SUCCESS,
                                  "Targeting adjacent building.");
        }
    
        // Attacks against buildings from inside automatically hit.
        if (null != los.getThruBldg()
                && (target.getTargetType() == Targetable.TYPE_BUILDING
                    || target.getTargetType() == Targetable.TYPE_BLDG_IGNITE
                    || target.getTargetType() == Targetable.TYPE_FUEL_TANK
                    || target.getTargetType() == Targetable.TYPE_FUEL_TANK_IGNITE
                    || target instanceof GunEmplacement)) {
            return new ToHitData(ToHitData.AUTOMATIC_SUCCESS,
                    "Targeting building from inside (are you SURE this is a good idea?).");
        }
    
        // add range mods
        toHit.append(Compute.getRangeMods(game, ae, weaponId, target));

        // If it's an anti-air system, add mods for that
        if (ae.getTargSysType() == MiscType.T_TARGSYS_ANTI_AIR && target instanceof Entity) {
            if (target instanceof VTOL)
                toHit.addModifier(-2, "anti-air targetting system vs. VTOL");
            else
                toHit.addModifier(1, "anti-air targetting system vs. non-aerial unit");
        }

    
        // Battle Armor targets are hard for Meks and Tanks to hit.
        if ( !isAttackerInfantry && te != null && te instanceof BattleArmor ) {
            toHit.addModifier( 1, "battle armor target" );
        }
        
        // Ejected MechWarriors are harder to hit
        if ( te != null && te instanceof MechWarrior ) {
            toHit.addModifier( 2, "ejected MechWarrior target" );
        }
    
        // Indirect fire has a +1 mod
        if (isIndirect) {
            toHit.addModifier( 1, "indirect fire" );
        }
    
        // attacker movement
        toHit.append(Compute.getAttackerMovementModifier(game, attackerId));
    
        // target movement
        if (te != null) {
            ToHitData thTemp = Compute.getTargetMovementModifier(game, target.getTargetId());
            toHit.append(thTemp);
            toSubtract += thTemp.getValue();

            // semiguided ammo negates this modifier, if TAG succeeded    
            if (atype != null && atype.getAmmoType() == AmmoType.T_LRM &&
                atype.getMunitionType() == AmmoType.M_SEMIGUIDED &&
                te.getTaggedBy() != -1) {
                int nAdjust = thTemp.getValue();
                if (nAdjust > 0) {
                    toHit.append(new ToHitData(-nAdjust, "Semi-guided ammo vs tagged target"));
                }
            }
            // precision ammo reduces this modifier
            else if (atype != null 
                    && (atype.getAmmoType() == AmmoType.T_AC
                        || atype.getAmmoType() == AmmoType.T_LAC)
                    && atype.getMunitionType() == AmmoType.M_PRECISION) {
                int nAdjust = Math.min(2, thTemp.getValue());
                if (nAdjust > 0) {
                    toHit.append(new ToHitData(-nAdjust, "Precision Ammo"));
                }
            }
        }
    
        // Armor Piercing ammo is a flat +1
        if ( atype != null
             && (atype.getAmmoType() == AmmoType.T_AC
                     || atype.getAmmoType() == AmmoType.T_LAC)
             && atype.getMunitionType() == AmmoType.M_ARMOR_PIERCING ) {
            toHit.addModifier( 1, "Armor-Piercing Ammo" );
        }
    
        // spotter movement, if applicable
        if (isIndirect) {
            // semiguided ammo negates this modifier, if TAG succeeded    
            if (atype != null && atype.getAmmoType() == AmmoType.T_LRM &&
                atype.getMunitionType() == AmmoType.M_SEMIGUIDED &&
                te.getTaggedBy() != -1) {
                toHit.addModifier(-1 , "semiguided ignores spotter movement & indirect fire penalties");
            } else {
                toHit.append(Compute.getSpotterMovementModifier(game, spotter.getId()));
            }
        }
    
        // attacker terrain
        toHit.append(Compute.getAttackerTerrainModifier(game, attackerId));
    
        // target terrain, not applicable when delivering minefields
        if (target.getTargetType() != Targetable.TYPE_MINEFIELD_DELIVER) {
            toHit.append(Compute.getTargetTerrainModifier(game, target, eistatus));
            toSubtract += Compute.getTargetTerrainModifier(game, target, eistatus).getValue();
        }
    
        // target in water?
        IHex targHex = game.getBoard().getHex(target.getPosition());
        if (targHex.containsTerrain(Terrains.WATER)
                && targHex.terrainLevel(Terrains.WATER) == 1
                && targEl == 0
                && te.height() > 0) { //target in partial water
            los.setTargetCover(los.getTargetCover() | LosEffects.COVER_HORIZONTAL);
            losMods = los.losModifiers(game, eistatus);
        }

        if(target instanceof Infantry && !wtype.hasFlag(WeaponType.F_FLAMER)) {
            if(targHex.containsTerrain(Terrains.FORTIFIED)
            || ((Infantry)target).getDugIn() == Infantry.DUG_IN_COMPLETE) {
                toHit.addModifier(2, "infantry dug in");
            }
        }
        
        // add in LOS mods that we've been keeping
        toHit.append(losMods);
        
        if (te != null && te.isHullDown()
            && (te instanceof Mech && los.getTargetCover() > LosEffects.COVER_NONE
                || te instanceof Tank && targHex.containsTerrain(Terrains.FORTIFIED) && te.sideTable(ae.getPosition()) == ToHitData.SIDE_FRONT)) {
            toHit.addModifier(2, "Hull down target");
        }
    
        // secondary targets modifier,
        // if this is not a iNarc Nemesis confused attack
        if (!isNemesisConfused) {
            toHit.append(Compute.getSecondaryTargetMod(game, ae, target, exchangeSwarmTarget));
        }        
    
        // heat
        if (ae.getHeatFiringModifier() != 0) {
            toHit.addModifier(ae.getHeatFiringModifier(), "heat");
        }
    
        // actuator & sensor damage to attacker
        toHit.append(Compute.getDamageWeaponMods(ae, weapon));
    
        // target immobile
        ToHitData immobileMod = Compute.getImmobileMod(target, aimingAt, aimingMode);
        if (immobileMod != null) {
            toHit.append(immobileMod);
            toSubtract += immobileMod.getValue();
        }

        // attacker prone
        toHit.append(Compute.getProneMods(game, ae, weaponId));

        
        // target prone
        ToHitData proneMod = null;
        if (te != null && te.isProne()) {
            // easier when point-blank
            if (distance <= 1) {
                // BMRr, pg. 72: Swarm Mek attacks get "an additional -4
                // if the BattleMech is prone or immoble."  I interpret
                // this to mean that the bonus gets applied *ONCE*.
                if ( Infantry.SWARM_MEK.equals( wtype.getInternalName() ) ) {
                    // If the target is immoble, don't give a bonus for prone.
                    if ( !te.isImmobile() ) {
                        proneMod = new ToHitData(-4, "swarm prone target");
                    }
                } else {
                    proneMod = new ToHitData(-2, "target prone and adjacent");
                }
            } else {
                // Harder at range.
                proneMod = new ToHitData(1, "target prone and at range");
            }
        }
        if (proneMod != null) {
            toHit.append(proneMod);
            toSubtract += proneMod.getValue();
        }
    
        // weapon to-hit modifier
        if (wtype.getToHitModifier() != 0) {
            toHit.addModifier(wtype.getToHitModifier(), "weapon to-hit modifier");
        }

        // ammo to-hit modifier
        if (te != null && te.getMovementMode() == IEntityMovementMode.VTOL
                && (atype != null
                && (atype.getAmmoType() == AmmoType.T_AC_LBX
                || atype.getAmmoType() == AmmoType.T_AC_LBX_THB)
                && atype.getMunitionType() == AmmoType.M_CLUSTER)
                                && te.getElevation() > 0
                && te.getElevation() > game.getBoard().getHex(te.getPosition()).terrainLevel(Terrains.BLDG_ELEV)
                && te.getElevation() != game.getBoard().getHex(te.getPosition()).terrainLevel(Terrains.BRIDGE_ELEV)) {

            toHit.addModifier(-3, "flak to-hit modifier");
        } else if (usesAmmo && atype.getToHitModifier() != 0) {
            toHit.addModifier(atype.getToHitModifier(), "ammunition to-hit modifier");
        }
        
        // add iNarc bonus
        if (isINarcGuided) {
            toHit.addModifier(-1, "iNarc homing pod");
        }
        
        if (isHaywireINarced) {
            toHit.addModifier(1, "iNarc Haywire pod");
        }
        
        //Heavy infantry have +1 penalty
        if(ae instanceof Infantry && ae.hasWorkingMisc(MiscType.F_TOOLS, MiscType.S_HEAVY_ARMOR)) {
            toHit.addModifier(1, "Heavy Armor");
        }
    
        // add targeting computer (except with LBX cluster ammo)
        if (aimingMode == IAimingModes.AIM_MODE_TARG_COMP &&
            aimingAt != Mech.LOC_NONE) {
            if(ae.hasActiveEiCockpit()) {
                if(ae.hasTargComp()) {
                    toHit.addModifier(2, "aiming with targeting computer & EI system");
                } else {
                    toHit.addModifier(6, "aiming with EI system");
                }
            } else {
                toHit.addModifier(3, "aiming with targeting computer");
            }
        } else {
            if (ae.hasTargComp() && wtype.hasFlag(WeaponType.F_DIRECT_FIRE) &&
               (!usesAmmo || 
                !((atype.getAmmoType() == AmmoType.T_AC_LBX
                || atype.getAmmoType() == AmmoType.T_AC_LBX_THB)
                && atype.getMunitionType() == AmmoType.M_CLUSTER))) {
                toHit.addModifier(-1, "targeting computer");
            }
        }
    
        // Change hit table for elevation differences inside building.
        if ( null != los.getThruBldg() && aElev != tElev ) {
    
            // Tanks get hit in a random side.
            if ( target instanceof Tank ) {
                toHit.setSideTable( ToHitData.SIDE_RANDOM );
            } else if ( target instanceof Mech ) {
                // Meks have special tables for shots from above and below.
                if ( aElev > tElev ) {
                    toHit.setHitTable( ToHitData.HIT_ABOVE );
                } else {
                    toHit.setHitTable( ToHitData.HIT_BELOW );
                }
            }
    
        }

        // Change hit table for partial cover, accomodate for partial underwater(legs)
        if (los.getTargetCover() != LosEffects.COVER_NONE) {
            if (ae.getLocationStatus(weapon.getLocation()) == ILocationExposureStatus.WET
                    && (targHex.containsTerrain(Terrains.WATER)
                    && targEl == 0
                    && te.height() > 0)) {
                //weapon underwater, target in partial water
                toHit.setHitTable(ToHitData.HIT_KICK);
            } else {
                if(game.getOptions().booleanOption("maxtech_partial_cover")) {
                    toHit.setHitTable(ToHitData.HIT_PARTIAL_COVER);
                    toHit.setCover(los.getTargetCover());
                } else {
                    toHit.setHitTable(ToHitData.HIT_PUNCH);
                    toHit.setCover(LosEffects.COVER_HORIZONTAL);
                }
            }
            // XXX what to do about GunEmplacements with partial cover?
        }
    
        // factor in target side
        if ( isAttackerInfantry && 0 == distance ) {
            // Infantry attacks from the same hex are resolved against the front.
            toHit.setSideTable( ToHitData.SIDE_FRONT );
        } else {
            toHit.setSideTable( Compute.targetSideTable(ae, target) );
        }
        
        
        //deal with grapples
        if(target instanceof Mech) {
            int grapple = ((Mech)target).getGrappled();
            if(grapple != Entity.NONE) {
                if(grapple == ae.getId())
                    toHit.addModifier(-4, "target grappled");
                else if(!exchangeSwarmTarget)
                    toHit.addModifier(1, "CQC, possible friendly fire");
                else {
                    //this -1 cancels the original +1
                    toHit.addModifier(-1, "friendly fire");
                    return toHit;
                }
            }
        }

        // remove old target movement and terrain mods,
        // add those for new target.
        if (exchangeSwarmTarget) {
            toHit.addModifier(-toSubtract, "original target mods");
            toHit.append(Compute.getImmobileMod(oldTarget, aimingAt, aimingMode));
            toHit.append(Compute.getTargetMovementModifier(game, oldTarget.getId()));
            toHit.append(Compute.getTargetTerrainModifier(game, game.getEntity(oldTarget.getId())));
            distance = Compute.effectiveDistance(game, ae, oldTarget);
            if (oldTarget != null && oldTarget.isProne()) {
                // easier when point-blank
                if (distance <= 1) {
                    proneMod = new ToHitData(-2, "target prone and adjacent");
                } else {
                    // Harder at range.
                    proneMod = new ToHitData(1, "target prone and at range");
                }
            }
            toHit.append(proneMod);
            if (!isECMAffected
                    && !oldTarget.isEnemyOf(ae)
                    && !(oldTarget.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_HEAD) > 0)
                    && atype.getMunitionType() == AmmoType.M_SWARM_I) {
                toHit.addModifier( +2, "Swarm-I at friendly unit with intact sensors");
            }
        }

        // okay!
        return toHit;
    }

    private static String toHitIsImpossible(IGame game, Entity ae, 
                              Targetable target, Mounted weapon, AmmoType atype,
                              WeaponType wtype, int ttype, 
                              boolean exchangeSwarmTarget, boolean usesAmmo,
                              Entity te, boolean isTAG, boolean isInferno, 
                              boolean isAttackerInfantry, boolean isIndirect, 
                              int attackerId, int weaponId, 
                              boolean isArtilleryIndirect, Mounted ammo,
                              boolean isArtilleryFLAK, boolean targetInBuilding,
                              boolean isArtilleryDirect) {
        boolean isHoming = false;
        ToHitData toHit = null;

        if ( ae.hasShield() && ae.hasActiveShield(weapon.getLocation(),weapon.isRearMounted()) ) {
            return "Weapon blocked by active shield";
        }
        // If it has a torso-mounted cockpit and two head sensor hits or three sensor hits...
        // It gets a =4 penalty for being blind!
        if (ae instanceof Mech && ((Mech)ae).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
            int sensorHits = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
            int sensorHits2 = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_CT);
            if ((sensorHits + sensorHits2) == 3) {
                return "Sensors Completely Destroyed for Torso-Mounted Cockpit";
            }
        }


        // missing, breached or jammed weapons can't fire
        if (!weapon.canFire() && !exchangeSwarmTarget) {
            return "Weapon is not in a state where it can be fired";
        }

        // If we're lying mines, we can't shoot.
        if (ae.isLayingMines()) {
            return "Can't fire weapons when laying mines";
        }

        // make sure weapon can deliver minefield
        if (target.getTargetType() == Targetable.TYPE_MINEFIELD_DELIVER &&
            !AmmoType.canDeliverMinefield(atype)) {
            return "Weapon can't deliver minefields";
        }
        if (target.getTargetType() == Targetable.TYPE_FLARE_DELIVER
                && !(usesAmmo
                && atype.getAmmoType() == AmmoType.T_LRM
                && atype.getMunitionType() == AmmoType.M_FLARE)) {
            return "Weapon can't deliver flares";
        }
        if (game.getPhase() == IGame.PHASE_TARGETING && !isArtilleryIndirect) {
            return "Only indirect artillery can be fired in the targeting phase";
        }
        if (game.getPhase() == IGame.PHASE_OFFBOARD && !isTAG) {
            return "Only TAG can be fired in the offboard attack phase";
        }
        if (game.getPhase() != IGame.PHASE_OFFBOARD && isTAG) {
            return "TAG can only be fired in the offboard attack phase";
        }
    
    
        if ( atype != null &&
             atype.getAmmoType() == AmmoType.T_LRM &&
             ( atype.getMunitionType() == AmmoType.M_THUNDER ||
               atype.getMunitionType() == AmmoType.M_THUNDER_ACTIVE ||
               atype.getMunitionType() == AmmoType.M_THUNDER_INFERNO ||
               atype.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB||
               atype.getMunitionType() == AmmoType.M_THUNDER_AUGMENTED ) &&
             target.getTargetType() != Targetable.TYPE_MINEFIELD_DELIVER) {
            return "Weapon can only deliver minefields";
        }
        if ( atype != null &&
             atype.getAmmoType() == AmmoType.T_LRM &&
             atype.getMunitionType() == AmmoType.M_FLARE &&
             target.getTargetType() != Targetable.TYPE_FLARE_DELIVER) {
            return "Weapon can only deliver flares";
        }
        // some weapons can only target infantry
        if ( wtype.hasFlag(WeaponType.F_INFANTRY_ONLY) ) {
            if (te != null && !(te instanceof Infantry) ||
                target.getTargetType() != Targetable.TYPE_ENTITY) {
                return "Weapon can only be used against infantry";
            }
        }    
        // make sure weapon can clear minefield
        if (target instanceof MinefieldTarget &&
            !AmmoType.canClearMinefield(atype)) {
            return "Weapon can't clear minefields";
        }
        
        // Arty shots have to be with arty, non arty shots with non arty.
        if (wtype.hasFlag(WeaponType.F_ARTILLERY)) {
            //check artillery is targetted appropriately for its ammo
            long munition = AmmoType.M_STANDARD;
            if (atype != null) {
                munition = atype.getMunitionType();
            }
            if (munition == AmmoType.M_HOMING) {
                //target type checked later because its different for direct/indirect (BMRr p77 on board arrow IV)
                isHoming = true;
            } 
            else if (ttype != Targetable.TYPE_HEX_ARTILLERY && !isArtilleryFLAK) {
                    return "Weapon must make artillery attacks.";
            }
        } else {
            //weapon is not artillery
            if (ttype == Targetable.TYPE_HEX_ARTILLERY) {
                return "Weapon can't make artillery attacks.";
            }
        }
        
        // can't target yourself, unless those are swarm missiles that
        // continued to a new target
        if (ae.equals(te) && !exchangeSwarmTarget) {
            return "You can't target yourself";
        }
    
        // weapon operational?
        if (weapon.isDestroyed() || weapon.isBreached()) {
            return "Weapon not operational.";
        }
    
        // got ammo?
        // don't check if it's a swarm-missile-follow-on-attack, we used the ammo previously
        if ( usesAmmo && !exchangeSwarmTarget && (ammo == null || ammo.getShotsLeft() == 0 || ammo.isBreached()) ) {
            return "Weapon out of ammo.";
        }
    
        // Are we dumping that ammo?
        if ( usesAmmo && ammo.isDumping() ) {
            ae.loadWeaponWithSameAmmo( weapon );
            if ( ammo.getShotsLeft() == 0 || ammo.isDumping() ) {
                return "Dumping remaining ammo.";
            }
        }
    
        // is the attacker even active?
        if (ae.isShutDown() || !ae.getCrew().isActive()) {
            return "Attacker is in no condition to fire weapons.";
        }
    
        // sensors operational?
        int sensorHits = ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
        if (ae instanceof Mech
                && ((Mech)ae).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
            sensorHits += ae.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_CT);
            if (sensorHits > 2)
                return "Attacker sensors destroyed.";
        } else if (sensorHits > 1) {
            return "Attacker sensors destroyed.";
        }
    
        // Is the weapon blocked by a passenger?
        if ( ae.isWeaponBlockedAt(weapon.getLocation(), weapon.isRearMounted()) ) {
            return "Weapon blocked by passenger.";
        }
    
        // Can't target an entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return "Target is swarming a Mek.";
        }
        
        //"Cool" mode for vehicle flamer requires coolant system
        boolean vf_cool = false;
        if(atype != null 
                && wtype.hasFlag(WeaponType.F_FLAMER)
                && weapon.curMode().equals("Cool")) {
            vf_cool = true;
            if(!ae.hasWorkingMisc(MiscType.F_COOLANT_SYSTEM,-1)) {
                return "Vehicle does not have a working coolant system";
            }
        }

        if(Targetable.TYPE_HEX_EXTINGUISH == target.getTargetType()) {
            if(!wtype.hasFlag(WeaponType.F_EXTINGUISHER)
                    && !vf_cool) {
                return "Weapon can't put out fires";
            }
            IHex hexTarget = game.getBoard().getHex(target.getPosition()); 
            if(!hexTarget.containsTerrain(Terrains.FIRE)) {
                return "Target is not on fire.";
            }
        }
        else if(wtype.hasFlag(WeaponType.F_EXTINGUISHER)) {
            if(!((target instanceof Tank && ((Tank)target).isOnFire())
                || (target instanceof Entity && ((Entity)target).infernos.getTurnsLeftToBurn() > 0))) {
                return "Target is not on fire.";
            }
        }
    
        // can't target non-wood hexes for clearing (except thin ice)
        if (Targetable.TYPE_HEX_CLEAR == target.getTargetType()) {
            IHex hexTarget = game.getBoard().getHex(target.getPosition()); 
            if(!hexTarget.containsTerrain(Terrains.WOODS) &&
               !hexTarget.containsTerrain(Terrains.JUNGLE) &&
               !(hexTarget.containsTerrain(Terrains.ICE) && hexTarget.containsTerrain(Terrains.WATER))) {
                return "Target terrain cannot be cleared.";
            }
            // Infantry can't clear woods.
            if ( isAttackerInfantry ) {
                return "Infantry can not clear terrain.";
            }
        }
        // Infantry can't clear woods.
        if ( isAttackerInfantry &&
             Targetable.TYPE_HEX_CLEAR == target.getTargetType() ) {
            return "Infantry can not clear woods.";
        }
    
        // Some weapons can't cause fires, but Infernos always can.
        if ( (vf_cool || wtype.hasFlag(WeaponType.F_NO_FIRES) && !isInferno)
            && Targetable.TYPE_HEX_IGNITE == target.getTargetType() ) {
            return "Weapon can not cause fires.";
        }

        // only woods and buildings can be set intentionally on fire
        if (target.getTargetType() == Targetable.TYPE_HEX_IGNITE
                && game.getOptions().booleanOption("no_ignite_clear")
                && !(game.getBoard().getHex(((HexTarget) target).getPosition()).containsTerrain(Terrains.WOODS)
                    || game.getBoard().getHex(((HexTarget) target).getPosition()).containsTerrain(Terrains.JUNGLE)
                    || game.getBoard().getHex(((HexTarget) target).getPosition()).containsTerrain(Terrains.FUEL_TANK)
                    || game.getBoard().getHex(((HexTarget) target).getPosition()).containsTerrain(Terrains.BUILDING))) {
            return "Only woods and building hexes can be set on fire intentionally.";
        }
    
        // Can't target infantry with Inferno rounds (BMRr, pg. 141).
        //  Also, enforce options for keeping vehicles and protos safe
        //  if those options are checked.
        if ( isInferno &&
             (te instanceof Infantry
              || te instanceof Tank && game.getOptions().booleanOption("vehicles_safe_from_infernos")
              || te instanceof Protomech && game.getOptions().booleanOption("protos_safe_from_infernos"))
              ) {
            return "Can not target that unit type with Inferno rounds.";
        }
    
        // The TAG system cannot target infantry.
        if( isTAG && te instanceof Infantry ) {
            return "Can not target infantry with TAG.";
        }

        // Can't raise the heat of infantry or tanks.
        if ( wtype.hasFlag(WeaponType.F_FLAMER) &&
             wtype.hasModes() &&
             weapon.curMode().equals("Heat") &&
             !(te instanceof Mech) ) {
            return "Can only raise the heat level of Meks.";
        }
    
        // Handle solo attack weapons.
        if ( wtype.hasFlag(WeaponType.F_SOLO_ATTACK) ) {
            for ( Enumeration i = game.getActions();
                  i.hasMoreElements(); ) {
                Object o = i.nextElement();
                if (!(o instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction)o;
                if (prevAttack.getEntityId() == attackerId) {
    
                    // If the attacker fires another weapon, this attack fails.
                    if ( weaponId != prevAttack.getWeaponId() ) {
                        return "Other weapon attacks declared.";
                    }
                }
            }
        } else if(isAttackerInfantry && !(ae instanceof BattleArmor)) {
            // check for trying to fire field gun after moving
            if(!wtype.hasFlag(WeaponType.F_INFANTRY) && ae.moved != IEntityMovementType.MOVE_NONE) {
                    return "Can't fire field guns in same turn as moving";
            }
            //check for mixing infantry and field gun attacks
            for ( Enumeration i = game.getActions();
                  i.hasMoreElements(); ) {
                Object o = i.nextElement();
                if (!(o instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction)o;
                if (prevAttack.getEntityId() == attackerId) {
                    Mounted prevWeapon = ae.getEquipment(prevAttack.getWeaponId());
                    if( prevWeapon.getType().hasFlag(WeaponType.F_INFANTRY) != wtype.hasFlag(WeaponType.F_INFANTRY) ) {
                        return "Can't fire field guns and small arms at the same time.";
                    }
                }
            }
        }

        // check if indirect fire is valid
        if (isIndirect
                && !game.getOptions().booleanOption("indirect_fire")) {
            return "Indirect fire option not enabled";
        }
        if (isIndirect && game.getOptions().booleanOption("indirect_fire") &&
            !game.getOptions().booleanOption("indirect_always_possible") &&
            LosEffects.calculateLos(game, attackerId, target).canSee()) {
            return "Indirect fire impossible with direct LOS";
        }
        
        // hull down vees can't fire front weapons
        if(ae instanceof Tank && ae.isHullDown() && weapon.getLocation() == Tank.LOC_FRONT) {
            return "Nearby terrain blocks front weapons.";
        }
    
        // Weapon in arc?
        if (!Compute.isInArc(game, attackerId, weaponId, target)) {
            return "Target not in arc.";
        }
        
        // BA Micro bombs only when flying
        if (atype != null && atype.getAmmoType() == AmmoType.T_BA_MICRO_BOMB) {
            if (ae.getElevation() == 0) {
                return "attacker must be at least at elevation 1";
            } else if (target.getTargetType() != Targetable.TYPE_HEX_BOMB) {
                return "must target hex with bombs";
            }
        }
        if (target.getTargetType() == Targetable.TYPE_HEX_BOMB &&
            !(usesAmmo &&
              atype.getAmmoType() == AmmoType.T_BA_MICRO_BOMB)) {
            return "Weapon can't deliver bombs";
        }
        Entity spotter = null;
        if (isIndirect) {
            spotter = Compute.findSpotter(game, ae, target);
            if (spotter == null) {
                return "No available spotter";
            }
        }
        
        int eistatus=0;

        // check LOS (indirect LOS is from the spotter)
        LosEffects los;
        ToHitData losMods;
        if (!isIndirect) {
            los = LosEffects.calculateLos(game, attackerId, target);

            if(ae.hasActiveEiCockpit()) {
                if(los.getLightWoods() > 0)
                    eistatus = 2;
                else
                    eistatus = 1;
            }

            losMods = los.losModifiers(game, eistatus);
        } else {
            los = LosEffects.calculateLos(game, spotter.getId(), target);
            // do not count attacker partial cover in indirect fire
            los.setAttackerCover(LosEffects.COVER_NONE);

            if(spotter.hasActiveEiCockpit()) {
                if(los.getLightWoods() > 0)
                    eistatus = 2;
                else
                    eistatus = 1;
            }

            losMods = los.losModifiers(game);
        }
    
        // if LOS is blocked, block the shot
        if (losMods.getValue() == ToHitData.IMPOSSIBLE && !isArtilleryIndirect) {
            return losMods.getDesc();
        }
    
        // Must target infantry in buildings from the inside.
        if ( targetInBuilding &&
             te instanceof Infantry &&
             null == los.getThruBldg() ) {
            return "Attack on infantry crosses building exterior wall.";
        }
        
        // attacker partial cover means no leg weapons
        if (los.isAttackerCover() && ae.locationIsLeg(weapon.getLocation()) &&
                ae.getLocationStatus(weapon.getLocation()) != ILocationExposureStatus.WET) {
            return "Nearby terrain blocks leg weapons.";
        }

        // Leg attacks, Swarm attacks, and
        // Mine Launchers don't use gunnery.
        if ( Infantry.LEG_ATTACK.equals( wtype.getInternalName() ) ) {
            toHit = Compute.getLegAttackBaseToHit( ae, te );
    
            // Return if the attack is impossible.
            if ( ToHitData.IMPOSSIBLE == toHit.getValue() ) {
                return toHit.getDesc();
            }
        }
        else if ( Infantry.SWARM_MEK.equals( wtype.getInternalName() ) ) {
            toHit = Compute.getSwarmMekBaseToHit( ae, te );
    
            // Return if the attack is impossible.
            if ( ToHitData.IMPOSSIBLE == toHit.getValue() ) {
                return toHit.getDesc();
            }
        }
        else if ( Infantry.STOP_SWARM.equals( wtype.getInternalName() ) ) {
            // Can't stop if we're not swarming, otherwise automatic.
            if ( Entity.NONE == ae.getSwarmTargetId() ) {
                return "Not swarming a Mek.";
            }
        }
        else if ( BattleArmor.MINE_LAUNCHER.equals(wtype.getInternalName()) ) {
            // Mine launchers can not hit infantry.
            if ( te instanceof Infantry ) {
                return "Can not attack infantry.";
            }
        }
        // Swarming infantry always hit their target, but
        // they can only target the Mek they're swarming.
        else if ( te != null && ae.getSwarmTargetId() == te.getId() ) {
            // Only certain weapons can be used in a swarm attack.
            if ( wtype.getDamage() == 0 ) {
                return "Weapon causes no damage.";
            }
        }
        else if ( Entity.NONE != ae.getSwarmTargetId() ) {
            return "Must target the Mek being swarmed.";
        }
        
        int distance = Compute.effectiveDistance(game, ae, target);

        if(atype != null && atype.getAmmoType() == AmmoType.T_LRM && atype.getMunitionType() == AmmoType.M_SEMIGUIDED) {
            if (te == null || te.getTaggedBy() == -1) {
                // from a rules clarification
                return "Semi-guided LRMs must target a unit tagged this turn";
            }
        }
        
        // Handle direct artillery attacks.
        if (isArtilleryDirect) {
            if (distance >17) {
                return "Direct artillery attack at range >17 hexes.";
            }
            if (isHoming) {
                if (te == null || te.getTaggedBy() == -1) {
                    // see BMRr p77 on board arrow IV
                    return "On board homing shot must target a unit tagged this turn";
                }
            }
        }
        if (isArtilleryIndirect) {
            int boardRange=(int)Math.ceil(distance / 17f);
            if (boardRange>wtype.getLongRange()) {
                return "Indirect artillery attack out of range";
            }
            if (distance<=17  && !(losMods.getValue()==ToHitData.IMPOSSIBLE)) {
                return "Cannot fire indirectly at range <=17 hexes unless no LOS.";
            }
            if (isHoming) {
                if(ttype != Targetable.TYPE_HEX_ARTILLERY) {
                    return "Off board homing shot must target a map sheet";
                }
            }
        }
        
        if(ae instanceof Mech) {
            int grapple = ((Mech)ae).getGrappled();
            if(grapple != Entity.NONE) {
                if(grapple != target.getTargetId()) {
                    return "Can only attack grappled mech";
                }
                int loc = weapon.getLocation();
                if((loc != Mech.LOC_CT
                        && loc != Mech.LOC_LT
                        && loc != Mech.LOC_RT
                        && loc != Mech.LOC_HEAD)
                        || weapon.isRearMounted()) {
                    return "Can only fire head and front torso weapons when grappled";
                }
            }
        }

        return null;
    }

    /**
     * @return Returns the nemesisConfused.
     */
    public boolean isNemesisConfused() {
        return nemesisConfused;
    }
    /**
     * @param nemesisConfused The nemesisConfused to set.
     */
    public void setNemesisConfused(boolean nemesisConfused) {
        this.nemesisConfused = nemesisConfused;
    }
    
    public boolean isSwarmingMissiles() {
        return swarmingMissiles;
    }
    
    public void setSwarmingMissiles(boolean swarmingMissiles) {
        this.swarmingMissiles = swarmingMissiles;
    }
    
    public void setOldTargetId(int id) {
        oldTargetId = id;
    }
}
