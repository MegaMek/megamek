/*
 * Copyright (c) 2003-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import megamek.logging.MMLogger;

import java.io.Serial;
import java.util.*;

/**
 * Represents a volume of space set aside for carrying Conventional Infantry and BattleArmor and their equipment under battle conditions.
 * TM p.239; see e.g. JI2A1 Attack APC
 */
public final class InfantryCompartment implements Transporter {
    @Serial
    private static final long serialVersionUID = 7837499891552862932L;
    private static final MMLogger LOGGER = MMLogger.create(InfantryCompartment.class);

    /** The carried troops, mapping unit ID to carried weight of the unit. */
    private final Map<Integer, Double> carriedTroops = new HashMap<>();
    final double totalSpace;
    private double currentSpace;

    private transient Game game;

    /**
     * Creates an InfantryCompartment for the given tonnage of troops.
     *
     * @param tonnage The weight of troops (in tons) this space can carry
     */
    public InfantryCompartment(double tonnage) {
        totalSpace = tonnage;
        currentSpace = tonnage;
    }

    @Override
    public boolean canLoad(Entity unit) {
        return unit.isInfantry() && (currentSpace >= unit.getWeight());
    }

    @Override
    public void load(Entity unit) throws IllegalArgumentException {
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Can not load " + unit + " into this troop space.");
        }

        currentSpace -= unit.getWeight();
        carriedTroops.put(unit.getId(), unit.getWeight());
    }

    @Override
    public List<Entity> getLoadedUnits() {
        return carriedTroops.keySet().stream().map(game::getEntity).toList();
    }

    @Override
    public boolean unload(Entity unit) {
        if (unit == null) {
            LOGGER.error("Trying to unload a null unit!");
            return false;
        } else if (carriedTroops.containsKey(unit.getId())) {
            currentSpace += carriedTroops.get(unit.getId());
            carriedTroops.remove(unit.getId());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getUnusedString() {
        return "Infantry Compartment - " + currentSpace + " tons";
    }

    @Override
    public double getUnused() {
        return currentSpace;
    }

    @Override
    public Entity getExteriorUnitAt(int loc, boolean isRear) {
        return null;
    }

    @Override
    public List<Entity> getExternalUnits() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        // Beware - this is used for saving to blk files - which is a bad idea
        return "troopspace:" + totalSpace;
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public void resetTransporter() {
        carriedTroops.clear();
        currentSpace = totalSpace;
    }
}
