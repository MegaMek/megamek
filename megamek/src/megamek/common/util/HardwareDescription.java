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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import megamek.logging.MMLogger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;

/**
 * Describes the player's CPU, graphics cards and memory for the log header and bug reports.
 *
 * <p>Only an allowlist of OSHI calls is made: processor name, core counts and clock speed; each graphics card's name,
 * memory and driver version; total and free memory. Nothing that identifies the machine is read - no serial numbers,
 * processor ID, motherboard, network adapters, disks or user accounts.</p>
 *
 * <p>The lookup runs once and is cached; later calls are free. If OSHI cannot read the hardware on a platform, the
 * lines fall back to what plain Java knows (thread count and total memory) rather than failing the caller.</p>
 */
public final class HardwareDescription {
    private static final MMLogger LOGGER = MMLogger.create(HardwareDescription.class);

    private static final double BYTES_PER_MEGABYTE = 1024.0 * 1024.0;
    private static final double BYTES_PER_GIGABYTE = BYTES_PER_MEGABYTE * 1024.0;
    private static final double HERTZ_PER_GIGAHERTZ = 1_000_000_000.0;
    private static final String UNKNOWN = "unknown";

    private static volatile List<String> cachedLines;

    private HardwareDescription() {}

    /**
     * @return one line each for the CPU, every graphics card and the system memory, such as
     *       {@code CPU: AMD Ryzen 7 5800X 8-Core Processor (8 cores, 16 threads, 3.80 GHz)}
     */
    public static List<String> lines() {
        List<String> lines = cachedLines;
        if (lines == null) {
            synchronized (HardwareDescription.class) {
                lines = cachedLines;
                if (lines == null) {
                    lines = readLines();
                    cachedLines = lines;
                }
            }
        }
        return lines;
    }

    private static List<String> readLines() {
        long startNanos = System.nanoTime();
        List<String> lines;
        try {
            lines = readWithOshi();
        } catch (Throwable throwable) {
            // OSHI uses native calls through JNA; an unsupported platform throws an Error rather than an Exception.
            LOGGER.warn("[Hardware] OSHI could not read the hardware ({}); using plain Java values",
                  throwable.toString());
            lines = readWithPlainJava();
        }
        LOGGER.info("[Hardware] Read {} hardware line(s) in {} ms", lines.size(),
              (System.nanoTime() - startNanos) / 1_000_000L);
        return List.copyOf(lines);
    }

    private static List<String> readWithOshi() {
        HardwareAbstractionLayer hardware = new SystemInfo().getHardware();
        List<String> lines = new ArrayList<>();

        CentralProcessor processor = hardware.getProcessor();
        lines.add(describeProcessor(processor.getProcessorIdentifier().getName().strip(),
              processor.getPhysicalPackageCount(),
              processor.getPhysicalProcessorCount(),
              processor.getLogicalProcessorCount(),
              processor.getMaxFreq()));

        List<GraphicsCard> graphicsCards = hardware.getGraphicsCards();
        if (graphicsCards.isEmpty()) {
            lines.add("GPU: " + UNKNOWN);
        }
        for (GraphicsCard graphicsCard : graphicsCards) {
            lines.add(describeGraphicsCard(graphicsCard.getName(), graphicsCard.getVRam(),
                  graphicsCard.getVersionInfo()));
        }

        GlobalMemory memory = hardware.getMemory();
        lines.add(describeMemory(memory.getTotal(), memory.getAvailable()));
        return lines;
    }

    private static List<String> readWithPlainJava() {
        List<String> lines = new ArrayList<>();
        lines.add("CPU: %s (%d threads)".formatted(UNKNOWN, Runtime.getRuntime().availableProcessors()));
        lines.add("GPU: " + UNKNOWN);
        if (ManagementFactory.getOperatingSystemMXBean()
              instanceof com.sun.management.OperatingSystemMXBean operatingSystem) {
            lines.add(describeMemory(operatingSystem.getTotalMemorySize(), operatingSystem.getFreeMemorySize()));
        } else {
            lines.add("System RAM: " + UNKNOWN);
        }
        return lines;
    }

    /**
     * @param name          the processor's marketing name
     * @param packageCount  the number of CPU sockets in use
     * @param coreCount     the physical cores across all sockets
     * @param threadCount   the logical processors across all sockets
     * @param maxFrequency  the rated clock speed in hertz, or zero or less when unknown
     *
     * @return the CPU line
     */
    static String describeProcessor(String name, int packageCount, int coreCount, int threadCount,
          long maxFrequency) {
        StringBuilder details = new StringBuilder();
        if (packageCount > 1) {
            details.append(packageCount).append(" CPUs, ");
        }
        details.append(coreCount).append(" cores, ").append(threadCount).append(" threads");
        if (maxFrequency > 0) {
            details.append(", ").append(String.format(Locale.ROOT, "%.2f GHz", maxFrequency / HERTZ_PER_GIGAHERTZ));
        }
        return "CPU: %s (%s)".formatted(name.isBlank() ? UNKNOWN : name, details);
    }

    /**
     * @param name          the card's name, including its vendor
     * @param videoMemory   the card's memory in bytes, or zero or less when unknown
     * @param versionInfo   the driver version as OSHI reports it, often prefixed {@code DriverVersion=}
     *
     * @return the GPU line
     */
    static String describeGraphicsCard(String name, long videoMemory, String versionInfo) {
        List<String> details = new ArrayList<>();
        if (videoMemory >= BYTES_PER_GIGABYTE) {
            details.add(String.format(Locale.ROOT, "%.0f GB VRAM", videoMemory / BYTES_PER_GIGABYTE));
        } else if (videoMemory > 0) {
            // Integrated graphics reserve a small slice, such as 128 MB, which would round to "0 GB".
            details.add(String.format(Locale.ROOT, "%.0f MB VRAM", videoMemory / BYTES_PER_MEGABYTE));
        }
        String driverVersion = (versionInfo == null) ? "" : versionInfo.replace("DriverVersion=", "").strip();
        if (!driverVersion.isEmpty() && !driverVersion.equalsIgnoreCase(UNKNOWN)) {
            details.add("driver " + driverVersion);
        }
        String cardName = ((name == null) || name.isBlank()) ? UNKNOWN : name.strip();
        return details.isEmpty() ? "GPU: " + cardName : "GPU: %s (%s)".formatted(cardName, String.join(", ", details));
    }

    /**
     * @param totalBytes     the installed memory
     * @param availableBytes the memory free right now
     *
     * @return the memory line
     */
    static String describeMemory(long totalBytes, long availableBytes) {
        return String.format(Locale.ROOT, "System RAM: %.0f GB (%.0f GB free)", totalBytes / BYTES_PER_GIGABYTE,
              availableBytes / BYTES_PER_GIGABYTE);
    }
}
