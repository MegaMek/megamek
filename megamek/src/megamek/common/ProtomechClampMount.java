/*
 * Copyright (C) 2018, 2023 - The MegaMek Team
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
package megamek.common;

/**
 * Represents a section of a Mech torso where a protomech equipped with the magnetic clamp system
 * can attach itself for transport. A mech has two of these, one front and one rear. An ultraheavy
 * protomech can only be carried on the front mount, and if carried this way the rear cannot be
 * used. The two mounts are not aware of each other and it is the responsibility of the code that
 * handles loading to enforce this limitation.
 * 
 * @author Neoancient
 */
public class ProtomechClampMount extends BattleArmorHandles {
    private static final long serialVersionUID = 3937766099677646981L;

    private final boolean rear;

    private static final String NO_VACANCY_STRING = "A protomech is loaded";
    private static final String HAVE_VACANCY_STRING = "One protomech";

    public ProtomechClampMount(boolean rear) {
        this.rear = rear;
    }
    
    public boolean isRear() {
        return rear;
    }

    @Override
    public String getUnusedString() {
        return (carriedUnit != Entity.NONE) ? NO_VACANCY_STRING : HAVE_VACANCY_STRING;
    }

    @Override
    public boolean canLoad(Entity unit) {
        return (carriedUnit == Entity.NONE) && unit.isProtoMek() && unit.hasWorkingMisc(MiscType.F_MAGNETIC_CLAMP)
                && (!rear || unit.getWeightClass() < EntityWeightClass.WEIGHT_SUPER_HEAVY);
    }

    @Override
    public boolean isWeaponBlockedAt(int loc, boolean isRear) {
        return (rear == isRear) && (loc == Mech.LOC_CT) && (carriedUnit != Entity.NONE);
    }

    @Override
    public int getCargoMpReduction(Entity carrier) {
        double protoWeight = 0.0;
        if (carriedUnit != Entity.NONE) {
            protoWeight = game.getEntity(carriedUnit).getWeight();
            if (carrier.isOmni()) {
                protoWeight = Math.max(0, protoWeight - 3.0);
            }
        }            
        if (protoWeight < carrier.getWeight() * 0.1) {
            return 0;
        } else if (protoWeight < carrier.getWeight() * 0.25) {
            return Math.min(3, carrier.getOriginalWalkMP() / 2);
        } else {
            return carrier.getOriginalWalkMP() / 2;
        }
    }

    @Override
    public String toString() {
        return "Protomech clamp mount:" + carriedUnit;
    }
}