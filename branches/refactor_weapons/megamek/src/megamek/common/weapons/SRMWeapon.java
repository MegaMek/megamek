
package megamek.common.weapons;

import megamek.common.AmmoType;

public abstract class SRMWeapon extends MissileWeapon {
    public SRMWeapon() {
        damagePerMissile=2;
        damageCluster=2;
        this.ammoType = AmmoType.T_SRM;        
    }
}
