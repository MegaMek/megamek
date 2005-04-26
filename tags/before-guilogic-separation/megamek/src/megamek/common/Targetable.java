/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

public interface Targetable
{
    public static final int TYPE_ENTITY               = 0;
    public static final int TYPE_HEX_CLEAR            = 1;
    public static final int TYPE_HEX_IGNITE           = 2;
    public static final int TYPE_BUILDING             = 3;
    public static final int TYPE_BLDG_IGNITE          = 4;
    public static final int TYPE_MINEFIELD_CLEAR      = 5;
    public static final int TYPE_MINEFIELD_DELIVER    = 6;
    public static final int TYPE_HEX_ARTILLERY        = 7;
    public static final int TYPE_HEX_FASCAM           = 8;
    public static final int TYPE_HEX_INFERNO_IV       = 9;
    public static final int TYPE_HEX_VIBRABOMB_IV     = 10;
    public static final int TYPE_INARC_POD            = 11;
    public static final int TYPE_SEARCHLIGHT          = 12;

    public int getTargetType();
    public int getTargetId();
    public Coords getPosition();
    public int absHeight();
    public int getHeight();
    public int getElevation();
    public boolean isImmobile();
    public String getDisplayName();
}
