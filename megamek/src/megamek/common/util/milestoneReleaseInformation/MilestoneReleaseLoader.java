package megamek.common.util.milestoneReleaseInformation;

import static megamek.SuiteConstants.ALL_MILESTONE_RELEASES;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.SuiteConstants;
import megamek.Version;

/**
 * Loads and provides access to milestone release information from a YAML configuration file.
 *
 * <p>This class supports reading milestone release data on instantiation and provides utility methods for retrieving
 * the loaded releases, as well as determining the latest milestone version from a static preloaded source.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class MilestoneReleaseLoader {
    /** Path to the YAML file containing milestone release definitions. */
    private static final String RESOURCE_PATH = "mmconf/milestoneReleases.yml";

    /** List of loaded milestone releases. */
    private List<MilestoneData> milestoneReleases = Collections.emptyList();

    /**
     * Constructs a new loader and immediately loads milestone releases from the YAML resource.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public MilestoneReleaseLoader() {
        loadMilestoneReleases();
    }

    /**
     * Loads the milestone release information from the configured YAML file.
     *
     * <p>Populates the {@link #milestoneReleases} list by deserializing the YAML document into a
     * {@link MilestoneReleaseWrapper} object.</p>
     *
     * @throws RuntimeException if loading or parsing the YAML file fails
     * @author Illiani
     * @since 0.50.07
     */
    private void loadMilestoneReleases() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            MilestoneReleaseWrapper wrapper = mapper.readValue(
                  new File(RESOURCE_PATH),
                  MilestoneReleaseWrapper.class
            );
            if (wrapper != null && wrapper.getMilestone_releases() != null) {
                milestoneReleases = wrapper.getMilestone_releases();
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not load milestone releases from YAML", ex);
        }
    }

    /**
     * Returns the list of loaded milestone release data.
     *
     * @return a list of {@link MilestoneData} objects representing milestone releases
     *
     * @author Illiani
     * @since 0.50.07
     */
    public List<MilestoneData> getMilestoneReleases() {
        return milestoneReleases;
    }

    /**
     * Returns the latest milestone release version available from the statically defined set.
     *
     * <p>This method searches {@link SuiteConstants#ALL_MILESTONE_RELEASES} and finds the highest version value.</p>
     *
     * @return the latest {@link Version} among all milestone releases
     *
     * @throws IllegalStateException if no milestone releases are found
     * @author Illiani
     * @since 0.50.07
     */
    public static Version getLatestMilestoneReleaseVersion() {
        Version latestMilestone = null;
        for (MilestoneData data : ALL_MILESTONE_RELEASES) {
            if (latestMilestone == null) {
                latestMilestone = data.version();
                continue;
            }

            if (data.version().isHigherThan(latestMilestone)) {
                latestMilestone = data.version();
            }
        }

        if (latestMilestone == null) {
            throw new IllegalStateException("No milestone releases found");
        }

        return latestMilestone;
    }
}
