package megamek.server.resolver;

import megamek.common.*;
import megamek.common.actions.BAVibroClawAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class ResolveBAVibroClawAttack {
    /**
     * Handle a thrash attack
     */
    public static void resolveBAVibroClawAttack(Server server, Game game, PhysicalResult pr, int lastEntityId) {
        final BAVibroClawAttackAction bvaa = (BAVibroClawAttackAction) pr.aaa;
        final Entity ae = game.getEntity(bvaa.getEntityId());

        // get damage, ToHitData and roll from the PhysicalResult
        int hits = pr.damage;
        final ToHitData toHit = pr.toHit;
        int roll = pr.roll;
        final boolean glancing = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_GLANCING_BLOWS)
                && (roll == toHit.getValue());

        // Set Margin of Success/Failure.
        toHit.setMoS(roll - max(2, toHit.getValue()));
        final boolean directBlow = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                && ((toHit.getMoS() / 3) >= 1);

        // PLEASE NOTE: buildings are *never* the target of a BA vibroclaw attack.
        final Entity te = game.getEntity(bvaa.getTargetId());
        Report r;

        if (lastEntityId != bvaa.getEntityId()) {
            // who is making the attacks
            r = new Report(4005);
            r.subject = ae.getId();
            r.addDesc(ae);
            server.addReport(r);
        }

        r = new Report(4146);
        r.subject = ae.getId();
        r.indent();
        r.addDesc(te);
        r.newlines = 0;
        server.addReport(r);

        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(4147);
            r.subject = ae.getId();
            r.add(toHit.getDesc());
            server.addReport(r);
            return;
        }

        // we may hit automatically
        if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            r = new Report(4120);
            r.subject = ae.getId();
            r.newlines = 0;
            server.addReport(r);
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
        }

        // Standard damage loop
        if (glancing) {
            hits = (int) floor(hits / 2.0);
        }

        if (directBlow) {
            hits += toHit.getMoS() / 3;
        }

        if (te.isConventionalInfantry()) {
            r = new Report(4149);
            r.subject = ae.getId();
            r.add(hits);
        } else {
            r = new Report(4148);
            r.subject = ae.getId();
            r.add(hits);
            r.add(ae.getVibroClaws());
        }
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
            // BA get hit separately by each attacking BA trooper
            int damage = min(ae.getVibroClaws(), hits);
            // conventional infantry get hit in one lump
            if (te.isConventionalInfantry()) {
                damage = hits;
            }
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
    }
}
