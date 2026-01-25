package megamek.client.ui.dialogs.randomArmy;

import megamek.client.generator.skillGenerators.AbstractSkillGenerator;
import megamek.common.loaders.MekSummary;
import megamek.common.options.GameOptions;
import megamek.common.units.Entity;

import java.util.List;

/**
 * This interface is implemented by Tabs that are shown in the Random Army Dialogs subclassed from
 * AbstractRandomArmyDialog. While the random army creators have wildly different inputs where a common interface
 * doesn't seem feasible, at least the Tabs that use them have some common behaviour. They can, if they want, use
 * provided GameOptions and a SkillGenerator and when generateMekSummaries() is used, they should provide a unit List as
 * a result. This interface is an attempt and should be upgraded where useful. Note that implementing classes should
 * extend JPanel (or JComponent) so they can be added directly to a JTabbedPane. If an internal JPanel field is added
 * instead, AbstractRandomArmyDialog cannot access the other functions from the currently selected Tab.
 */
interface RandomArmyTab {

    // TODO: return ForceDescriptor instead of List<Entity> as the superior class?
    // TODO: Give ForceDescriptor a semblance of an API?
    // TODO: allow other returns than Entity (how?) AlphaStrikeElement / SBF Unit are options
    // TODO: keep a map of tabs in AbstractRandomArmyDialog, thus not require extending JPanel?

    /**
     * Makes this Tab use its present parameters to roll up a force and returns this force. For forces with a structure
     * the Entities should have appropriate force Strings set. By default, this method forwards to
     * generateMekSummaries() and loads Entities from the results. Tabs that generate only MekSummaries do not need to
     * override this method. For Tabs that generate Entities, override this method to provide the result directly.
     *
     * @return A list of units generated
     */
    default List<Entity> generateUnits() {
        return generateMekSummaries().stream().map(MekSummary::loadEntity).toList();
    }

    /**
     * Makes this Tab use its present parameters to roll up a force and returns this force.
     *
     * @return A list of units generated
     */
    List<MekSummary> generateMekSummaries();

    void setGameOptions(GameOptions gameOptions);

    void setSkillGenerator(AbstractSkillGenerator skillGenerator);
}
