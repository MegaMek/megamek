package megamek.server.resolver;

import megamek.common.*;
import megamek.common.actions.ThrashAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class ResolveThrashAttack {
    /**
     * Handle a thrash attack
     */
    public static void resolveThrashAttack(Server server, PhysicalResult pr, int lastEntityId) {
        final ThrashAttackAction taa = (ThrashAttackAction) pr.aaa;
        final Entity ae = server.getGame().getEntity(taa.getEntityId());

        // get damage, ToHitData and roll from the PhysicalResult
        int hits = pr.damage;
        final ToHitData toHit = pr.toHit;
        int roll = pr.roll;
        final boolean glancing = server.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_GLANCING_BLOWS)
                && (roll == toHit.getValue());

        // Set Margin of Success/Failure.
        toHit.setMoS(roll - max(2, toHit.getValue()));
        final boolean directBlow = server.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                && ((toHit.getMoS() / 3) >= 1);

        // PLEASE NOTE: buildings are *never* the target of a "thrash".
        final Entity te = server.getGame().getEntity(taa.getTargetId());
        Report r;

        if (lastEntityId != taa.getEntityId()) {
            // who is making the attacks
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            server.addReport(r);
        }

        r = new Report(4110);
        r.subject = ae.getId();
        r.indent();
        r.addDesc(te);
        r.newlines = 0;
        server.addReport(r);

        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(4115);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            server.addReport(r);
            return;
        }

        // Thrash attack may hit automatically
        if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            r = new Report(4120);
        } else {
            // report the roll
            r = new Report(4025);
            r.subject = ae.getId();
            r.add(toHit);
            r.add(roll);
            r.newlines = 0;
            server.addReport(r);

            // do we hit?
            if (roll < toHit.getValue()) {
                // miss
                r = new Report(4035);
                r.subject = ae.getId();
                server.addReport(r);
                return;
            }
            r = new Report(4125);
        }
        r.subject = ae.getId();
        r.newlines = 0;
        server.addReport(r);

        // Standard damage loop in 5 point clusters.
        if (glancing) {
            hits = (int) floor(hits / 2.0);
        }

        if (directBlow) {
            hits += toHit.getMoS() / 3;
        }

        r = new Report(4130);
        r.subject = ae.getId();
        r.add(hits);
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

        while (hits > 0) {
            int damage = min(5, hits);
            hits -= damage;
            HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
            hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
            r = new Report(4135);
            r.subject = ae.getId();
            r.add(te.getLocationAbbr(hit));
            r.newlines = 0;
            server.addReport(r);
            server.addReport(server.damageEntity(te, hit, damage));
        }

        server.addNewLines();

        // Thrash attacks cause PSRs. Failed PSRs cause falling damage.
        // This fall damage applies even though the Thrashing Mek is prone.
        PilotingRollData rollData = ae.getBasePilotingRoll();
        ae.addPilotingModifierForTerrain(rollData);
        rollData.addModifier(0, "thrashing at infantry");
        r = new Report(4140);
        r.subject = ae.getId();
        r.addDesc(ae);
        server.addReport(r);
        final int diceRoll = Compute.d6(2);
        r = new Report(2190);
        r.subject = ae.getId();
        r.add(rollData.getValueAsString());
        r.add(rollData.getDesc());
        r.add(diceRoll);
        if (diceRoll < rollData.getValue()) {
            r.choose(false);
            server.addReport(r);
            server.addReport(server.doEntityFall(ae, rollData));
        } else {
            r.choose(true);
            server.addReport(r);
        }
    }
}
