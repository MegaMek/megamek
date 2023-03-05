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

import java.io.Serializable;

public class TagInfo implements Serializable {
    private static final long serialVersionUID = -8428068101269842100L;
    public int attackerId; // who fired the TAG
    public int targetType; // keeps track of the target's type
    public Targetable target; // entity the tag was fired at
    public boolean missed; // did the TAG hit?

    public TagInfo(int attackerId, int targetType, Targetable target, boolean missed) {
        this.attackerId = attackerId;
        this.targetType = targetType;
        this.target = target;
        this.missed = missed;
    }
}
