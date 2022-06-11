/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.Game;
import megamek.common.Mounted;
import megamek.common.TechAdvancement;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.missiles.MissileWeapon;
import megamek.server.GameManager;

/**
 * @author Sebastian Brocks, Modified by Greg
 */
public abstract class CLIATMWeapon extends MissileWeapon {

    /**
     * I think i can just assign 1? I don't think SVUIDs conflict with those from other classes
     */
    private static final long serialVersionUID = 1L;

    public CLIATMWeapon() {
        super();
        ammoType = AmmoType.T_IATM; // the Artemis Bonus is Tied to the ATM ammo, but i think i can ignore it in the handler. However, i think i still need a new ammo type since i dont know if the special ammo could get used with regular ATMs if i don#t change it. And i assume bad things will happen.
        atClass = CLASS_ATM; // Do I need to change this? Streak LRMs still use the CLASS_LRM flag... I think I can leave it.
        techAdvancement.setTechBase(TechAdvancement.TECH_BASE_CLAN);
        techAdvancement.setClanAdvancement(3049, 3070);
        techAdvancement.setTechRating(RATING_F);
        techAdvancement.setAvailability(new int[]{ RATING_X, RATING_X, RATING_F, RATING_E });
        
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     *      megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     *      megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, GameManager manager) {
        
        // MML does different handlers here. I think I'll go with implementing different ammo in the Handler.
        return new CLIATMHandler(toHit, waa, game, manager);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        if (range == AlphaStrikeElement.SHORT_RANGE) {
            return 0.3 * rackSize;
        } else if (range == AlphaStrikeElement.MEDIUM_RANGE) {
            return 0.2 * rackSize;
        } else {
            return 0.1 * rackSize;
        }
    }
    
    @Override
    public int getBattleForceClass() {
        return BFCLASS_IATM;
    }
    
    @Override
    public boolean hasAlphaStrikeIndirectFire() {
        return false;
    }

    @Override
    public boolean hasIndirectFire() {
        return true;
    }
    
    @Override
    public void adaptToGameOptions(GameOptions gOp) {
        super.adaptToGameOptions(gOp);

        // Indirect Fire
        if (gOp.booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)) {
            addMode("");
            addMode("Indirect");
        } else {
            removeMode("");
            removeMode("Indirect");
        }
    }

    @Override
    public String getSortingName() {
        return "ATM IMP " + ((rackSize < 10) ? "0" + rackSize : rackSize);
    }
}
