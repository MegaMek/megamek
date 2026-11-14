/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.Image;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class ImageKeyedCacheTest {

    /**
     * A picture whose hash code is fixed, so two of them always share it. Equality stays identity, as it is for the JDK
     * image classes: that mismatch is the collision under test.
     */
    private static final class SameHashImage extends BufferedImage {
        SameHashImage() {
            super(4, 4, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        public boolean equals(Object other) {
            return this == other;
        }

        @Override
        public int hashCode() {
            return 42;
        }
    }

    @Test
    void twoPicturesWithTheSameHashCodeKeepTheirOwnResults() {
        ImageKeyedCache<String> cache = new ImageKeyedCache<>();
        Image vedette = new SameHashImage();
        Image noDachi = new SameHashImage();
        assertEquals(vedette.hashCode(), noDachi.hashCode());

        cache.put(noDachi, "No-Dachi result");

        assertNull(cache.get(vedette), "A different picture must not get another picture's result");

        cache.put(vedette, "Vedette result");

        assertEquals("Vedette result", cache.get(vedette));
        assertEquals("No-Dachi result", cache.get(noDachi));
        assertEquals(2, cache.size());
    }

    @Test
    void samePictureReturnsTheStoredResult() {
        ImageKeyedCache<Image> cache = new ImageKeyedCache<>();
        Image source = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        Image scaled = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);

        cache.put(source, scaled);

        assertSame(scaled, cache.get(source));
    }

    @Test
    void nullsAreIgnored() {
        ImageKeyedCache<String> cache = new ImageKeyedCache<>();
        Image source = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);

        cache.put(null, "result");
        cache.put(source, null);

        assertEquals(0, cache.size());
        assertNull(cache.get(null));
        assertNull(cache.get(source));
    }

    @Test
    void clearRemovesEverything() {
        ImageKeyedCache<String> cache = new ImageKeyedCache<>();
        Image source = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        cache.put(source, "result");

        cache.clear();

        assertNull(cache.get(source));
        assertEquals(0, cache.size());
    }
}
