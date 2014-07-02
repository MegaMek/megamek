/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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
package megamek.server;

import java.util.Vector;

import megamek.common.Report;

public abstract class DynamicTerrainProcessor {
    protected Server server;

    DynamicTerrainProcessor(Server server) {
        this.server = server;
    }

    /**
     * Process terrain changes in the end phase
     * 
     * @param vPhaseReport reports for the server to send out
     */
    abstract void doEndPhaseChanges(Vector<Report> vPhaseReport);
}
