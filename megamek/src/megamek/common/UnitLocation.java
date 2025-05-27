/*
 * MegaMek -
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package megamek.common;

import java.io.Serial;
import java.io.Serializable;

/**
 * A simple class to specify a location and facing for a unit.
 * @since July 5, 2005
 */
public record UnitLocation(int entityId, Coords coords, int facing, int elevation, int boardId) implements Serializable {
    @Serial
    private static final long serialVersionUID = 3989732522854387850L;


}
