package megamek.common.util.milestoneReleaseInformation;

import java.util.List;

/**
 * Wrapper class for holding a list of milestone release information.
 *
 * <p>This class is primarily used as a container for transferring or serializing/deserializing a collection of
 * {@link MilestoneData} entries, such as when loading or processing milestone release details.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class MilestoneReleaseWrapper {
    /** List of milestone release data entries. */
    private List<MilestoneData> milestone_releases;

    /**
     * Returns the list of milestone release data.
     *
     * @return a list containing {@link MilestoneData} objects representing each milestone release
     *
     * @author Illiani
     * @since 0.50.07
     */
    public List<MilestoneData> getMilestone_releases() {
        return milestone_releases;
    }

    /**
     * Sets the list of milestone release data.
     *
     * @param milestone_releases a list of {@link MilestoneData} objects to be set
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void setMilestone_releases(List<MilestoneData> milestone_releases) {
        this.milestone_releases = milestone_releases;
    }
}
