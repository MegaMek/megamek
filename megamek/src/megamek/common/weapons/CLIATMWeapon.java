/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.weapons;

import megamek.common.AmmoType;
import megamek.common.EquipmentType;
import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

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
        this.availRating = new int[]{EquipmentType.RATING_X, EquipmentType.RATING_X,EquipmentType.RATING_F};
        this.introDate = 3070;
        
        setModes(new String[] { "", "Indirect" }); // iATMS can IDF
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
            WeaponAttackAction waa, IGame game, Server server) {
        
        // MML does different handlers here. I think I'll go with implementing different ammo in the Handler.
        return new CLIATMHandler(toHit, waa, game, server);
    }
}
