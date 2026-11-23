package megamek.common.rules.core;
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

import megamek.common.CriticalSlot;
import megamek.common.HitData;
import megamek.common.game.Game;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rules.RulesUnits;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;

public class CoreRulesUnits extends RulesUnits {
    /**
     * {@inheritDoc}
     * Mule kicks have no additional modifier Core p.238
     */
    @Override
    public int getMuleKickModifier() { return 0; }

    /**
     * {@inheritDoc}
     * Is it immobile due to leg destruction? Core p.237 (tripod), p.239 (quad), p.90
     */
    @Override
    public boolean getDoesLegDestructionCauseImmobile(Mek mek) {
        int legsDestroyed = 0;
        for (int i = 0; i < mek.locations(); i++) {
            if (mek.locationIsLeg(i)) {
                if (mek.isLocationBad(i)) {
                    legsDestroyed++;
                }
            }
        }
        if (legsDestroyed >= 2 && !(mek instanceof QuadMek)) {
            return true;
        } else if (legsDestroyed == 4 && (mek instanceof QuadMek)) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * reduce a quad's walk MP for legs destroyed, hip hits, and actuator hits. Core p.90, 238
     */
    @Override
    public int reduceQuadWalkMP(int mp, int legsDestroyed, int hipHits, int actuatorHits,
          boolean bTOLegDamage) {
        if (legsDestroyed > 0) {
            if (legsDestroyed == 1) {
                mp--;
            } else if (legsDestroyed == 2) {
                mp = mp - 2;
            } else if (legsDestroyed == 3) {
                mp = 1;
            } else if (legsDestroyed == 4) {
                mp = 0;
            }
        }
        // A quad's first hip hit has no standard effects; each further one counts as a biped hip hit (Core p.238)
        mp -= Math.max(0, hipHits - 1);
        mp -= actuatorHits;

        if (mp > 0) {
            return mp;
        }
        if (legsDestroyed < 4 && mp <= 0) {
            return 1;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     * Quads modify PSR rolls for legs as per Core p.238
     */
    @Override
    public void quadPilotModForLegsDestroyed(int destroyedLegs, PilotingRollData roll) {
        switch (destroyedLegs) {
            case 1:
                roll.addModifier(1, "1 leg destroyed");
                break;
            case 2:
                roll.addModifier(2, "2 legs destroyed");
                break;
            case 3:
                roll.addModifier(Game.rulesManager.getRulesPSR().getLegDestroyedModifier(), "3 legs destroyed");
                break;
        }
    }

    /**
     * {@inheritDoc}
     * Reduce MP for a mek with hip hits. Core p.99
     */
    @Override
    public int getMekMPReduction(int hipHits, boolean bTOLegDamage, int mp) {
        mp -= hipHits;
        return mp;
    }

    /**
     * {@inheritDoc}
     * MP cannot be reduced below 1 by actuator damage. only by leg destruction Core p.99
     */
    @Override
    public int getMinimumMP(int mp) {
        return 1;
    }

    /**
     * {@inheritDoc}
     * A destroyed leg, in biped terms for a quad; a quad's two destroyed legs also count, as they apply the effects of a
     * hip hit (Core p.238)
     */
    @Override
    public boolean hasBadLegs(Entity entity) {
        if (entity instanceof Mek mek) {
            return hasBipedEquivalentDestroyedLeg(mek) || ((mek instanceof QuadMek) && (mek.countBadLegs() == 2));
        }
        return entity.hasBadLegs();
    }

    /**
     * @param mek the Mek
     *
     * @return the hip hits that have the effects of a biped's hip hit (Core p.99). A quad ignores its first hip hit and
     *       treats two destroyed legs as a hip hit (Core p.238). Hips in destroyed legs do not count.
     */
    static int bipedEquivalentHipHits(Mek mek) {
        int hipHits = hipHits(mek);
        if (mek instanceof QuadMek) {
            return Math.max(0, hipHits - 1) + ((mek.countBadLegs() == 2) ? 1 : 0);
        }
        return hipHits;
    }

    /**
     * @param mek the Mek
     *
     * @return the hip hits in legs that are not destroyed
     */
    static int hipHits(Mek mek) {
        int hipHits = 0;
        for (int loc = 0; loc < mek.locations(); loc++) {
            if (mek.legHasHipCrit(loc)) {
                hipHits++;
            }
        }
        return hipHits;
    }

    /**
     * @param mek the Mek
     *
     * @return the legs forcing a PSR when the Mek jumps, one per leg that is destroyed or has an actuator hit (Core
     *       pp.90, 99, 110). A quad ignores its first hip hit, in location order as hit order is not recorded, and
     *       counts destroyed legs in biped terms (Core p.238).
     */
    static int legsForcingLandingChecks(Mek mek) {
        boolean quad = mek instanceof QuadMek;
        boolean hipIgnored = !quad;
        int legs = quad ? ((hasBipedEquivalentDestroyedLeg(mek) ? 1 : 0) + ((mek.countBadLegs() == 2) ? 1 : 0)) : 0;
        for (int loc = 0; loc < mek.locations(); loc++) {
            if (!mek.locationIsLeg(loc)) {
                continue;
            }
            if (mek.isLocationBad(loc)) {
                legs += quad ? 0 : 1;
                continue;
            }
            boolean hip = mek.legHasHipCrit(loc);
            if (hip && !hipIgnored) {
                hipIgnored = true;
                hip = false;
            }
            if (hip || (mek.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_UPPER_LEG, loc) > 0)
                  || (mek.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_LOWER_LEG, loc) > 0)
                  || (mek.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_FOOT, loc) > 0)) {
                legs++;
            }
        }
        return legs;
    }

    /**
     * @param mek the Mek
     *
     * @return true if the Mek has a destroyed leg in biped terms (Core p.90); a quad needs three destroyed legs
     *       (Core p.238)
     */
    static boolean hasBipedEquivalentDestroyedLeg(Mek mek) {
        return mek.countBadLegs() >= ((mek instanceof QuadMek) ? 3 : 1);
    }

    /**
     * @param mek the Mek
     *
     * @return true if hip damage or a destroyed leg keeps the Mek to one level per hex moving forward and no backward
     *       level change (Core pp.47, 90, 238)
     */
    static boolean hasHipOrLegLevelRestriction(Mek mek) {
        return (bipedEquivalentHipHits(mek) > 0) || hasBipedEquivalentDestroyedLeg(mek);
    }

    /**
     * @param mek the Mek
     *
     * @return the legs forcing a running PSR: each leg that is destroyed or has a hip hit, in biped terms for a quad
     *       (Core pp.45, 110-111, 238)
     */
    static int legsForcingRunningChecks(Mek mek) {
        if (mek instanceof QuadMek) {
            return (hasBipedEquivalentDestroyedLeg(mek) ? 1 : 0) + bipedEquivalentHipHits(mek);
        }
        int legs = 0;
        for (int loc = 0; loc < mek.locations(); loc++) {
            if (mek.locationIsLeg(loc) && (mek.isLocationBad(loc) || mek.legHasHipCrit(loc))) {
                legs++;
            }
        }
        return legs;
    }

    /**
     * {@inheritDoc}
     * Core allows Torso twisting in physical if you haven't already.
     * Returns true.
     */
    @Override
    public boolean getPhysicalTwistEnabled() {
        return true;
    }

    /** Core p.205 shares the last point only with exposed composite structure in the next location. */
    @Override
    public boolean transfersCompositePip(Mek mek, HitData hit, boolean ammoExplosion) {
        if (ammoExplosion && (mek.locationHasCase(hit.getLocation()) || mek.hasCASEII(hit.getLocation()))) {
            return false;
        }
        HitData next = mek.getTransferLocation(hit);
        int loc = next.getLocation();
        return loc >= 0 && mek.getInternal(next) > 0 && mek.hasCompositeStructure(loc)
              && (ammoExplosion || mek.getArmor(next) <= 0);
    }
}
