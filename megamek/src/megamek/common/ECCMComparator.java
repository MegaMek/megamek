/*
 * Copyright (C) 2015 Nicholas Walczak (walczak@cs.umn.edu)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common;

import java.util.Comparator;

/**
 * Compares two ECMInfo to determine which should take precedence, assuming that the goal is to find the strongest ECCM
 * field.
 */
public class ECCMComparator implements Comparator<ECMInfo> {
    @Override
    public int compare(ECMInfo o1, ECMInfo o2) {
        // Compare two ECCMs
        if (o1.isECCM() && o2.isECCM()) {
            if (o2.angelECCMStrength > o1.angelECCMStrength) {
                return -1;
            } else if (o2.angelECCMStrength < o1.angelECCMStrength) {
                return 1;
            } else { // Angel strengths are equal
                if (o2.eccmStrength > o1.eccmStrength) {
                    return -1;
                } else if (o2.eccmStrength < o1.eccmStrength) {
                    return 1;
                }
            }
            // Both angel and regular ECCM strength are equal
            return 0;
            // Compare ECCM to ECM
        } else if (o1.isECCM() && !o2.isECCM()) {
            if (o2.angelStrength > o1.angelECCMStrength) {
                return -1;
            } else if (o2.angelStrength < o1.angelECCMStrength) {
                return 1;
            } else { // Angel strengths are equal
                if (o2.strength > o1.eccmStrength) {
                    return -1;
                } else if (o2.strength < o1.eccmStrength) {
                    return 1;
                }
            }
            // Both angel and regular ECCM strength are equal
            return 1;
            // Compare ECM to ECCM
        } else if (!o1.isECCM() && o2.isECCM()) {
            if (o2.angelECCMStrength > o1.angelStrength) {
                return -1;
            } else if (o2.angelECCMStrength < o1.angelStrength) {
                return 1;
            } else { // Angel strengths are equal
                if (o2.eccmStrength > o1.strength) {
                    return -1;
                } else if (o2.eccmStrength < o1.strength) {
                    return 1;
                }
            }
            // Both angel and regular ECCM strength are equal
            return -1;
        } else { // Compare two ECMs
            if (o2.angelStrength > o1.angelStrength) {
                return -1;
            } else if (o2.angelStrength < o1.angelStrength) {
                return 1;
            } else { // Angel strengths are equal
                if (o2.strength > o1.strength) {
                    return -1;
                } else if (o2.strength < o1.strength) {
                    return 1;
                }
            }
            // Both angel and regular strength are equal
            return 0;
        }
    }

}
