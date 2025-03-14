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

package megamek.client.bot.common.formation;

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
    private final Entity leader;
    private final FormationType formationType;
    private int formationSpeed;
    private Coords formationCenter;

    public enum FormationType {
        BOX(3, 4, 5),
        LINE(2, 3, 4),
        WEDGE(2, 3, 4),
        COLUMN(2, 3, 5),
        SCATTERED(3, 5, 10);

        private final int minDistance;
        private final int idealDistance;
        private final int maxDistance;

        FormationType(int minDistance, int idealDistance, int maxDistance) {
            this.minDistance = minDistance;
            this.idealDistance = idealDistance;
            this.maxDistance = maxDistance;
        }

        public int getMinDistance() {
            return minDistance;
        }

        public int getIdealDistance() {
            return idealDistance;
        }

        public int getMaxDistance() {
            return maxDistance;
        }
    }

    public Formation(String id, Set<Entity> members, UnitRole primaryRole, Entity leader,
                     FormationType formationType) {
        this.id = id;
        this.members = members;
        this.primaryRole = primaryRole;
        this.leader = leader;
        this.formationType = formationType;
        this.formationSpeed = members.stream().map(Entity::getRunMP).min(Integer::compareTo).orElse(0);
    }

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

    public int getFormationSpeed() {
        return formationSpeed;
    }

    public void calculateFormationCenter() {
        formationCenter =  Coords.average(getMembers().stream().map(Entity::getPosition).toList());
    }
}
