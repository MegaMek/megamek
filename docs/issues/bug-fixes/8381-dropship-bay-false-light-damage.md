# Issue #8381 - Custom DropShip shows "light damage" at best condition

## Summary
A custom DropShip (`Lee (Pocket CM)`) is reported as **light damage** even when brand-new, fully
crewed, fully armored, and with no expended ammunition. The damage level is computed by MegaMek
(`Entity.getDamageLevel`), and MekHQ surfaces the same value via `Unit.getDamageState ->
Entity.getDamageLevel(false)`, so the misreport appears in both applications.

## Root Cause
`Aero.isDmgLight()/isDmgModerate()/isDmgHeavy()` iterate `getTotalWeaponList()`, which **includes the
weapon-bay mounts themselves** (`Entity.addEquipment` adds bays to `totalWeaponList`). For each weapon
they call `Mounted.isCrippled()`:

```java
if ((type instanceof AmmoWeapon) || (type instanceof AmmoBayWeapon)) {
    if ((getLinked() == null) || (entity.getTotalAmmoOfType(getLinked().getType()) < 1)) {
        return true;
    }
}
```

A weapon **bay** never links its ammunition through `getLinked()` - a bay tracks ammo in its own
`bayAmmo` list (`getBayAmmo()`) and its weapons in `bayWeapons` (`getBayWeapons()`). So for any
ammo-using bay (`AmmoBayWeapon`: capital missile bays, AC bays, LRM bays, Laser-AMS bays, ...),
`getLinked()` is always `null` and the bay is flagged crippled even when fully stocked. Once enough of
`totalWeaponList` is ammo bays, the inoperable-weapon ratio crosses the 0.25 threshold and the unit
reports light damage. The individual member weapons inside the bays link to ammo correctly and are not
the problem - only the bay mounts are.

`Lee (Pocket CM)` is built almost entirely from Capital Missile (Barracuda) bays and Laser-AMS bays,
both `AmmoBayWeapon` subclasses, which is why it always shows damage.

## Changes
1. `megamek/src/megamek/common/equipment/Mounted.java` - `isCrippled()` is now bay-aware: when the
   mount is a `BayWeapon`, it is crippled only when **every** weapon it contains is crippled (assessed
   via `getBayWeapons()`), instead of consulting the always-null bay link. This also correctly reports a
   bay whose weapons are all destroyed/out-of-ammo as crippled. Added `import ...bayWeapons.BayWeapon`.

## Files Changed
- `megamek/src/megamek/common/equipment/Mounted.java` - bay-aware `isCrippled()`.
- `megamek/unittests/megamek/common/units/DamageLevelTest.java` - regression tests
  (`WeaponBayDamageLevelTests`): a fully-loaded DropShip LRM bay -> `DMG_NONE`; an all-out-of-ammo bay
  -> bay crippled and unit `DMG_CRIPPLED`.

## MekHQ
No change required. MekHQ's `Unit.getDamageState(Entity)` delegates to `Entity.getDamageLevel(false)`,
so it inherits the engine fix through the MegaMek dependency.

## Testing
- `DamageLevelTest` (incl. new bay tests) - pass.
- Regression sanity: `WeaponTypeTest`, `FireControlTest`, `BasicPathRankerTest`, `MekFileParserTest` -
  pass.

Fixes #8381
