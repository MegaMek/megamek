
package megamek.common.weapons;

import megamek.common.AmmoType;

public class ISSRM6 extends SRMWeapon {

    public ISSRM6() {
        super();
        this.numMissiles=6;
        this.name = "SRM 6";
        this.setInternalName(this.name);
        this.addLookupName("IS SRM-6");
        this.addLookupName("ISSRM6");
        this.addLookupName("IS SRM 6");
        this.heat = 4;
        this.rackSize = 6;
        this.minimumRange = 0;
        this.shortRange = 3;
        this.mediumRange = 6;
        this.longRange = 9;
        this.tonnage = 3.0f;
        this.criticals = 2;
        this.bv = 59;
    }

}
