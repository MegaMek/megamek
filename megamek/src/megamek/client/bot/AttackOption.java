package megamek.client.bot;

import java.util.Comparator;

import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.WeaponType;


public class AttackOption extends ToHitData {
    
    public static class Sorter implements Comparator {
        CEntity primary = null;
    
        public Sorter(CEntity primary_target) {
            this.primary = primary_target;
        }
        public int compare(Object obj, Object obj1) {
            AttackOption a = (AttackOption) obj;
            AttackOption a1 = (AttackOption) obj1;
            if (a.target.getKey().intValue() == a1.target.getKey().intValue()) {
                WeaponType w = (WeaponType) a.weapon.getType();
                WeaponType w1 = (WeaponType) a1.weapon.getType();
                if (w.getDamage() == WeaponType.DAMAGE_MISSILE) {
                    if (w1.getDamage() == WeaponType.DAMAGE_MISSILE) {
                        if (a.expected > a1.expected) {
                            return -1;
                        }
                        return 1;
                    }
                    return 1;
                } else if (w.getDamage() == WeaponType.DAMAGE_MISSILE) {
                    return -1;
                } else if (a.expected > a1.expected) {
                    return -1;
                } else {
                    return 1;
                }
            } else if (a.target.getKey().equals(this.primary.getKey())) {
                return -1;
            }
            return 1;
        }
    }

    public CEntity target;
    public double value;
    public Mounted weapon;
    public ToHitData toHit;
    public double odds; // secondary odds
    public double primary_odds; // primary odds
    public int heat;
    public double expected; // damage adjusted by secondary to-hit odds
    public double primary_expected; // damage aadjusted by primary to-hit odds
    public int ammoLeft = -1; //-1 doesn't use ammo

    public AttackOption(CEntity target, Mounted weapon, double value, ToHitData toHit) {
        this.target = target;
        this.weapon = weapon;
        this.toHit = toHit;
        this.value = value;
        if (target != null) {
            WeaponType w = (WeaponType) weapon.getType();
            
            // As a primary attack.  Damage is already odds-adjusted.
            this.primary_odds = Compute.oddsAbove(toHit.getValue()) / 100.0;
            this.primary_expected = this.value;
            
            // As a secondary attack.  Raw damage is extracted, then adjusted
            // for secondary to-hit odds.  Since units with active Stealth armor
            // cannot be secondary targets, chances of hitting are 0.
            
            if (target.getEntity().isStealthActive()){
                this.odds = 0.0;
            } else {
                this.odds = Compute.oddsAbove(toHit.getValue() + 1) / 100.0;
            }
            this.heat = w.getHeat();
            this.expected = this.value/this.primary_odds;
            this.expected = this.expected * this.odds;
            final boolean isInfantryWeapon = ((w.getFlags() & WeaponType.F_INFANTRY) == WeaponType.F_INFANTRY);
            final boolean usesAmmo = (!isInfantryWeapon && w.getAmmoType() != AmmoType.T_NA);
            final Mounted ammo = usesAmmo ? weapon.getLinked() : null;
            if (usesAmmo && (ammo == null || ammo.getShotsLeft() == 0)) {
                this.value = 0; //should have already been caught...
            } else if (usesAmmo) {
                this.ammoLeft = ammo.getShotsLeft();
            }
        }
    }
}