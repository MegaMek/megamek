/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
import java.util.ArrayList;
import java.util.Vector;

import megamek.common.HexTarget;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.BuildingTarget;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Targetable;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Weapon handler for vehicular grenade launchers.  Rather than have a separate handler for each ammo type, all ammo
 * types are handled here.
 *
 * @author arlith
 */
public class VGLWeaponHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = -4934490646657484486L;

    public VGLWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
        generalDamageType = HitData.DAMAGE_NONE;
    }

    /**
     * handle this weapons firing
     *
     * @return a <code>boolean</code> value indicating whether this should be kept or not
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }
        // VGLs automatically hit the three adjacent hex in their side


        // Determine what coords get hit
        AmmoType ammoType = ammo.getType();
        int facing = weapon.getFacing();
        ArrayList<Coords> affectedCoords = new ArrayList<>(3);
        int af = attackingEntity.getFacing();
        if (attackingEntity.isSecondaryArcWeapon(attackingEntity.getEquipmentNum(weapon))) {
            af = attackingEntity.getSecondaryFacing();
        }
        affectedCoords.add(attackingEntity.getPosition().translated(af + facing));
        affectedCoords.add(attackingEntity.getPosition().translated((af + facing + 1) % 6));
        affectedCoords.add(attackingEntity.getPosition().translated((af + facing + 5) % 6));

        Report r = new Report(3117);
        r.indent();
        r.subject = subjectId;
        r.add(weaponType.getName());
        r.add(ammoType.getSubMunitionName());
        r.add(affectedCoords.get(0).getBoardNum());
        r.add(affectedCoords.get(1).getBoardNum());
        r.add(affectedCoords.get(2).getBoardNum());
        vPhaseReport.add(r);


        for (Coords c : affectedCoords) {
            IBuilding bldg = game.getBoard().getBuildingAt(c);
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_SMOKE)) {
                gameManager.deliverSmokeGrenade(c, vPhaseReport);
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_CHAFF)) {
                gameManager.deliverChaffGrenade(c, vPhaseReport);
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_INCENDIARY)) {
                Vector<Report> dmgReports;
                // Delivery an inferno to the hex
                Targetable grenadeTarget = new HexTarget(c, Targetable.TYPE_HEX_IGNITE);
                dmgReports = gameManager
                      .deliverInfernoMissiles(attackingEntity, grenadeTarget, 1);
                r = new Report(3372);
                r.add("Hex " + c.getBoardNum());
                r.indent();
                dmgReports.insertElementAt(r, 0);
                dmgReports.get(dmgReports.size() - 1).newlines++;
                for (Report dr : dmgReports) {
                    dr.indent();
                }
                vPhaseReport.addAll(dmgReports);
                // If there's a building, delivery an inferno to it
                if (bldg != null) {
                    grenadeTarget = new BuildingTarget(c, game.getBoard(),
                          Targetable.TYPE_BLDG_IGNITE);
                    dmgReports = gameManager.deliverInfernoMissiles(attackingEntity,
                          grenadeTarget, 1);
                    r = new Report(3372);
                    r.add(bldg.getName());
                    r.indent();
                    dmgReports.insertElementAt(r, 0);
                    dmgReports.get(dmgReports.size() - 1).newlines++;
                    for (Report dr : dmgReports) {
                        dr.indent();
                    }
                    vPhaseReport.addAll(dmgReports);
                }
                // Delivery an inferno to each entity in the affected hex
                for (Entity entTarget : game.getEntitiesVector(c)) {
                    // Infantry in a building take damage when the building is
                    //  targeted, so should be ignored here
                    if (bldg != null && (entTarget instanceof Infantry)
                          && Compute.isInBuilding(game, entTarget)) {
                        continue;
                    }
                    dmgReports = gameManager
                          .deliverInfernoMissiles(attackingEntity, entTarget, 1);
                    r = new Report(3371);
                    r.addDesc(entTarget);
                    r.indent();
                    dmgReports.insertElementAt(r, 0);
                    dmgReports.get(dmgReports.size() - 1).newlines++;
                    for (Report dr : dmgReports) {
                        dr.indent();
                    }
                    vPhaseReport.addAll(dmgReports);
                }
            } else { // Assume fragmentation grenade
                // Damage each Entity in the target coord
                for (Entity entTarget : game.getEntitiesVector(c)) {
                    boolean inBuilding = (bldg != null)
                          && Compute.isInBuilding(game, entTarget, c);

                    hit = entTarget.rollHitLocation(toHit.getHitTable(),
                          toHit.getSideTable(), weaponAttackAction.getAimedLocation(),
                          weaponAttackAction.getAimingMode(), toHit.getCover());
                    hit.setAttackerId(getAttackerId());

                    Vector<Report> dmgReports = new Vector<>();
                    // Infantry take 2D6 burst damage
                    if (!inBuilding && entTarget.isConventionalInfantry()) {
                        int infDmg = Compute.directBlowInfantryDamage(0, 0,
                              WeaponType.WEAPON_BURST_2D6,
                              ((Infantry) entTarget).isMechanized(),
                              toHit.getThruBldg() != null);
                        dmgReports = gameManager.damageEntity(entTarget, hit, infDmg);
                    } else if (inBuilding && entTarget.isConventionalInfantry()) {
                        r = new Report(3417);
                        r.addDesc(entTarget);
                        r.indent(2);
                        dmgReports.add(r);
                    } else if (entTarget.getBARRating(hit.getLocation()) < 5) {
                        int dmg = 5 - entTarget.getBARRating(hit.getLocation());
                        dmgReports = gameManager.damageEntity(entTarget, hit, dmg);
                    } else {
                        r = new Report(3416);
                        r.addDesc(entTarget);
                        r.indent(2);
                        dmgReports.add(r);
                    }
                    dmgReports.get(dmgReports.size() - 1).newlines++;
                    vPhaseReport.addAll(dmgReports);
                }
            }
        }

        return false;
    }

}

