package megamek.server.resolver;

import megamek.common.*;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

public class ResolveUnjam {
    /**
     * Resolve an Unjam Action object
     */
    public static void resolveUnjam(Server server, Entity entity) {
        Report r;
        final int TN = entity.getCrew().getGunnery() + 3;
        if (server.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_UNJAM_UAC)) {
            r = new Report(3026);
        } else {
            r = new Report(3025);
        }
        r.subject = entity.getId();
        r.addDesc(entity);
        server.addReport(r);
        for (Mounted mounted : entity.getTotalWeaponList()) {
            if (mounted.isJammed() && !mounted.isDestroyed()) {
                WeaponType wtype = (WeaponType) mounted.getType();
                if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                    int roll = Compute.d6(2);
                    r = new Report(3030);
                    r.indent();
                    r.subject = entity.getId();
                    r.add(wtype.getName());
                    r.add(TN);
                    r.add(roll);
                    if (roll >= TN) {
                        r.choose(true);
                        mounted.setJammed(false);
                    } else {
                        r.choose(false);
                    }
                    server.addReport(r);
                }
                // Unofficial option to unjam UACs, ACs, and LACs like Rotary
                // Autocannons
                if (((wtype.getAmmoType() == AmmoType.T_AC_ULTRA)
                        || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)
                        || (wtype.getAmmoType() == AmmoType.T_AC)
                        || (wtype.getAmmoType() == AmmoType.T_AC_IMP)
                        || (wtype.getAmmoType() == AmmoType.T_PAC)
                        || (wtype.getAmmoType() == AmmoType.T_LAC))
                        && server.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_UNJAM_UAC)) {
                    int roll = Compute.d6(2);
                    r = new Report(3030);
                    r.indent();
                    r.subject = entity.getId();
                    r.add(wtype.getName());
                    r.add(TN);
                    r.add(roll);
                    if (roll >= TN) {
                        r.choose(true);
                        mounted.setJammed(false);
                    } else {
                        r.choose(false);
                    }
                    server.addReport(r);
                }
            }
        }
    }
}
