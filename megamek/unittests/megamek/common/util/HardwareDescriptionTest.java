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

package megamek.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests the hardware lines written to the log header and bug reports.
 */
class HardwareDescriptionTest {

    private static final long GIGABYTE = 1024L * 1024L * 1024L;

    @Test
    void processorLineShowsCoresThreadsAndSpeed() {
        assertEquals("CPU: AMD Ryzen 7 5800X 8-Core Processor (8 cores, 16 threads, 3.80 GHz)",
              HardwareDescription.describeProcessor("AMD Ryzen 7 5800X 8-Core Processor", 1, 8, 16, 3_800_000_000L));
    }

    @Test
    void processorLineNamesSocketsOnlyWhenThereIsMoreThanOne() {
        assertEquals("CPU: Xeon (2 CPUs, 32 cores, 64 threads)",
              HardwareDescription.describeProcessor("Xeon", 2, 32, 64, 0L));
    }

    @Test
    void graphicsCardLineShowsMemoryAndDriver() {
        assertEquals("GPU: NVIDIA GeForce RTX 3070 (8 GB VRAM, driver 32.0.15.6094)",
              HardwareDescription.describeGraphicsCard("NVIDIA GeForce RTX 3070", 8 * GIGABYTE,
                    "DriverVersion=32.0.15.6094"));
    }

    @Test
    void integratedGraphicsMemoryIsShownInMegabytes() {
        assertEquals("GPU: Intel(R) UHD Graphics (128 MB VRAM, driver 31.0.101.5187)",
              HardwareDescription.describeGraphicsCard("Intel(R) UHD Graphics", 128L * 1024L * 1024L,
                    "DriverVersion=31.0.101.5187"));
    }

    @Test
    void graphicsCardLineDropsUnknownDetails() {
        assertEquals("GPU: Microsoft Basic Display Adapter",
              HardwareDescription.describeGraphicsCard("Microsoft Basic Display Adapter", 0L, "unknown"));
    }

    @Test
    void memoryLineShowsTotalAndFree() {
        assertEquals("System RAM: 32 GB (18 GB free)", HardwareDescription.describeMemory(32 * GIGABYTE,
              18 * GIGABYTE));
    }

    @Test
    void readsThisMachineOnceAndCachesTheLines() {
        List<String> lines = HardwareDescription.lines();

        assertTrue(lines.size() >= 3, "expected CPU, at least one GPU and RAM lines: " + lines);
        assertTrue(lines.getFirst().startsWith("CPU: "), lines.toString());
        assertTrue(lines.getLast().startsWith("System RAM: "), lines.toString());
        assertSame(lines, HardwareDescription.lines(), "the second call should reuse the first result");
    }
}
