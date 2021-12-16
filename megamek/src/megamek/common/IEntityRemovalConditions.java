/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
 * This interface represents Entity Removal Conditions
 */
public interface IEntityRemovalConditions {
    int REMOVE_UNKNOWN = 0x0000;
    int REMOVE_IN_RETREAT = 0x0100;
    int REMOVE_PUSHED = 0x0110;
    int REMOVE_CAPTURED = 0x0120;
    int REMOVE_SALVAGEABLE = 0x0200;
    int REMOVE_EJECTED = 0x0210;
    int REMOVE_DEVASTATED = 0x0400;
    int REMOVE_NEVER_JOINED = 0x0800;
}
