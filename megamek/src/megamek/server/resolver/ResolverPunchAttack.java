package megamek.server.resolver;

import megamek.common.*;
import megamek.common.actions.PunchAttackAction;
import megamek.common.enums.DamageType;
import megamek.common.options.OptionsConstants;
import megamek.server.DamageEntityControl;
import megamek.server.Server;

import java.util.Vector;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class ResolverPunchAttack {
    /**
     * Handle a punch attack
     */
    public static void resolvePunchAttack(Server server, PhysicalResult pr, int lastEntityId) {
        final PunchAttackAction paa = (PunchAttackAction) pr.aaa;
        final Entity ae = server.getGame().getEntity(paa.getEntityId());
        final Targetable target = server.getGame().getTarget(paa.getTargetType(), paa.getTargetId());
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }
        boolean throughFront = true;
        if (te != null) {
            throughFront = Compute.isThroughFrontHex(server.getGame(), ae.getPosition(), te);
        }
        final String armName = (paa.getArm() == PunchAttackAction.LEFT) ? "Left Arm" : "Right Arm";
        final int armLoc = (paa.getArm() == PunchAttackAction.LEFT) ? Mech.LOC_LARM : Mech.LOC_RARM;

        // get damage, ToHitData and roll from the PhysicalResult
        int damage = paa.getArm() == PunchAttackAction.LEFT ? pr.damage : pr.damageRight;
        // LAMs in airmech mode do half damage if airborne.
        if (ae.isAirborneVTOLorWIGE()) {
            damage = (int) ceil(damage * 0.5);
        }
        final ToHitData toHit = paa.getArm() == PunchAttackAction.LEFT ? pr.toHit : pr.toHitRight;
        int roll = paa.getArm() == PunchAttackAction.LEFT ? pr.roll : pr.rollRight;
        final boolean targetInBuilding = Compute.isInBuilding(server.getGame(), te);
        final boolean glancing = server.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_GLANCING_BLOWS)
                && (roll == toHit.getValue());

        Report r;

        // Set Margin of Success/Failure.
        toHit.setMoS(roll - max(2, toHit.getValue()));
        final boolean directBlow = server.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                && ((toHit.getMoS() / 3) >= 1);

        // Which building takes the damage?
        Building bldg = server.getGame().getBoard().getBuildingAt(target.getPosition());

        if (lastEntityId != paa.getEntityId()) {
            // report who is making the attacks
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            server.addReport(r);
        }

        r = new Report(4010);
        r.subject = ae.getId();
        r.indent();
        r.add(armName);
        r.add(target.getDisplayName());
        r.newlines = 0;
        server.addReport(r);

        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(4015);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            server.addReport(r);
            if ((ae instanceof LandAirMech) && ae.isAirborneVTOLorWIGE()) {
                server.getGame().addControlRoll(new PilotingRollData(ae.getId(), 0, "missed punch attack"));
            }
            return;
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            r = new Report(4020);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            r.newlines = 0;
            server.addReport(r);
            roll = Integer.MAX_VALUE;
        } else {
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
            // nope
            r = new Report(4035);
            r.subject = ae.getId();
            server.addReport(r);

            if (ae instanceof LandAirMech && ae.isAirborneVTOLorWIGE()) {
                server.getGame().addControlRoll(new PilotingRollData(ae.getId(), 0, "missed punch attack"));
            }
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

            if (paa.isZweihandering()) {
                server.applyZweihanderSelfDamage(ae, true, Mech.LOC_RARM, Mech.LOC_LARM);
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

            if (paa.isZweihandering()) {
                server.applyZweihanderSelfDamage(ae, false,  Mech.LOC_RARM, Mech.LOC_LARM);
            }

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
            Vector<Report> buildingReport = server.damageBuilding(bldg, toBldg, target.getPosition());
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
            r.indent();
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

            damage = server.checkForSpikes(te, hit.getLocation(), damage, ae,
                    (paa.getArm() == PunchAttackAction.LEFT) ?  Mech.LOC_LARM : Mech.LOC_RARM);
            DamageType damageType = DamageType.NONE;
            server.addReport(DamageEntityControl.damageEntity(server, te, hit, damage, false, damageType, false,
                    false, throughFront));
            if (target instanceof VTOL) {
                // destroy rotor
                server.addReport(server.applyCriticalHit(te, VTOL.LOC_ROTOR,
                        new CriticalSlot(CriticalSlot.TYPE_SYSTEM, VTOL.CRIT_ROTOR_DESTROYED),
                        false, 0, false));
            }
            // check for extending retractable blades
            if (paa.isBladeExtended(paa.getArm())) {
                server.addNewLines();
                r = new Report(4455);
                r.indent(2);
                r.subject = ae.getId();
                r.newlines = 0;
                server.addReport(r);
                // conventional infantry don't take crits and battle armor need
                // to be handled differently
                if (!(target instanceof Infantry)) {
                    server.addNewLines();
                    server.addReport(server.criticalEntity(te, hit.getLocation(), hit.isRear(), 0,
                            true, false, damage));
                }

                if ((target instanceof BattleArmor) && (hit.getLocation() < te.locations())
                        && (te.getInternal(hit.getLocation()) > 0)) {
                    // TODO : we should really apply BA criticals through the critical
                    // TODO : hits methods. Right now they are applied in damageEntity
                    HitData baHit = new HitData(hit.getLocation(), false, HitData.EFFECT_CRITICAL);
                    server.addReport(server.damageEntity(te, baHit, 0));
                }
                // extend the blade
                // since retracting/extending is a freebie in the movement
                // phase, lets assume that the
                // blade retracts to its original mode
                // ae.extendBlade(paa.getArm());
                // check for breaking a nail
                if (Compute.d6(2) > 9) {
                    server.addNewLines();
                    r = new Report(4456);
                    r.indent(2);
                    r.subject = ae.getId();
                    r.newlines = 0;
                    server.addReport(r);
                    ae.destroyRetractableBlade(armLoc);
                }
            }
        }

        server.addNewLines();

        if (paa.isZweihandering()) {
            server.applyZweihanderSelfDamage(ae, false,  Mech.LOC_RARM, Mech.LOC_LARM);
        }

        server.addNewLines();

        // if the target is an industrial mech, it needs to check for crits at the end of turn
        if ((target instanceof Mech) && ((Mech) target).isIndustrial()) {
            ((Mech) target).setCheckForCrit(true);
        }
    }
}
