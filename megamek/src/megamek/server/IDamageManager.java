package megamek.server;

import megamek.common.DamageInfo;
import megamek.common.Report;

import java.util.Vector;

public interface IDamageManager {
    public Vector<Report> damageEntity(DamageInfo damageInfo);
}
