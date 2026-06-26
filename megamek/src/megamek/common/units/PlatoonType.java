/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
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
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.units;

/**
 * The amount of space taken up by an infantry unit in a transport bay differs from the space in an infantry compartment
 * (used in APCs) due to quarters, equipment storage, and maintenance equipment. A single cubicle holds a platoon,
 * except in the case of mechanized which requires a cubicle per squad.
 */

public enum PlatoonType {
    FOOT(5, 28, 25),
    JUMP(6, 21, 20),
    MOTORIZED(7, 28, 25),
    MECHANIZED(8, 7, 5);

    private final int weight;
    private final int isPersonnel;
    private final int clanPersonnel;

    PlatoonType(int weight, int isPersonnel, int clanPersonnel) {
        this.weight = weight;
        this.isPersonnel = isPersonnel;
        this.clanPersonnel = clanPersonnel;
    }

    public int getWeight() {
        return weight;
    }

    public int getISPersonnel() {
        return isPersonnel;
    }

    public int getClanPersonnel() {
        return clanPersonnel;
    }

    @Override
    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }

    public static PlatoonType getPlatoonType(Entity en) {
        return switch (en.getMovementMode()) {
            case TRACKED, WHEELED, HOVER, VTOL, SUBMARINE -> MECHANIZED;
            case INF_MOTORIZED -> MOTORIZED;
            case INF_JUMP -> JUMP;
            default -> FOOT;
        };
    }
}
