/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */

package megamek.client.bot.common;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.UnitRole;

import java.util.Set;

/**
 * Represents a formation of units moving and fighting together.
 * @author Luana Coppio
 */
public class Formation {
    private final String id;
    private final Set<Entity> members;
    private final UnitRole primaryRole;
    private Entity leader;
    private FormationType formationType;
    private Coords formationCenter;

    public enum FormationType {
        BOX,
        LINE,
        WEDGE,
        COLUMN,
        SCATTERED
    }

    public Formation(String id, Set<Entity> members, UnitRole primaryRole, Entity leader,
                     FormationType formationType) {
        this.id = id;
        this.members = members;
        this.primaryRole = primaryRole;
        this.leader = leader;
        this.formationType = formationType;
    }

    // Getters and utility methods
    public String getId() {
        return id;
    }

    public Set<Entity> getMembers() {
        return members;
    }

    public UnitRole getPrimaryRole() {
        return primaryRole;
    }

    public Entity getLeader() {
        return leader;
    }

    public FormationType getFormationType() {
        return formationType;
    }

    public Coords getFormationCenter() {
        return formationCenter;
    }

    public void calculateFormationCenter() {
        formationCenter =  Coords.average(getMembers().stream().map(Entity::getPosition).toList());
    }
}
