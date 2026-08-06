package megamek.common.rules;
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

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.units.Entity;

public abstract class RulesPhysical {

    /**
     * Do shields boost punch damage.
     *
     * @param entity the entity attacking
     * @param armLoc the arm location with the shield
     * @return the damage boost from the shield
     */
    public abstract int getShieldDamageBoost(Entity entity, int armLoc);

    /**
     * Return the claw to-hit modifier.
     *
     * @return the claw to-hit modifier
     */
    public abstract int getClawToHitModifier();

    /**
     * Should the shield reset with phase change.
     *
     * @return true if the shield resets with phase change
     */
    public abstract boolean phaseChangeShield();

    /**
     * What is the to-hit modifier for attacking when there is a shield on the arm.
     *
     * @param toHit the to-hit data to modify
     * @param attacker the attacking entity
     * @param weapon the mounted weapon being used
     */
    public abstract void getShieldToHitModifier(ToHitData toHit, Entity attacker, Mounted<?> weapon);

    /**
     * Can retractable blades be used during punch attacks.
     *
     * @param toRetractableBlade true if checking for retractable blade use
     * @return true if retractable blades can be used
     */
    public abstract boolean retractableBladeArmCheck(boolean toRetractableBlade);

    /**
     * Does a retractable blade break when used during the punch attack.
     *
     * @return true if the retractable blade breaks
     */
    public abstract boolean checkRetractableBladeBroke();

    /**
     * Does a missed mace attack cause a PSR.
     *
     * @return true if a missed mace causes a piloting skill roll
     */
    public abstract boolean getMaceMissedPSR();

    /**
     * What is the target number for a lance to do internal damage.
     *
     * @return the target number
     */
    public abstract int getLanceTarget();

    /**
     * Does the lance do anything special on a charge?
     *
     * @return true if the lance has special charge effects
     */
    public abstract boolean isLanceCharging();

    /**
     * Does a shield do anything in a charge?
     *
     * @param attackingEntity the entity performing the charge
     * @return the hit data from shield charge damage, or null if no shield damage
     */
    @Nullable
    public abstract HitData shieldChargeDamage(Entity attackingEntity);

    /**
     * Do the spikes break?
     *
     * @param entity the entity with spikes
     * @param loc the location being checked
     * @return a report of whether spikes broke
     */
    public abstract Report checkBreakSpikes(Entity entity, int loc);

    /**
     * Can talons increase DFA damage.
     *
     * @param entity the entity to check
     * @return true if the entity has talons
     */
    public abstract boolean hasTalons(Entity entity);

    /**
     * What is the kick modifier.
     *
     * @return the kick modifier
     */
    public abstract int getKickModifier();

    /**
     * Do we have a modifier for punching.
     *
     * @return the punch modifier
     */
    public abstract int getPunchModifier();

    /**
     * What is the damage of the charge.
     *
     * @param entity the attacking entity
     * @param target the target entity
     * @param tacOps true if using tactical operations rules
     * @param mos the margin of success
     * @param hexesMoved the number of hexes moved in the charge
     * @return the charge damage
     */
    public abstract int getChargeDamage(Entity entity, Entity target, boolean tacOps, int mos, int hexesMoved);

    /**
     * How much damage does the charge attacker take.
     *
     * @param entity the attacking entity
     * @param effectiveTargetWeight the effective weight of the target
     * @param tacOps true if using tactical operations rules
     * @param distance the distance traveled in the charge
     * @return the damage taken by the attacker
     */
    public abstract int getChargeDamageTakenBy(Entity entity, double effectiveTargetWeight, boolean tacOps,
          int distance);

    /**
     * Missed charges, where does the attacker end up?
     *
     * @param game the game instance
     * @param entityId the ID of the charging entity
     * @param src the source coordinates
     * @param direction the direction of the charge
     * @return the final coordinates after a missed charge
     */
    public abstract Coords getMissedChargeDisplacement(Game game, int entityId, Coords src, int direction);

    /**
     * Can you club a prone target?
     *
     * @param targetElevation the elevation of the target
     * @param attackerElevation the elevation of the attacker
     * @return true if the target cannot be clubbed while prone
     */
    public abstract boolean cannotClubProne(int targetElevation, int attackerElevation);

    /**
     * For Charge/DFA, get the pilot difference modifier.
     *
     * @param attackerPiloting the piloting skill of the attacking pilot
     * @param targetPiloting the piloting skill of the defending pilot
     * @param immobile true if one of the entities is immobile
     * @return the pilot difference modifier
     */
    public abstract int getPilotDiffModifier(int attackerPiloting, int targetPiloting, boolean immobile);

    /**
     * Can a charge be cancelled.
     *
     * @return true if a charge can be cancelled
     */
    public abstract boolean canChargeCancel();

    /**
     * Get the right table for falls from above.
     *
     * @param affaTarget the entity that is the target of a fall from above
     * @return the hit data for fall from above
     */
    public abstract HitData getFallFromAboveTable(Entity affaTarget);

    /**
     * What kind of rubble can you find a club in?
     * @return the level of the rubble needed
     */
    public abstract int getClubFindInRubble();

    /**
     * Check if the quad can kick
     * @param leg what leg is kicking
     * @param entity the entity that is kicking
     * @return true if the kick is impossible
     */
    public abstract boolean quadMuleKickImpossible(int leg, Entity entity);
}
