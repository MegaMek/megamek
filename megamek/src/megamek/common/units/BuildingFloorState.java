/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import java.io.Serializable;
import java.util.Arrays;

/** Optional per-floor CF/armor and top-down collapse, TO:AR pp. 119–121. Floor identities survive displacement. */
public class BuildingFloorState implements Serializable {
    private final int[] cf;
    private final int[] phaseCF;
    private final int[] startTurnCF;
    private final int[] armor;
    private final int[] levels;
    private boolean changed;

    public BuildingFloorState(int height, int constructionFactor, int armorPoints) {
        cf = new int[height];
        phaseCF = new int[height];
        startTurnCF = new int[height];
        armor = new int[height];
        levels = new int[height];
        Arrays.fill(cf, constructionFactor);
        Arrays.fill(phaseCF, constructionFactor);
        Arrays.fill(startTurnCF, constructionFactor);
        Arrays.fill(armor, armorPoints);
        for (int floor = 0; floor < height; floor++) {
            levels[floor] = floor;
        }
    }

    /** Independent snapshot for board/client synchronization, including displaced floor identities. */
    public BuildingFloorState(BuildingFloorState source) {
        cf = source.cf.clone();
        phaseCF = source.phaseCF.clone();
        startTurnCF = source.startTurnCF.clone();
        armor = source.armor.clone();
        levels = source.levels.clone();
        changed = source.changed;
    }

    public int size() {
        return cf.length;
    }

    public int getCF(int floor) {
        return floor < 0 || floor >= size() ? 0 : cf[floor];
    }

    public int getPhaseCF(int floor) {
        return floor < 0 || floor >= size() ? 0 : phaseCF[floor];
    }

    public int getArmor(int floor) {
        return floor < 0 || floor >= size() ? 0 : armor[floor];
    }

    public int getCriticalThreshold(int floor) {
        return floor < 0 || floor >= size() ? 0 : (startTurnCF[floor] + 9) / 10;
    }

    public int getLevel(int floor) {
        return floor < 0 || floor >= size() ? -1 : levels[floor];
    }

    public int floorAtLevel(int level) {
        if (level < 0) {
            return -1;
        }
        for (int floor = 0; floor < size(); floor++) {
            if (levels[floor] == level) {
                return floor;
            }
        }
        return -1;
    }

    public int height() {
        return Arrays.stream(levels).max().orElse(-1) + 1;
    }

    public void setCF(int floor, int value) {
        if (floor >= 0 && floor < size()) {
            cf[floor] = Math.max(0, value);
            changed = true;
        }
    }

    public void setArmor(int floor, int value) {
        if (floor >= 0 && floor < size()) {
            armor[floor] = Math.max(0, value);
            changed = true;
        }
    }

    public int maximumCF() {
        return Arrays.stream(cf).max().orElse(0);
    }

    public void setPhaseCF(int floor, int value) {
        if (floor >= 0 && floor < size()) {
            phaseCF[floor] = Math.max(0, value);
        }
    }

    public boolean hasChanges() {
        return changed;
    }

    public void newPhase() {
        java.lang.System.arraycopy(cf, 0, phaseCF, 0, size());
        changed = false;
    }

    public void newRound() {
        java.lang.System.arraycopy(cf, 0, startTurnCF, 0, size());
    }

    /** Snapshot of physical levels, useful for moving equipment and occupants after a collapse. */
    public int[] levels() {
        return levels.clone();
    }

    /**
     * Resolve destroyed levels, transferring excess collapse damage upward and downward independently. Surviving
     * upper levels settle onto the surviving lower structure; destroying its lowest level destroys the entire hex.
     */
    public void resolveCollapse() {
        for (int floor = 0; floor < size(); floor++) {
            if (levels[floor] < 0 || cf[floor] > 0) {
                continue;
            }
            if (levels[floor] == 0) {
                Arrays.fill(cf, 0);
                Arrays.fill(armor, 0);
                Arrays.fill(levels, -1);
                changed = true;
                return;
            }
            levels[floor] = -1;
            changed = true;
            int lower = previousStanding(floor);
            int upper = nextStanding(floor);
            while (lower >= 0 && upper < size()) {
                int fallingCF = 0;
                for (int i = upper; i < size(); i++) {
                    fallingCF += cf[i];
                }
                int damage = fallingCF / 3;
                if (damage == 0) {
                    break;
                }
                damageStack(upper, 1, damage);
                damageStack(lower, -1, damage);
                if (cf[lower] > 0) {
                    break;
                }
                lower = previousStanding(lower);
                upper = nextStanding(upper - 1);
            }
            if (lower < 0) {
                Arrays.fill(cf, 0);
            }
        }
        compactLevels();
    }

    private int previousStanding(int floor) {
        while (--floor >= 0 && cf[floor] == 0) {
            // Find the next surviving lower floor.
        }
        return floor;
    }

    private int nextStanding(int floor) {
        while (++floor < size() && cf[floor] == 0) {
            // Find the next surviving upper floor.
        }
        return floor;
    }

    private void damageStack(int floor, int direction, int damage) {
        for (int i = floor; i >= 0 && i < size() && damage > 0; i += direction) {
            int absorbed = Math.min(cf[i], damage);
            setCF(i, cf[i] - absorbed);
            if (cf[i] == 0) {
                // This floor is already part of the current collapse; do not collapse it again below.
                levels[i] = -1;
            }
            damage -= absorbed;
        }
    }

    private void compactLevels() {
        int level = 0;
        for (int floor = 0; floor < size(); floor++) {
            int newLevel = cf[floor] > 0 ? level++ : -1;
            changed |= newLevel != levels[floor];
            levels[floor] = newLevel;
            if (newLevel < 0) {
                armor[floor] = 0;
            }
        }
    }
}
