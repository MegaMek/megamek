package megamek.server.resolver;

import megamek.common.*;
import megamek.common.actions.KickAttackAction;
import megamek.common.enums.DamageType;
import megamek.common.options.OptionsConstants;
import megamek.server.DamageEntityControl;
import megamek.server.Server;

import java.util.Vector;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class ResolveKickAttack {
    /**
     * Handle a kick attack
     */
    public static void resolveKickAttack(Server server, Game game, PhysicalResult pr, int lastEntityId) {
        KickAttackAction kaa = (KickAttackAction) pr.aaa;
        final Entity ae = game.getEntity(kaa.getEntityId());
        final Targetable target = game.getTarget(kaa.getTargetType(), kaa.getTargetId());
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }
        boolean throughFront = true;
        if (te != null) {
            throughFront = Compute.isThroughFrontHex(game, ae.getPosition(), te);
        }
        String legName = (kaa.getLeg() == KickAttackAction.LEFT)
                || (kaa.getLeg() == KickAttackAction.LEFTMULE) ? "Left " : "Right ";
        if ((kaa.getLeg() == KickAttackAction.LEFTMULE)
                || (kaa.getLeg() == KickAttackAction.RIGHTMULE)) {
            legName = legName.concat("rear ");
        } else if (ae instanceof QuadMech) {
            legName = legName.concat("front ");
        }
        legName = legName.concat("leg");
        Report r;

        // get damage, ToHitData and roll from the PhysicalResult
        int damage = pr.damage;
        // LAMs in airmech mode do half damage if airborne.
        if (ae.isAirborneVTOLorWIGE()) {
            damage = (int) ceil(damage * 0.5);
        }
        final ToHitData toHit = pr.toHit;
        int roll = pr.roll;
        final boolean targetInBuilding = Compute.isInBuilding(game, te);
        final boolean glancing = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_GLANCING_BLOWS)
                && (roll == toHit.getValue());

        // Set Margin of Success/Failure.
        toHit.setMoS(roll - max(2, toHit.getValue()));
        final boolean directBlow = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                && ((toHit.getMoS() / 3) >= 1);

        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt(target.getPosition());

        if (lastEntityId != ae.getId()) {
            // who is making the attacks
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            server.addReport(r);
        }

        r = new Report(4055);
        r.subject = ae.getId();
        r.indent();
        r.add(legName);
        r.add(target.getDisplayName());
        r.newlines = 0;
        server.addReport(r);

        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(4060);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            server.addReport(r);
            if ((ae instanceof LandAirMech) && ae.isAirborneVTOLorWIGE()) {
                game.addControlRoll(new PilotingRollData(ae.getId(), 0, "missed a kick"));
            } else {
                game.addPSR(new PilotingRollData(ae.getId(), 0, "missed a kick"));
            }
            return;
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            r = new Report(4065);
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
            if (ae instanceof LandAirMech && ae.isAirborneVTOLorWIGE()) {
                game.addControlRoll(new PilotingRollData(ae.getId(), 0, "missed a kick"));
            } else {
                game.addPSR(new PilotingRollData(ae.getId(), 0, "missed a kick"));
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

            int leg;
            switch (kaa.getLeg()) {
                case KickAttackAction.LEFT:
                    leg = (ae instanceof QuadMech) ? Mech.LOC_LARM : Mech.LOC_LLEG;
                    break;
                case KickAttackAction.RIGHT:
                    leg = (ae instanceof QuadMech) ? Mech.LOC_RARM : Mech.LOC_RLEG;
                    break;
                case KickAttackAction.LEFTMULE:
                    leg = Mech.LOC_LLEG;
                    break;
                case KickAttackAction.RIGHTMULE:
                default:
                    leg = Mech.LOC_RLEG;
                    break;
            }
            damage = server.checkForSpikes(te, hit.getLocation(), damage, ae, leg);
            DamageType damageType = DamageType.NONE;
            server.addReport(DamageEntityControl.damageEntity(server, te, hit, damage, false, damageType, false,
                    false, throughFront));
            if (target instanceof VTOL) {
                // destroy rotor
                server.addReport(server.applyCriticalHit(te, VTOL.LOC_ROTOR,
                        new CriticalSlot(CriticalSlot.TYPE_SYSTEM, VTOL.CRIT_ROTOR_DESTROYED),
                        false, 0, false));
            }

            if (te.hasQuirk(OptionsConstants.QUIRK_NEG_WEAK_LEGS)) {
                server.addNewLines();
                server.addReport(server.criticalEntity(te, hit.getLocation(), hit.isRear(), 0, 0));
            }
        }

        if (te.canFall()) {
            PilotingRollData kickPRD = server.getKickPushPSR(te, te, "was kicked");
            game.addPSR(kickPRD);
        }

        // if the target is an industrial mech, it needs to check for crits at the end of turn
        if ((te instanceof Mech) && ((Mech) te).isIndustrial()) {
            ((Mech) te).setCheckForCrit(true);
        }

        server.addNewLines();
    }
}
