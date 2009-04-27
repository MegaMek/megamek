/*
 * MegaMek - Copyright (C) 2007-2008 Ben Mazur (bmazur@sev.org)
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
package megamek.server.victory;

import java.io.Serializable;

/**
 * 
 */
public class SpaghettiVictoryFactory implements VictoryFactory, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 3401309900972098765L;

    /**
     * This is a really nasty implementation
     */
    public Victory createVictory(String victory) {
        return new SpaghettiVictory();
    }
}