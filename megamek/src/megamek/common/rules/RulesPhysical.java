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

    // Do shields boost punch damage
    public abstract int getShieldDamageBoost(Entity entity, int armLoc);

    // Return the claw to-hit modifier
    public abstract int getClawToHitModifier();

    // Should the shield reset with phase change
    public abstract boolean phaseChangeShield();

    // What is the to-hit modifier for attacking when there is a shield on the arm
    public abstract void getShieldToHitModifier(ToHitData toHit, Entity attacker, Mounted<?> weapon);

    // Can retractable blades be used during punch attacks
    public abstract boolean retractableBladeArmCheck(boolean toRetractableBlake);

    // Does a retractable blade break when used during the punch attack
    public abstract boolean checkRetractableBladeBroke();

    // Does a missed mace attack cause a PSR
    public abstract boolean getMaceMissedPSR();

    // What is the target number for a lance to do internal damage
    public abstract int getLanceTarget();

    // Does the lance do anything special on a charge?
    public abstract boolean isLanceCharging();

    // Does a shield do anything in a charge?
    @Nullable
    public abstract HitData shieldChargeDamage(Entity attackingEntity);

    // Do the spikes break?
    public abstract Report checkBreakSpikes(Entity entity, int loc);

    // Can talons increase DFA damage
    public abstract boolean hasTalons(Entity entity);

    // What is the kick modifier
    public abstract int getKickModifier();

    // Do we have a modifier for punching
    public abstract int getPunchModifier();

    // What is the damage of the charge
    public abstract int getChargeDamage(Entity entity, Entity target, boolean tacOps, int mos, int hexesMoved);

    // How much damage does the charge attacker take
    public abstract int getChargeDamageTakenBy(Entity entity, double effectiveTargetWeight, boolean tacOps,
          int distance);

    // Missed charges, where does the attacker end up?
    public abstract Coords getMissedChargeDisplacement(Game game, int entityId, Coords src, int direction);

    // Can you club a prone target?
    public abstract boolean cannotClubProne(int targetElevation, int attackerElevation);

    // For Charge/DFA, get the pilot difference modifier
    public abstract int getPilotDiffModifier(int piloting, int piloting1, boolean immobile);

    // Can a charge be cancelled
    public abstract boolean canChargeCancel();

    // Get the right table for falls from above
    public abstract HitData getFallFromAboveTable(Entity affaTarget);
}
