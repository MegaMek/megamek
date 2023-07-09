/*
 * Copyright (c) 2000-2002 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2022-2023 - The MegaMek Team. All Rights Reserved.
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
package megamek.common;

/**
 * Represents the space on an standard Mech (i.e. one that is not an OmniMech)
 * used by Battle Armor squads equipped with Magnetic Clamps to attach themselves
 * for transport. This transporter gets assigned to all of a player's standard
 * Mechs in the Exchange Phase if any Battle Armor squad equipped with a
 * Magnetic Clamp is on that player's side.
 */
public class ClampMountMech extends BattleArmorHandles {
    private static final long serialVersionUID = -5687854937528642266L;

    private static final String NO_VACANCY_STRING = "A BA squad with magnetic clamps is loaded";
    private static final String HAVE_VACANCY_STRING = "One BA-magclamp squad";

    @Override
    public String getUnusedString() {
        return (carriedUnit != Entity.NONE) ? NO_VACANCY_STRING : HAVE_VACANCY_STRING;
    }

    @Override
    public int getCargoMpReduction(Entity carrier) {
        return getLoadedUnits().size();
    }

    @Override
    public boolean canLoad(Entity unit) {
        return (carriedUnit == Entity.NONE) && (unit instanceof BattleArmor) && ((BattleArmor) unit).hasMagneticClamps();
    }

    @Override
    public String toString() {
        return "ClampMountMech - troopers:" + carriedUnit;
    }
}