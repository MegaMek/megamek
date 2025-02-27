/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons.autocannons;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.options.IGameOptions;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.*;
import megamek.server.Server;
import megamek.server.totalwarfare.TWGameManager;

import java.io.Serial;

/**
 * N.B. This class is overridden for AC/2, AC/5, AC/10, AC/10, NOT ultras/LB/RAC. (No difference between ACWeapon and
 * AmmoWeapon except the ability to use special ammos (precision, AP, etc.))
 *
 * @author Andrew Hunter
 */
public abstract class ACWeapon extends AmmoWeapon {
    @Serial
    private static final long serialVersionUID = 1537808266032711407L;

    public ACWeapon() {
        super();
        flags = flags.or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_MEK_WEAPON).or(F_AERO_WEAPON).or(F_TANK_WEAPON);
        ammoType = AmmoType.T_AC;
        explosive = true; // when firing incendiary ammo
        atClass = CLASS_AC;
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game, TWGameManager gameManager) {
        AmmoType atype = (AmmoType) game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId()).getLinked().getType();

        Mounted<?> weapon = game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId());

        if (weapon.curMode().equals("Rapid")) {
            return new RapidfireACWeaponHandler(toHit, waa, game, gameManager);
        }
        if (atype.getMunitionType().contains(AmmoType.Munitions.M_ARMOR_PIERCING)) {
            return new ACAPHandler(toHit, waa, game, gameManager);
        }

        if (atype.getMunitionType().contains(AmmoType.Munitions.M_FLECHETTE)) {
            return new ACFlechetteHandler(toHit, waa, game, gameManager);
        }

        if (atype.getMunitionType().contains(AmmoType.Munitions.M_INCENDIARY_AC)) {
            return new ACIncendiaryHandler(toHit, waa, game, gameManager);
        }

        if (atype.getMunitionType().contains(AmmoType.Munitions.M_TRACER)) {
            return new ACTracerHandler(toHit, waa, game, gameManager);
        }

        if (atype.getMunitionType().contains(AmmoType.Munitions.M_FLAK)) {
            return new ACFlakHandler(toHit, waa, game, gameManager);
        }

        if (atype.getMunitionType().contains(AmmoType.Munitions.M_CASELESS)) {
            return new ACCaselessHandler(toHit, waa, game, gameManager);
        }

        return new ACWeaponHandler(toHit, waa, game, gameManager);

    }

    @Override
    public int getDamage() {
        int dmg = super.getDamage();
        if ((dmg != 5) && (dmg != 2)) {
            return dmg;
        }

        if (Server.getServerInstance() != null) {
            IOption increasedAc = Server.getServerInstance().getGame()
                    .getOptions().getOption(OptionsConstants.ADVCOMBAT_INCREASED_AC_DMG);
            if ((increasedAc != null) && increasedAc.booleanValue()) {
                dmg++;
            }
        }

        return dmg;
    }

    @Override
    public double getBattleForceDamage(int range) {
        double damage = 0;
        if (range <= getLongRange()) {
            damage = getRackSize();
            if ((range == AlphaStrikeElement.SHORT_RANGE) && (getMinimumRange() > 0)) {
                damage = adjustBattleForceDamageForMinRange(damage);
            }
        }
        return damage / 10.0;
    }

    @Override
    public int getBattleForceClass() {
        return BFCLASS_AC;
    }

    @Override
    public void adaptToGameOptions(IGameOptions gameOptions) {
        super.adaptToGameOptions(gameOptions);

        // Modes for allowing standard and light AC rapid fire
        IOption rapidAc = gameOptions.getOption(OptionsConstants.ADVCOMBAT_TACOPS_RAPID_AC);
        if ((rapidAc != null) && rapidAc.booleanValue()) {
            addMode("");
            addMode(Weapon.MODE_AC_RAPID);
        } else {
            removeMode("");
            removeMode(Weapon.MODE_AC_RAPID);
        }
    }
}
