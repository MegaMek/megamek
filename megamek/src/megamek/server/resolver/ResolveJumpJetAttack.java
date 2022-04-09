package megamek.server.resolver;

import megamek.common.*;
import megamek.common.actions.JumpJetAttackAction;
import megamek.common.enums.DamageType;
import megamek.common.options.OptionsConstants;
import megamek.server.DamageEntityControl;
import megamek.server.Server;

import java.util.Vector;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class ResolveJumpJetAttack {
    /**
     * Handle a kick attack
     */
    public static void resolveJumpJetAttack(Server server, PhysicalResult pr, int lastEntityId) {
        JumpJetAttackAction kaa = (JumpJetAttackAction) pr.aaa;
        final Entity ae = server.getGame().getEntity(kaa.getEntityId());
        final Targetable target = server.getGame().getTarget(kaa.getTargetType(), kaa.getTargetId());
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }
        boolean throughFront = true;
        if (te != null) {
            throughFront = Compute.isThroughFrontHex(server.getGame(), ae.getPosition(), te);
        }
        String legName;
        switch (kaa.getLeg()) {
            case JumpJetAttackAction.LEFT:
                legName = "Left leg";
                break;
            case JumpJetAttackAction.RIGHT:
                legName = "Right leg";
                break;
            default:
                legName = "Both legs";
                break;
        }

        Report r;

        // get damage, ToHitData and roll from the PhysicalResult
        int damage = pr.damage;
        final ToHitData toHit = pr.toHit;
        int roll = pr.roll;
        final boolean targetInBuilding = Compute.isInBuilding(server.getGame(), te);
        final boolean glancing = server.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_GLANCING_BLOWS)
                && (roll == toHit.getValue());

        // Set Margin of Success/Failure.
        toHit.setMoS(roll - max(2, toHit.getValue()));
        final boolean directBlow = server.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                && ((toHit.getMoS() / 3) >= 1);

        // Which building takes the damage?
        Building bldg = server.getGame().getBoard().getBuildingAt(target.getPosition());

        if (lastEntityId != ae.getId()) {
            // who is making the attacks
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            server.addReport(r);
        }

        r = new Report(4290);
        r.subject = ae.getId();
        r.indent();
        r.add(legName);
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
                damage += pr.damageRight;
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
            damage += pr.damageRight;
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

        r = new Report(4040);
        r.subject = ae.getId();
        r.newlines = 0;
        server.addReport(r);

        for (int leg = 0; leg < 2; leg++) {
            if (leg == 1) {
                damage = pr.damageRight;
                if (damage == 0) {
                    break;
                }
            }
            HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
            hit.setGeneralDamageType(HitData.DAMAGE_ENERGY);

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
                server.addReport(DamageEntityControl.damageEntity(server, te, hit, damage, false, DamageType.NONE,
                        false, false, throughFront));
            }
        }

        server.addNewLines();

        // if the target is an industrial mech, it needs to check for crits at the end of turn
        if ((target instanceof Mech) && ((Mech) target).isIndustrial()) {
            ((Mech) target).setCheckForCrit(true);
        }
    }
}
