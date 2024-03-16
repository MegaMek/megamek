/*
 * MegaMek - Copyright (C) 2002-2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common;

import megamek.common.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a set of handles on an OmniMech used by Battle Armor units
 * equipped with Boarding Claws to attach themselves for transport. This is
 * standard equipment on OmniMechs.
 *
 * @see MechFileParser#postLoadInit
 */
class BattleArmorHandles implements Transporter {
    private static final long serialVersionUID = -7149931565043762975L;

    /** The troopers being carried. */
    protected int carriedUnit = Entity.NONE;
    transient Game game;

    private static final String NO_VACANCY_STRING = "A squad is loaded";
    private static final String HAVE_VACANCY_STRING = "One battle armor squad";

    @Override
    public boolean canLoad(Entity unit) {
        return (carriedUnit == Entity.NONE) && (unit instanceof BattleArmor) && ((BattleArmor) unit).canDoMechanizedBA();
    }

    @Override
    public final void load(Entity unit) throws IllegalArgumentException {
        // If we can't load the unit, throw an exception.
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Can not load " + unit.getShortName() + " onto this OmniMech.");
        }

        // Assign the unit as our carried troopers.
        carriedUnit = unit.getId();
    }

    @Override
    public final List<Entity> getLoadedUnits() {
        List<Entity> units = new ArrayList<>(1);
        Entity entity = game.getEntity(carriedUnit);
        if (entity != null) {
            units.add(entity);
        }
        return units;
    }

    @Override
    public final boolean unload(Entity unit) {
        // Are we carrying the unit?
        Entity trooper = game.getEntity(carriedUnit);
        if ((trooper == null) || !trooper.equals(unit)) {
            // Nope.
            return false;
        }

        // Remove the troopers.
        carriedUnit = Entity.NONE;
        return true;
    }

    @Override
    public String getUnusedString() {
        return (carriedUnit != Entity.NONE) ? NO_VACANCY_STRING : HAVE_VACANCY_STRING;
    }

    @Override
    public double getUnused() {
        return (carriedUnit == Entity.NONE) ? 1 : 0;
    }

    @Override
    public void resetTransporter() {
        carriedUnit = Entity.NONE;
    }
    
    @Override
    public boolean isWeaponBlockedAt(int loc, boolean isRear) {
        Entity carriedBA = game.getEntity(carriedUnit);
        if (carriedBA == null) {
            return false;
        } else {
            int tloc = BattleArmor.LOC_SQUAD;
            switch (loc) {
                case Mech.LOC_CT:
                    tloc = isRear ? BattleArmor.LOC_TROOPER_5 : BattleArmor.LOC_TROOPER_6;
                    break;
                case Mech.LOC_LT:
                    tloc = isRear ? BattleArmor.LOC_TROOPER_4 : BattleArmor.LOC_TROOPER_2;
                    break;
                case Mech.LOC_RT:
                    tloc = isRear ? BattleArmor.LOC_TROOPER_3 : BattleArmor.LOC_TROOPER_1;
                    break;
            }
            return (carriedBA.locations() > tloc) && (carriedBA.getInternal(tloc) > 0);
        }
    }

    @Override
    public final @Nullable Entity getExteriorUnitAt(int loc, boolean isRear) {
        return isWeaponBlockedAt(loc, isRear) ? game.getEntity(carriedUnit) : null;
    }

    @Override
    public final List<Entity> getExternalUnits() {
        return getLoadedUnits();
    }

    @Override
    public int getCargoMpReduction(Entity carrier) {
        return 0;
    }
    
    @Override
    public String toString() {
        return "BattleArmorHandles - troopers:" + carriedUnit;
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
    }
}