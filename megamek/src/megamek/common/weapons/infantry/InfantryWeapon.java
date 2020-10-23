/*
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons.infantry;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.Weapon;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public abstract class InfantryWeapon extends Weapon {
    /**
     *
     */
    private static final long serialVersionUID = -4437093890717853422L;

    protected double infantryDamage;
    protected int infantryRange;
    protected int crew;
    protected double ammoWeight;
    protected int ammoCost;
    protected int shots;
    protected int bursts;

    public InfantryWeapon() {
        super();
        damage = DAMAGE_VARIABLE;
        flags = flags.or(F_INFANTRY);
        ammoType = AmmoType.T_NA;
        shortRange = 0;
        mediumRange = 0;
        longRange = 0;
        extremeRange = 0;
        heat = 0;
        tonnage = 0.0;
        criticals = 0;
        tankslots = 0;
        svslots = 1;
        infantryDamage = 0;
        crew = 1;
        ammoWeight = 0.0;
        ammoCost = 0;
        shots = 0;
        bursts = 0;
        infantryRange = 0;
        infDamageClass = WEAPON_NA;
    }

    public double getInfantryDamage() {
        return infantryDamage;
    }

    public int getInfantryRange() {
        return infantryRange;
    }

    @Override
    public double getShortAV() {
        return infantryDamage;
    }

    @Override
    public double getMedAV() {
        if (infantryRange * 3 > AIRBORNE_WEAPON_RANGES[RANGE_SHORT]) {
            return infantryDamage;
        } else {
            return 0.0;
        }
    }

    @Override
    public double getLongAV() {
        if (infantryRange * 3 > AIRBORNE_WEAPON_RANGES[RANGE_MED]) {
            return infantryDamage;
        } else {
            return 0.0;
        }
    }

    @Override
    public double getExtAV() {
        if (infantryRange * 3 > AIRBORNE_WEAPON_RANGES[RANGE_LONG]) {
            return infantryDamage;
        } else {
            return 0.0;
        }
    }

    @Override
    public int getRoundShortAV() {
        return (int) Math.round(getShortAV());
    }

    @Override
    public int getRoundMedAV() {
        return (int) Math.round(getMedAV());
    }

    @Override
    public int getRoundLongAV() {
        return (int) Math.round(getLongAV());
    }

    @Override
    public int getRoundExtAV() {
        return (int) Math.round(getExtAV());
    }

    @Override
    public int getMaxRange(Mounted weapon) {
        for (int range = RangeType.RANGE_EXTREME; range >= RangeType.RANGE_SHORT; range--) {
            if (infantryRange * 3 > AIRBORNE_WEAPON_RANGES[range - 1]) {
                return range;
            }
        }
        return RangeType.RANGE_SHORT;
    }

    public int getCrew() {
        return crew;
    }
    
    /**
     * The long range of this weapon type. Infantry weapons calculate ranges based on the "infantry range" value rather than
     * explicit short/long/medium ranges
     */
    @Override
    public int getLongRange() {
        if (longRange == 0) {
            return infantryRange * 3;
        }
        return longRange;
    }

    /**
     * The extreme range of this weapon type. Infantry weapons calculate ranges based on the "infantry range" value rather than
     * explicit short/long/medium ranges
     */
    @Override
    public int getExtremeRange() {
        if (extremeRange == 0) {
            return infantryRange * 4;
        }
        return extremeRange;
    }

    /**
     * @return The weight of an ammo clip in tons
     */
    public double getAmmoWeight() {
        return ammoWeight;
    }

    /**
     * @return The number of shots in an ammo clip
     */
    public int getShots() {
        return shots;
    }

    /**
     * @return The number of bursts in an ammo clip.
     */
    public int getBursts() {
        return bursts;
    }

    /**
     * @return The cost of an ammo clip in C-bills
     */
    public int getAmmoCost() {
        return ammoCost;
    }

    /**
     * Check for whether small support vehicles have an option of standard or
     * inferno munitions for this weapon. Returns true for both the inferno and
     * the standard variant.
     *
     * @return Whether the weapon has alternate inferno ammo
     */
    public boolean hasInfernoAmmo() {
        return internalName.endsWith("Inferno")
                || (EquipmentType.get(internalName + "Inferno") != null);
    }

    /**
     * For weapons that can use inferno ammo, returns the inferno version. If there is
     * no inferno version or this is the inferno version, returns {@code this}.
     *
     * @return The inferno ammo variant of this weapon
     */
    public InfantryWeapon getInfernoVariant() {
        if (internalName.endsWith("Inferno")) {
            return this;
        } else {
            EquipmentType inferno = EquipmentType.get(internalName + "Inferno");
            if (inferno == null) {
                return this;
            }
            return (InfantryWeapon) inferno;
        }
    }

    /**
     * For weapons that can use inferno ammo, returns the standard ammo version. If there is
     * no standard version or this is the standard version, returns {@code this}.
     *
     * @return The standard ammo variant of this weapon
     */
    public InfantryWeapon getNonInfernoVariant() {
        if (internalName.endsWith("Inferno")) {
            EquipmentType standard = EquipmentType.get(internalName.replace("Inferno", ""));
            if (standard != null) {
                return (InfantryWeapon) standard;
            }
        }
        return this;
    }

    @Override
    public int getSupportVeeSlots(Entity entity) {
        return 1;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.IGame)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, IGame game, Server server) {
        Mounted m = game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId());
        if (((null != m) && (m.curMode().equals(Weapon.MODE_FLAMER_HEAT)
                || (waa.getEntity(game).isSupportVehicle()
                    && (((AmmoType) m.getLinked().getType()).getMunitionType() == AmmoType.M_INFERNO))))) {
            return new InfantryHeatWeaponHandler(toHit, waa, game, server);
        }
        return new InfantryWeaponHandler(toHit, waa, game, server);
    }

}
