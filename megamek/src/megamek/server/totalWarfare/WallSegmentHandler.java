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

package megamek.server.totalWarfare;

import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Infantry;
import megamek.common.units.ProtoMek;
import megamek.common.units.WallRules;
import megamek.common.units.WallTarget;

/** Resolves damage, passage and independent support failure for stand-alone hexsides (TO:AR p.114). */
final class WallSegmentHandler {
    private final TWGameManager manager;

    WallSegmentHandler(TWGameManager manager) { this.manager = manager; }

    Vector<Report> damage(WallRules.Segment segment, int damage, boolean ignoreArmor) {
        Vector<Report> reports = new Vector<>();
        if (segment == null || segment.cf() <= 0 || damage <= 0) {
            return reports;
        }
        int armorDamage = ignoreArmor ? 0 : Math.min(segment.armor(), damage);
        segment.setArmor(segment.armor() - armorDamage);
        int cfDamage = Math.min(segment.cf(), damage - armorDamage);
        segment.setCF(segment.cf() - cfDamage);
        Report report = new Report(9896, Report.PUBLIC);
        report.add(segment.target(null).getDisplayName());
        report.add(armorDamage);
        report.add(cfDamage);
        report.add(segment.cf());
        reports.add(report);
        resolveSupport(segment, reports);
        manager.entityUpdate(segment.building().getId());
        return reports;
    }

    void crossing(Entity entity, Coords from, Coords to, int fromElevation, int toElevation,
          int distance, boolean backwards, EntityMovementType movement, Vector<Report> reports) {
        for (WallRules.Segment segment : WallRules.crossed(entity, from, to, fromElevation, toElevation)) {
            if (!segment.fence() && !(entity instanceof Infantry) && !(entity instanceof ProtoMek)) {
                PilotingRollData psr = entity.getBasePilotingRoll(movement);
                psr.addModifier(WallRules.pilotingModifier(segment), "crossing wall segment");
                psr.addModifier(distance >= 25 ? 6 : distance >= 18 ? 5 : distance >= 10 ? 4
                      : distance >= 7 ? 3 : distance >= 5 ? 2 : distance >= 3 ? 1 : 0, "distance moved");
                if (manager.doSkillCheckWhileMoving(entity, toElevation, from, to, psr, false, reports) > 0) {
                    int suffered = (int) Math.floor(.5 * Math.ceil(segment.cf() / 10.0));
                    if (suffered > 0) {
                        HitData hit = entity.rollHitLocation(ToHitData.HIT_NORMAL,
                              backwards ? ToHitData.SIDE_REAR : ToHitData.SIDE_FRONT);
                        hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL_NONATTACK);
                        hit.setEffect(hit.getEffect() & ~HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                        reports.addAll(manager.damageEntity(entity, hit, suffered));
                    }
                    reports.addAll(manager.rollMotiveDamageForFailedBuildingEntry(entity));
                }
            }
            // Infantry passage does not damage a structure; ProtoMeks inflict one point (TW pp.167-168).
            if (!(entity instanceof Infantry)) {
                int structuralDamage = entity instanceof ProtoMek ? 1 : (int) Math.ceil(entity.getWeight() / 10.0);
                reports.addAll(damage(segment, structuralDamage, false));
            }
        }
    }

    void resolveSupport(WallRules.Segment segment, Vector<Report> reports) {
        WallTarget target = segment.target(null);
        var occupants = manager.getGame().getEntitiesVector().stream().filter(entity -> {
            WallTarget wall = entity.getOccupiedWall();
            return wall != null && wall.getBoardId() == target.getBoardId() && wall.getId() == target.getId()
                  && wall.getTargetType() == target.getTargetType() && !entity.isAirborne();
        }).toList();
        if (segment.cf() > 0 && (!segment.fence() || occupants.isEmpty())
              && occupants.stream().mapToDouble(Entity::getWeight).sum() <= segment.cf()) {
            return;
        }
        segment.setCF(0);
        for (Entity occupant : occupants) {
            int oldElevation = occupant.getElevation();
            occupant.setOccupiedWall(null);
            reports.addAll(manager.doEntityFallsInto(occupant, oldElevation, occupant.getPosition(),
                  occupant.getPosition(), occupant.getBasePilotingRoll(), true, 0));
            manager.entityUpdate(occupant.getId());
        }
    }
}
