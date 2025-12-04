/*
 * Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.handlers;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.weapons.Weapon;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Handler for rapid-fire autocannons. When using special ammunition, this handler spawns
 * auto-hit attacks using the appropriate special ammo handler (ACAPHandler, ACFlakHandler, etc.)
 * to ensure special ammo effects are properly applied to each hit.
 *
 * @author Andrew Hunter
 */
public class RapidFireACWeaponHandler extends UltraWeaponHandler {
    @Serial
    private static final long serialVersionUID = -1770392652874842106L;

    /** Marker added to auto-hit ToHitData to indicate spawned rapid-fire attacks. */
    public static final String RAPID_FIRE_SPAWN_MARKER = "rapid-fire hit";

    public RapidFireACWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
    }

    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (doAmmoFeedProblemCheck(vPhaseReport)) {
            return true;
        }

        int jamLevel = 4;
        boolean kindRapidFire = game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_KIND_RAPID_AC);
        if (kindRapidFire) {
            jamLevel = 2;
        }
        if ((roll.getIntValue() <= jamLevel) && (howManyShots == 2) && !attackingEntity.isConventionalInfantry()) {
            if (roll.getIntValue() > 2 || kindRapidFire) {
                Report r = new Report(3161);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
                weapon.setJammed(true);
            } else {
                Report r = new Report(3162);
                r.subject = subjectId;
                r.choose(false);
                r.indent();
                vPhaseReport.addElement(r);

                explodeRoundInBarrel(vPhaseReport);
            }
            return false;
        }
        return false;
    }

    @Override
    protected boolean usesClusterTable() {
        return true;
    }

    /**
     * Checks if the current ammo type has special effects that require a specialized handler.
     * Note: M_INCENDIARY_AC is intentionally excluded as it was retconned (FMFS pg 158).
     *
     * @return true if using special ammo (AP, flak, flechette, tracer, caseless)
     */
    private boolean hasSpecialAmmo() {
        if (ammoType == null) {
            return false;
        }
        return ammoType.getMunitionType().contains(AmmoType.Munitions.M_ARMOR_PIERCING)
              || ammoType.getMunitionType().contains(AmmoType.Munitions.M_ARMOR_PIERCING_PLAYTEST)
              || ammoType.getMunitionType().contains(AmmoType.Munitions.M_FLAK)
              || ammoType.getMunitionType().contains(AmmoType.Munitions.M_FLECHETTE)
              || ammoType.getMunitionType().contains(AmmoType.Munitions.M_TRACER)
              || ammoType.getMunitionType().contains(AmmoType.Munitions.M_CASELESS);
    }

    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // Get the number of shots that hit using parent's cluster table logic
        int shotsHit = super.calcHits(vPhaseReport);

        // If no special ammo, use standard rapid-fire damage handling
        if (!hasSpecialAmmo()) {
            return shotsHit;
        }

        // For special ammo, spawn auto-hit attacks to apply ammo-specific effects
        if (shotsHit > 0) {
            // For infantry, all shots hit as a single "lump" so shotsHit=1,
            // but we need to spawn attacks for all shots that actually fired
            int attacksToSpawn = target.isConventionalInfantry() ? howManyShots : shotsHit;

            // Report number of shots hitting (UltraWeaponHandler skips this for infantry)
            if (target.isConventionalInfantry() && (howManyShots > 1)) {
                Report r = new Report(3325);
                r.subject = subjectId;
                r.add(howManyShots);
                r.add(sSalvoType);
                r.add(toHit.getTableDesc());
                vPhaseReport.addElement(r);
            }

            spawnAutoHitAttacks(attacksToSpawn, vPhaseReport);
        }

        // Return shotsHit but mark as missed to skip redundant damage processing.
        // This prevents "Attack deals zero damage" message since spawned attacks
        // already handled all damage reporting.
        bMissed = true;
        return shotsHit;
    }

    /**
     * Spawns auto-hit attacks for each shot that hit. Each spawned attack uses the appropriate special ammo handler
     * (ACAPHandler, ACFlakHandler, etc.) to apply munition-specific effects.
     *
     * @param shotsHit     number of shots that hit
     * @param vPhaseReport report vector for game messages
     */
    private void spawnAutoHitAttacks(int shotsHit, Vector<Report> vPhaseReport) {
        ToHitData autoHit = new ToHitData();
        autoHit.addModifier(TargetRoll.AUTOMATIC_SUCCESS, RAPID_FIRE_SPAWN_MARKER);

        for (int i = 0; i < shotsHit; i++) {
            // Create a new attack action for this shot
            WeaponAttackAction shotWaa = new WeaponAttackAction(
                  weaponAttackAction.getEntityId(),
                  weaponAttackAction.getTargetType(),
                  weaponAttackAction.getTargetId(),
                  weaponAttackAction.getWeaponId());
            shotWaa.setAmmoId(weaponAttackAction.getAmmoId());
            shotWaa.setAmmoMunitionType(weaponAttackAction.getAmmoMunitionType());

            // Get the correct handler for this ammo type
            // ACWeapon.getCorrectHandler() will route to ACAPHandler, ACFlakHandler, etc.
            // because we marked this as an auto-hit (skips rapid-fire routing)
            Weapon weaponImpl = (Weapon) weapon.getType();
            AttackHandler shotHandler = weaponImpl.getCorrectHandler(autoHit, shotWaa, game, gameManager);

            if (shotHandler instanceof WeaponHandler wHandler) {
                // Don't re-announce firing (already announced by parent)
                wHandler.setAnnouncedEntityFiring(false);
                // Mark as spawned from rapid-fire parent
                wHandler.setParentBayHandler(this);
            }

            // Handle this shot - applies special ammo effects
            if (shotHandler != null) {
                shotHandler.handle(game.getPhase(), vPhaseReport);
            }
        }
    }
}
