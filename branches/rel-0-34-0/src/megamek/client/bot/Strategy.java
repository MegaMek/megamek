/*
 * MegaMek -
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot;

/**
 * Container for strategy modifiers
 *
 * TODO: alot...
 */
public class Strategy {

    /*
     * 0 full out retreat (damage shy) +inf full out attack (damage preference)
     */
    public double attack = 1;

    /*
     * Modify attacks against me based upon this. i.e. how strongly do people
     * want to attack me
     */
    public double target = 1;

    static CEntity MainTarget;
}