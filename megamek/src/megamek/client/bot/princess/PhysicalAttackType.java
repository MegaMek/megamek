/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.bot.princess;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 12/18/13 1:29 PM
 */
public enum PhysicalAttackType {
    LEFT_KICK, RIGHT_KICK, LEFT_PUNCH, RIGHT_PUNCH, CHARGE, DEATH_FROM_ABOVE, SHOVE, WEAPON, FRENZY;

    public boolean isPunch() {
        return LEFT_PUNCH.equals(this) || RIGHT_PUNCH.equals(this);
    }

    public boolean isKick() {
        return LEFT_KICK.equals(this) || RIGHT_KICK.equals(this);
    }
}
