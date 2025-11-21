/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.handlers;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.server.totalWarfare.TWGameManager;

/**
 * WeaponHandler for the Centurion Weapon System weapon, which is found in Jihad Conspiracies Interstellar Players 2, pg
 * 127.
 *
 * @author arlith Created on Sept 5, 2005
 */
public class CenturionWeaponSystemHandler extends EnergyWeaponHandler {
    @Serial
    private static final long serialVersionUID = -5226841653686213141L;

    /**
     *
     */
    public CenturionWeaponSystemHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.EnergyWeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        return 0;
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport,
          IBuilding bldg, int hits, int nCluster, int bldgAbsorbs) {
        super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);

        // Report that this unit has been hit by CWS
        Report r = new Report(7510);
        r.subject = entityTarget.getId();
        r.addDesc(entityTarget);
        r.indent(2);
        vPhaseReport.add(r);

        // CWS has no effect against infantry
        if (entityTarget.isConventionalInfantry()) {
            // No Effect
            r = new Report(7515);
            r.subject = entityTarget.getId();
            r.indent(3);
            vPhaseReport.add(r);
            return;
        }

        // If the Entity is shutdown, it will remain shutdown next turn
        if (entityTarget.isShutDown()) {
            r = new Report(7511);
            r.subject = entityTarget.getId();
            r.addDesc(entityTarget);
            r.indent(3);
            vPhaseReport.add(r);
            if (entityTarget.getTaserShutdownRounds() < 1) {
                entityTarget.setTaserShutdownRounds(1);
            }
        } else { // Otherwise, there's a shutdown check
            boolean mtHeat = game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HEAT);
            int hotDogMod = 0;
            if (entityTarget.hasAbility(OptionsConstants.PILOT_HOT_DOG)) {
                hotDogMod = 1;
            }
            int shutdown = (4 + (((entityTarget.heat) / 4) * 2))
                  - hotDogMod;
            if (mtHeat) {
                shutdown -= 5;
                switch (entityTarget.getCrew().getPiloting()) {
                    case 0:
                    case 1:
                        shutdown -= 2;
                        break;
                    case 2:
                    case 3:
                        shutdown -= 1;
                        break;
                    case 6:
                    case 7:
                        shutdown += 1;
                        break;
                }
            }
            Roll diceRoll = entityTarget.getCrew().rollPilotingSkill();
            r = new Report(5060);
            r.subject = entityTarget.getId();
            r.indent(3);
            r.addDesc(entityTarget);
            r.add(shutdown);
            r.add(diceRoll);
            if (diceRoll.getIntValue() >= shutdown) {
                // avoided
                r.choose(true);
                vPhaseReport.add(r);
            } else {
                // shutting down...
                r.choose(false);
                vPhaseReport.add(r);
                // okay, now mark shut down
                if (entityTarget instanceof BattleArmor) {
                    r = new Report(3706);
                    r.addDesc(entityTarget);
                    r.indent(4);
                    // shut down for rest of scenario, treat as blown off loc
                    r.add(entityTarget.getLocationAbbr(hit));
                    vPhaseReport.add(r);
                    // TODO: fix for salvage purposes
                    entityTarget.destroyLocation(hit.getLocation());
                    // Check to see if the squad has been eliminated
                    if (entityTarget.getTransferLocation(hit).getLocation() ==
                          Entity.LOC_DESTROYED) {
                        vPhaseReport.addAll(gameManager.destroyEntity(entityTarget,
                              "all troopers eliminated", false));
                    }
                } else {
                    entityTarget.setShutDown(true);
                }
            }
        }
    }
}
