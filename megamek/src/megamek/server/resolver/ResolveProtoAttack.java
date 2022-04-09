package megamek.server.resolver;

import megamek.common.*;
import megamek.common.actions.ProtomechPhysicalAttackAction;
import megamek.common.enums.DamageType;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

import java.util.Vector;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class ResolveProtoAttack {
    /**
     * Handle a ProtoMech physical attack
     */
    public static void resolveProtoAttack(Server server, Game game, Vector<Report> vPhaseReport, PhysicalResult pr, int lastEntityId) {
        final ProtomechPhysicalAttackAction ppaa = (ProtomechPhysicalAttackAction) pr.aaa;
        final Entity ae = game.getEntity(ppaa.getEntityId());
        // get damage, ToHitData and roll from the PhysicalResult
        int damage = pr.damage;
        final ToHitData toHit = pr.toHit;
        int roll = pr.roll;
        final Targetable target = game.getTarget(ppaa.getTargetType(), ppaa.getTargetId());
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }
        boolean throughFront = true;
        if (te != null) {
            throughFront = Compute.isThroughFrontHex(game, ae.getPosition(), te);
        }
        final boolean targetInBuilding = Compute.isInBuilding(game, te);
        final boolean glancing = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_GLANCING_BLOWS)
                && (roll == toHit.getValue());
        // Set Margin of Success/Failure.
        toHit.setMoS(roll - max(2, toHit.getValue()));
        final boolean directBlow = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                && ((toHit.getMoS() / 3) >= 1);

        Report r;

        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt(target.getPosition());

        if (lastEntityId != ae.getId()) {
            // who is making the attacks
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            server.addReport(r);
        }

        r = new Report(4070);
        r.subject = ae.getId();
        r.indent();
        r.add(target.getDisplayName());
        r.newlines = 0;
        server.addReport(r);

        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(4075);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            server.addReport(r);
            return;
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            r = new Report(4080);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            r.newlines = 0;
            server.addReport(r);
            roll = Integer.MAX_VALUE;
        } else {
            // report the roll
            r = new Report(4025);
            r.subject = ae.getId();
            r.add(toHit);
            r.add(roll);
            r.newlines = 0;
            server.addReport(r);
            if (glancing) {
                r = new Report(3186);
                r.subject = ae.getId();
                r.newlines = 0;
                server.addReport(r);
            }

            if (directBlow) {
                r = new Report(3189);
                r.subject = ae.getId();
                r.newlines = 0;
                server.addReport(r);
            }
        }

        // do we hit?
        if (roll < toHit.getValue()) {
            // miss
            r = new Report(4035);
            r.subject = ae.getId();
            server.addReport(r);

            // If the target is in a building, the building absorbs the damage.
            if (targetInBuilding && (bldg != null)) {
                // Only report if damage was done to the building.
                if (damage > 0) {
                    Vector<Report> buildingReport = server.damageBuilding(bldg, damage, target.getPosition());
                    for (Report report : buildingReport) {
                        report.subject = ae.getId();
                    }
                    server.addReport(buildingReport);
                }
            }
            return;
        }

        // Targeting a building.
        if ((target.getTargetType() == Targetable.TYPE_BUILDING)
                || (target.getTargetType() == Targetable.TYPE_FUEL_TANK)) {
            // The building takes the full brunt of the attack.
            r = new Report(4040);
            r.subject = ae.getId();
            server.addReport(r);
            Vector<Report> buildingReport = server.damageBuilding(bldg, damage, target.getPosition());
            for (Report report : buildingReport) {
                report.subject = ae.getId();
            }
            server.addReport(buildingReport);

            // Damage any infantry in the hex.
            server.addReport(server.damageInfantryIn(bldg, damage, target.getPosition()));

            // And we're done!
            return;
        }

        HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
        hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);

        r = new Report(4045);
        r.subject = ae.getId();
        r.add(toHit.getTableDesc());
        r.add(te.getLocationAbbr(hit));
        server.addReport(r);

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        if (targetInBuilding && (bldg != null)) {
            int bldgAbsorbs = bldg.getAbsorbtion(target.getPosition());
            int toBldg = min(bldgAbsorbs, damage);
            damage -= toBldg;
            server.addNewLines();
            Vector<Report> buildingReport = server.damageBuilding(bldg, damage, target.getPosition());
            for (Report report : buildingReport) {
                report.subject = ae.getId();
            }
            server.addReport(buildingReport);

            // some buildings scale remaining damage that is not absorbed
            // TODO : this isn't quite right for castles brian
            damage = (int) floor(bldg.getDamageToScale() * damage);
        }

        // A building may absorb the entire shot.
        if (damage == 0) {
            r = new Report(4050);
            r.subject = ae.getId();
            r.add(te.getShortName());
            r.add(te.getOwner().getName());
            r.newlines = 0;
            server.addReport(r);
        } else {
            if (glancing) {
                // Round up glancing blows against conventional infantry
                damage = (int) (te.isConventionalInfantry() ? ceil(damage / 2.0) : floor(damage / 2.0));
            }

            if (directBlow) {
                damage += toHit.getMoS() / 3;
                hit.makeDirectBlow(toHit.getMoS() / 3);
            }
            server.addReport(server.damageEntity(te, hit, damage, false, DamageType.NONE,
                    false, false, throughFront));
            if (((Protomech) ae).isEDPCharged()) {
                r = new Report(3701);
                int taserRoll = Compute.d6(2) - 2;
                r.add(taserRoll);
                r.newlines = 0;
                vPhaseReport.add(r);

                if (te instanceof BattleArmor) {
                    r = new Report(3706);
                    r.addDesc(te);
                    // shut down for rest of scenario, so we actually kill it
                    // TODO : fix for salvage purposes
                    HitData targetTrooper = te.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
                    r.add(te.getLocationAbbr(targetTrooper));
                    vPhaseReport.add(r);
                    vPhaseReport.addAll(server.criticalEntity(ae, targetTrooper.getLocation(),
                            targetTrooper.isRear(), 0, false, false, 0));
                } else if (te instanceof Mech) {
                    if (((Mech) te).isIndustrial()) {
                        if (taserRoll >= 8) {
                            r = new Report(3705);
                            r.addDesc(te);
                            r.add(4);
                            te.taserShutdown(4, false);
                        } else {
                            // suffer +2 to piloting and gunnery for 4 rounds
                            r = new Report(3710);
                            r.addDesc(te);
                            r.add(2);
                            r.add(4);
                            te.setTaserInterference(2, 4, true);
                        }
                    } else {
                        if (taserRoll >= 11) {
                            r = new Report(3705);
                            r.addDesc(te);
                            r.add(3);
                            vPhaseReport.add(r);
                            te.taserShutdown(3, false);
                        } else {
                            r = new Report(3710);
                            r.addDesc(te);
                            r.add(2);
                            r.add(3);
                            vPhaseReport.add(r);
                            te.setTaserInterference(2, 3, true);
                        }
                    }
                } else if ((te instanceof Protomech) || (te instanceof Tank)
                           || (te instanceof Aero)) {
                    if (taserRoll >= 8) {
                        r = new Report(3705);
                        r.addDesc(te);
                        r.add(4);
                        vPhaseReport.add(r);
                        te.taserShutdown(4, false);
                    } else {
                        r = new Report(3710);
                        r.addDesc(te);
                        r.add(2);
                        r.add(4);
                        vPhaseReport.add(r);
                        te.setTaserInterference(2, 4, false);
                    }
                }

            }
        }

        server.addNewLines();

        // if the target is an industrial mech, it needs to check for crits at the end of turn
        if ((target instanceof Mech) && ((Mech) target).isIndustrial()) {
            ((Mech) target).setCheckForCrit(true);
        }
    }
}
