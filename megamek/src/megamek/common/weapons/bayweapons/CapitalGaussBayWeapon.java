/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.bayweapons;

import megamek.common.EquipmentTypeLookup;

/**
 * @author Jay Lawson
 * @since Sep 25, 2004
 */
public class CapitalGaussBayWeapon extends AmmoBayWeapon {
    private static final long serialVersionUID = 8756042527483383101L;

    public CapitalGaussBayWeapon() {
        super();
        // tech levels are a little tricky
        this.name = "Capital Gauss Bay";
        this.setInternalName(EquipmentTypeLookup.CAPITAL_GAUSS_BAY);
        this.heat = 0;
        this.damage = DAMAGE_VARIABLE;
        this.shortRange = 12;
        this.mediumRange = 24;
        this.longRange = 40;
        this.extremeRange = 50;
        this.tonnage = 0.0;
        this.bv = 0;
        this.cost = 0;
        this.atClass = CLASS_CAPITAL_GAUSS;
        this.capital = true;
    }
    
    @Override
    public int getBattleForceClass() {
        return BFCLASS_CAPITAL;
    }
}
