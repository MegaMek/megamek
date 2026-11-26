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
 * values fall back to what plain Java knows (thread count and total memory) rather than failing the caller.</p>
 *
 * <p>Each part is also available on its own ({@link #processor()}, {@link #graphicsCards()},
 * {@link #systemMemory()}) so the bug report form can fill one field per part. The log lines are built from the same
 * values, so the form and the log header always agree.</p>
 */
public final class HardwareDescription {
    private static final MMLogger LOGGER = MMLogger.create(HardwareDescription.class);

    private static final double BYTES_PER_MEGABYTE = 1024.0 * 1024.0;
    private static final double BYTES_PER_GIGABYTE = BYTES_PER_MEGABYTE * 1024.0;
    private static final double HERTZ_PER_GIGAHERTZ = 1_000_000_000.0;
    private static final String UNKNOWN = "unknown";

    private static final String PROCESSOR_PREFIX = "CPU: ";
    private static final String GRAPHICS_CARD_PREFIX = "GPU: ";
    private static final String MEMORY_PREFIX = "System RAM: ";

    private static volatile Hardware cachedHardware;

    /**
     * What was read from this machine, kept together so every caller sees the same lookup.
     *
     * @param processor     the CPU description, without its log prefix
     * @param graphicsCards one description per graphics card, without the log prefix; never empty
     * @param systemMemory  the system memory description, without its log prefix
     * @param lines         the log header lines built from the three values above
     */
    private record Hardware(String processor, List<String> graphicsCards, String systemMemory, List<String> lines) {}

    private HardwareDescription() {}

    /**
     * @return one line each for the CPU, every graphics card and the system memory, such as
     *       {@code CPU: AMD Ryzen 7 5800X 8-Core Processor (8 cores, 16 threads, 3.80 GHz)}
     */
    public static List<String> lines() {
        return hardware().lines();
    }

    /**
     * @return the CPU, such as {@code AMD Ryzen 7 5800X 8-Core Processor (8 cores, 16 threads, 3.80 GHz)}
     */
    public static String processor() {
        return hardware().processor();
    }

    /**
     * @return one entry per graphics card, such as {@code NVIDIA GeForce RTX 3070 (8 GB VRAM, driver 32.0.15.6094)};
     *       a single {@code unknown} entry when none could be read
     */
    public static List<String> graphicsCards() {
        return hardware().graphicsCards();
    }

    /**
     * @return the installed and free system memory, such as {@code 32 GB (18 GB free)}
     */
    public static String systemMemory() {
        return hardware().systemMemory();
    }

    private static Hardware hardware() {
        Hardware hardware = cachedHardware;
        if (hardware == null) {
            synchronized (HardwareDescription.class) {
                hardware = cachedHardware;
                if (hardware == null) {
                    hardware = readHardware();
                    cachedHardware = hardware;
                }
            }
        }
        return hardware;
    }

    private static Hardware readHardware() {
        long startNanos = System.nanoTime();
        Hardware hardware;
        try {
            hardware = readWithOshi();
        } catch (Throwable throwable) {
            // OSHI uses native calls through JNA; an unsupported platform throws an Error rather than an Exception.
            LOGGER.warn("[Hardware] OSHI could not read the hardware ({}); using plain Java values",
                  throwable.toString());
            hardware = readWithPlainJava();
        }
        LOGGER.info("[Hardware] Read {} hardware line(s) in {} ms", hardware.lines().size(),
              (System.nanoTime() - startNanos) / 1_000_000L);
        return hardware;
    }

    private static Hardware readWithOshi() {
        HardwareAbstractionLayer hardware = new SystemInfo().getHardware();

        CentralProcessor processor = hardware.getProcessor();
        String processorDescription = describeProcessor(processor.getProcessorIdentifier().getName().strip(),
              processor.getPhysicalPackageCount(),
              processor.getPhysicalProcessorCount(),
              processor.getLogicalProcessorCount(),
              processor.getMaxFreq());

        List<String> graphicsCardDescriptions = new ArrayList<>();
        for (GraphicsCard graphicsCard : hardware.getGraphicsCards()) {
            graphicsCardDescriptions.add(describeGraphicsCard(graphicsCard.getName(), graphicsCard.getVRam(),
                  graphicsCard.getVersionInfo()));
        }

        GlobalMemory memory = hardware.getMemory();
        return assemble(processorDescription, graphicsCardDescriptions,
              describeMemory(memory.getTotal(), memory.getAvailable()));
    }

    private static Hardware readWithPlainJava() {
        String processorDescription = "%s (%d threads)".formatted(UNKNOWN, Runtime.getRuntime().availableProcessors());
        String memoryDescription = UNKNOWN;
        if (ManagementFactory.getOperatingSystemMXBean()
              instanceof com.sun.management.OperatingSystemMXBean operatingSystem) {
            memoryDescription = describeMemory(operatingSystem.getTotalMemorySize(),
                  operatingSystem.getFreeMemorySize());
        }
        return assemble(processorDescription, List.of(), memoryDescription);
    }

    /**
     * @param processor     the CPU description
     * @param graphicsCards the graphics card descriptions, possibly empty
     * @param systemMemory  the memory description
     *
     * @return the values together with the log lines built from them
     */
    private static Hardware assemble(String processor, List<String> graphicsCards, String systemMemory) {
        List<String> cards = graphicsCards.isEmpty() ? List.of(UNKNOWN) : List.copyOf(graphicsCards);
        List<String> lines = new ArrayList<>();
        lines.add(PROCESSOR_PREFIX + processor);
        for (String card : cards) {
            lines.add(GRAPHICS_CARD_PREFIX + card);
        }
        lines.add(MEMORY_PREFIX + systemMemory);
        return new Hardware(processor, cards, systemMemory, List.copyOf(lines));
    }

    /**
     * @param name          the processor's marketing name
     * @param packageCount  the number of CPU sockets in use
     * @param coreCount     the physical cores across all sockets
     * @param threadCount   the logical processors across all sockets
     * @param maxFrequency  the rated clock speed in hertz, or zero or less when unknown
     *
     * @return the CPU description, without the log prefix
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
        return "%s (%s)".formatted(name.isBlank() ? UNKNOWN : name, details);
    }

    /**
     * @param name          the card's name, including its vendor
     * @param videoMemory   the card's memory in bytes, or zero or less when unknown
     * @param versionInfo   the driver version as OSHI reports it, often prefixed {@code DriverVersion=}
     *
     * @return the graphics card description, without the log prefix
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
        return details.isEmpty() ? cardName : "%s (%s)".formatted(cardName, String.join(", ", details));
    }

    /**
     * @param totalBytes     the installed memory
     * @param availableBytes the memory free right now
     *
     * @return the memory description, without the log prefix
     */
    static String describeMemory(long totalBytes, long availableBytes) {
        return String.format(Locale.ROOT, "%.0f GB (%.0f GB free)", totalBytes / BYTES_PER_GIGABYTE,
              availableBytes / BYTES_PER_GIGABYTE);
    }
}
