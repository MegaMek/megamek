package megamek.server.resolver;

import megamek.common.*;
import megamek.common.actions.AirmechRamAttackAction;
import megamek.common.actions.ChargeAttackAction;
import megamek.common.enums.DamageType;
import megamek.common.options.OptionsConstants;
import megamek.server.DamageEntityControl;
import megamek.server.Server;

import java.util.Vector;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.min;

public class ResolveChargeDamage {
    /**
     * Handle a charge's damage
     */
    public static void resolveChargeDamage(Server server, Entity ae, Entity te, ToHitData toHit, int direction) {
        resolveChargeDamage(server, server.getGame(), ae, te, toHit, direction, false, true, false);
    }

    public static void resolveChargeDamage(Server server, Game game, Entity ae, Entity te, ToHitData toHit, int direction, boolean glancing, boolean throughFront, boolean airmechRam) {
        // we hit...

        PilotingRollData chargePSR = null;
        // If we're upright, we may fall down.
        if (!ae.isProne() && !airmechRam) {
            chargePSR = new PilotingRollData(ae.getId(), 2, "charging");
        }

        // Damage To Target
        int damage;

        // Damage to Attacker
        int damageTaken;

        if (airmechRam) {
            damage = AirmechRamAttackAction.getDamageFor(ae);
            damageTaken = AirmechRamAttackAction.getDamageTakenBy(ae, te);
        } else {
            damage = ChargeAttackAction.getDamageFor(ae, te, game.getOptions()
                    .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CHARGE_DAMAGE), toHit.getMoS());
            damageTaken = ChargeAttackAction.getDamageTakenBy(ae, te, game
                    .getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CHARGE_DAMAGE));
        }
        if (ae.hasWorkingMisc(MiscType.F_RAM_PLATE)) {
            damage = (int) ceil(damage * 1.5);
            damageTaken = (int) floor(damageTaken * 0.5);
        }
        if (glancing) {
            // Glancing Blow rule doesn't state whether damage to attacker on charge
            // or DFA is halved as well, assume yes. TODO : Check with PM
            damage = (int) (te.isConventionalInfantry() ? ceil(damage / 2.0) : floor(damage / 2.0));
            damageTaken = (int) floor(damageTaken / 2.0);
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                && ((toHit.getMoS() / 3) >= 1)) {
            damage += toHit.getMoS() / 3;
        }

        // Is the target inside a building?
        final boolean targetInBuilding = Compute.isInBuilding(game, te);

        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt(te.getPosition());

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        int bldgAbsorbs = 0;
        if (targetInBuilding && (bldg != null)) {
            bldgAbsorbs = bldg.getAbsorbtion(te.getPosition());
        }

        // damage to attacker
        Report r = new Report(4240);
        r.subject = ae.getId();
        r.add(damageTaken);
        r.indent();
        server.addReport(r);

        // Charging vehicles check for possible motive system hits.
        if (ae instanceof Tank) {
            r = new Report(4241);
            r.indent();
            server.addReport(r);
            int side = Compute.targetSideTable(te, ae);
            int mod = ae.getMotiveSideMod(side);
            server.addReport(server.vehicleMotiveDamage((Tank) ae, mod));
        }

        while (damageTaken > 0) {
            int cluster;
            HitData hit;
            // An airmech ramming attack does all damage to attacker's CT
            if (airmechRam) {
                cluster = damageTaken;
                hit = new HitData(Mech.LOC_CT);
            } else {
                cluster = min(5, damageTaken);
                hit = ae.rollHitLocation(toHit.getHitTable(), ae.sideTable(te.getPosition()));
            }
            damageTaken -= cluster;
            hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
            cluster = server.checkForSpikes(ae, hit.getLocation(), cluster, te, Mech.LOC_CT);
            server.addReport(DamageEntityControl.damageEntity(server, ae, hit, cluster, false, DamageType.NONE,
                    false, false, throughFront));
        }

        // Damage to target
        if (ae instanceof Mech) {
            int spikeDamage = 0;
            for (int loc = 0; loc < ae.locations(); loc++) {
                if (((Mech) ae).locationIsTorso(loc) && ae.hasWorkingMisc(MiscType.F_SPIKES, -1, loc)) {
                    spikeDamage += 2;
                }
            }

            if (spikeDamage > 0) {
                r = new Report(4335);
                r.indent(2);
                r.subject = ae.getId();
                r.add(spikeDamage);
                server.addReport(r);
            }
            damage += spikeDamage;
        }
        r = new Report(4230);
        r.subject = ae.getId();
        r.add(damage);
        r.add(toHit.getTableDesc());
        r.indent();
        server.addReport(r);

        // Vehicles that have *been* charged check for motive system damage,
        // too...
        // ...though VTOLs don't use that table and should lose their rotor
        // instead,
        // which would be handled as part of the damage already.
        if ((te instanceof Tank) && !(te instanceof VTOL)) {
            r = new Report(4242);
            r.indent();
            server.addReport(r);

            int side = Compute.targetSideTable(ae, te);
            int mod = te.getMotiveSideMod(side);
            server.addReport(server.vehicleMotiveDamage((Tank) te, mod));
        }

        // track any additional damage to the attacker due to the target having spikes
        while (damage > 0) {
            int cluster = min(5, damage);
            // Airmech ramming attacks do all damage to a single location
            if (airmechRam) {
                cluster = damage;
            }
            damage -= cluster;
            if (bldgAbsorbs > 0) {
                int toBldg = min(bldgAbsorbs, cluster);
                cluster -= toBldg;
                server.addNewLines();
                Vector<Report> buildingReport = server.damageBuilding(bldg, damage, te.getPosition());
                for (Report report : buildingReport) {
                    report.subject = ae.getId();
                }
                server.addReport(buildingReport);

                // some buildings scale remaining damage that is not absorbed
                // TODO : this isn't quite right for castles brian
                damage = (int) floor(bldg.getDamageToScale() * damage);
            }

            // A building may absorb the entire shot.
            if (cluster == 0) {
                r = new Report(4235);
                r.subject = ae.getId();
                r.addDesc(te);
                r.indent();
                server.addReport(r);
            } else {
                HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
                hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
                cluster = server.checkForSpikes(te, hit.getLocation(), cluster, ae, Mech.LOC_CT);
                server.addReport(DamageEntityControl.damageEntity(server, te, hit, cluster, false,
                        DamageType.NONE, false, false, throughFront));
            }
        }

        if (airmechRam) {
            if (!ae.isDoomed()) {
                PilotingRollData controlRoll = ae.getBasePilotingRoll();
                Vector<Report> reports = new Vector<>();
                r = new Report(9320);
                r.subject = ae.getId();
                r.addDesc(ae);
                r.add("successful ramming attack");
                reports.add(r);
                int diceRoll = Compute.d6(2);
                // different reports depending on out-of-control status
                r = new Report(9606);
                r.subject = ae.getId();
                r.add(controlRoll.getValueAsString());
                r.add(controlRoll.getDesc());
                r.add(diceRoll);
                r.newlines = 1;
                if (diceRoll < controlRoll.getValue()) {
                    r.choose(false);
                    reports.add(r);
                    server.crashAirMech(ae, controlRoll, reports);
                } else {
                    r.choose(true);
                    reports.addElement(r);
                    if (ae instanceof LandAirMech) {
                        reports.addAll(server.landAirMech((LandAirMech) ae, ae.getPosition(), 1, ae.delta_distance));
                    }
                }
                server.addReport(reports);
            }
        } else {
            // move attacker and target, if possible
            Coords src = te.getPosition();
            Coords dest = src.translated(direction);

            if (Compute.isValidDisplacement(game, te.getId(), te.getPosition(), direction)) {
                server.addNewLines();
                server.addReport(server.doEntityDisplacement(te, src, dest, new PilotingRollData(
                        te.getId(), 2, "was charged")));
                server.addReport(server.doEntityDisplacement(ae, ae.getPosition(), src, chargePSR));
            }

            server.addNewLines();
        }

        // if the target is an industrial mech, it needs to check for crits at the end of turn
        if ((te instanceof Mech) && ((Mech) te).isIndustrial()) {
            ((Mech) te).setCheckForCrit(true);
        }
    }
}
