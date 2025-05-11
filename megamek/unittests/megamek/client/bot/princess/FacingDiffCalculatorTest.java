/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.bot.princess;

import megamek.common.BipedMek;
import megamek.common.Coords;
import megamek.common.Mek;
import megamek.common.MovePath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Luana Coppio
 */
class FacingDiffCalculatorTest {

    @Test
    void testFacingDiff() {
        Mek mek = mock(BipedMek.class);
        when(mek.getArmor(Mek.LOC_LARM)).thenReturn(5);
        when(mek.getArmor(Mek.LOC_LLEG)).thenReturn(5);
        when(mek.getArmor(Mek.LOC_LT)).thenReturn(10);
        when(mek.getArmor(Mek.LOC_RARM)).thenReturn(4);
        when(mek.getArmor(Mek.LOC_RLEG)).thenReturn(4);
        when(mek.getArmor(Mek.LOC_RT)).thenReturn(10);
        when(mek.isMek()).thenReturn(true);
        FacingDiffCalculator facingDiffCalculator = new FacingDiffCalculator();
        MovePath path = mock(MovePath.class);

        // If the final position is adjacent to the closest enemy position, then it must face the enemy.
        // position 0505 (new Coords(4, 4)) is adjacent to 0605 (new Coords(5, 4)), the direction from 4,4 to 5,4 is 2,
        // this case the final facing of the movepath is considering that it is facing towards north (0), so the
        // facing diff is 2, but because it has the right side with less armor, it has to bias its facing to show the
        // left side, so the facing diff increases in one more time, going up to 3.
        when(path.getFinalCoords()).thenReturn(new Coords(4, 4));
        when(path.getFinalFacing()).thenReturn(0);
        int facingDiff = facingDiffCalculator.getFacingDiff(mek, path, new Coords(10, 10),
              new Coords(10,10), new Coords(5,4));
        assertEquals(2, facingDiff);

        // If the final position is not adjacent to the closest enemy position, then it must face the enemy median
        // position 1111(new Coords(10, 10)), the direction from 0606 to 1111
        // is 2, this case the final facing of the movepath is considering that it is towards south east(2), so the
        // facing diff is 0, because it has the left side with less armor, it has to bias its facing to show the
        // right side, so the facing diff increases in one time time, going up to 1.
        when(path.getFinalCoords()).thenReturn(new Coords(5, 5));
        when(path.getFinalFacing()).thenReturn(2);

        when(mek.getArmor(Mek.LOC_RARM)).thenReturn(8);
        when(mek.getArmor(Mek.LOC_RLEG)).thenReturn(8);

        facingDiff = facingDiffCalculator.getFacingDiff(mek, path, new Coords(10, 10),
              new Coords(10,10), new Coords(2,2));
        assertEquals(0, facingDiff);


        // If there are no enemies, it should them face the secondary target position, in this case the board center,
        // from its position 0606 to the board center 1111 the facing is 2, it has no inherent bias towards any side
        // because its left and right armor are equal, therefore it will call a facing diff between 2 and its final
        // facing which is in this case also 0, resulting in an expected diff of 0
        when(path.getFinalCoords()).thenReturn(new Coords(5, 5));
        when(path.getFinalFacing()).thenReturn(2);

        when(mek.getArmor(Mek.LOC_RARM)).thenReturn(5);
        when(mek.getArmor(Mek.LOC_RLEG)).thenReturn(5);

        facingDiff = facingDiffCalculator.getFacingDiff(mek, path, new Coords(10, 10),
              null, null);
        assertEquals(0, facingDiff);
    }

}
