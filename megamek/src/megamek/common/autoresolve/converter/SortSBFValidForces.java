/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.autoresolve.converter;

/**
 * BalancedConsolidateForces is a helper class that redistribute entities and forces
 * in a way to consolidate then into valid forces to build Formations out of them.
 * @author Luana Coppio
 */
public class SortSBFValidForces extends ForceConsolidation {

    public static final int MAX_ENTITIES_IN_SUB_FORCE = 6;
    public static final int MAX_ENTITIES_IN_TOP_LEVEL_FORCE = 20;

    @Override
    protected int getMaxEntitiesInSubForce() {
        return MAX_ENTITIES_IN_SUB_FORCE;
    }

    @Override
    protected int getMaxEntitiesInTopLevelForce() {
        return MAX_ENTITIES_IN_TOP_LEVEL_FORCE;
    }

}

