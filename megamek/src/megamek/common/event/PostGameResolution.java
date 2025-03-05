/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MegaMek.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.event;

import megamek.common.Entity;

import java.util.Enumeration;

public interface PostGameResolution {
    Enumeration<Entity> getEntities();

    Entity getEntity(int id);

    Enumeration<Entity> getGraveyardEntities();

    Enumeration<Entity> getWreckedEntities();

    Enumeration<Entity> getRetreatedEntities();

    Enumeration<Entity> getDevastatedEntities();
}
