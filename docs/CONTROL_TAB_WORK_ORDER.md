# Work Order — Opt-in "Control" Unit-Display Layout (MegaMek)

**Audience:** a Claude Code CLI session working in the `megamek/megamek` repo (IntelliJ IDEA). Everything here is self-contained; you do not need any prior conversation. Line numbers are against current `main`/`claude/ui-design-planning-6xhkdh` (HEAD ≈ commit `3996679b`, includes the GM 2.0 tools). **Re-read a file before editing — line numbers shift as you insert.**

## Goal

Add an **opt-in** alternative unit-display layout with **three tabs** (General · Weapon · **Control**) instead of the current six (General · Pilot · Armor · Weapon · Systems · Extras). The new **Control** tab is a paperdoll-driven control panel that folds Armor + Systems + Extras + Pilot into one view, modeled on the GM damage dialog's paperdoll + per-location layout, but wired to the **live control** widgets (equipment mode combo, ammo dump, sensors, heat sinks, hidden-unit activation) — not damage spinners.

## Hard constraints

1. **Opt-in via a setting**, default **OFF** (classic 6-tab). Classic path must be byte-for-byte behavior-preserving.
2. New behavior lives in **its own classes** (new package). Reuse existing panels; do not gut them.
3. Refactoring the GM dialog (`DamageEditorDiagram`) into an **abstract base** shared with the new panel is acceptable and preferred (Option A below). A no-touch fallback is Option B.
4. **No MekHQ dependency.** (MekHQ depends on MegaMek, not vice-versa.) Any styling ideas are copied/adapted into MegaMek.
5. The setting may take effect on the **next open** of the unit display (no live re-layout required for v1).

## Facts already verified (rely on these)

- `SystemPanel.selectLocation(int loc)` **already exists** and is used by `UnitDisplayPanel.showSpecificSystem` (`UnitDisplayPanel.java:586`). The paperdoll→systems location routing already works in classic mode.
- `UnitDisplayPanel.updateDisplay()` (`:505`) fans out to `mPan/pPan/aPan/wPan/sPan/ePan` by **field reference**, independent of parent — so panels reused inside a new container still refresh.
- `ArmorPanel(@Nullable Game, @Nullable LocationSelectListener)` (`ArmorPanel.java:118`) renders **every** unit type from its `*MapSet`, includes the heat scale, calls `locationSelectListener.locationSelected(int)` on single click, and has `setFitToWindow(boolean)`, `setDisplayScale(double)`, `displayMek(Entity)`, `setCriticalLocations(...)`.
- `DamageEditorDiagram` (`dialogs/unitEditor/DamageEditorDiagram.java`) is `JSplitPane implements LocationSelectListener`: left = `new ArmorPanel(game, this)` in a scrollpane; right = location combo + `CardLayout` of per-location panels + a general panel; has `enlargeToFillDialog()`, `refresh()`, `selectsOnSingleClick()==true`. It is the template.
- Phase displays only ever call `showPanel(SUMMARY)` / `showPanel(WEAPONS)` — both survive. No other tab is referenced from gameplay code.
- `MekPanelTabStrip` (`client/ui/widget/MekPanelTabStrip.java`) is an image-based `PicMap` hardcoded to 6 tabs.

---

## Task checklist (single PR)

- [ ] 1. New `GUIPreferences` boolean setting `UNIT_DISPLAY_CONTROL_LAYOUT` (default false).
- [ ] 2. `CommonSettingsDialog` checkbox to toggle it + `messages.properties` key.
- [ ] 3. New `AbstractLocationDiagram` base (extract shared scaffold).
- [ ] 4. Refactor `DamageEditorDiagram` to extend it (Option A) — behavior-preserving.
- [ ] 5. New `UnitDisplayStyle` themed-Swing helper (small, optional polish).
- [ ] 6. New `ControlPanel` composite (the Control tab body).
- [ ] 7. Make `MekPanelTabStrip` tab-set data-driven; add `CONTROL` + `controlTabs()`.
- [ ] 8. Wire `UnitDisplayPanel` to build the control layout when the setting is on.
- [ ] 9. `KeyCommandBind` F-key for Control (optional; can reuse existing binds).
- [ ] 10. Compile (`./gradlew :megamek:compileJava`), fix, then run a smoke test.

---

## 1. `GUIPreferences.java` — new setting

File: `megamek/src/megamek/client/ui/clientGUI/GUIPreferences.java`. Mirror the existing `UNIT_DISPLAY_START_TABBED` idiom (store-backed boolean; **default lives in the constructor**, not the getter). Apply bottom-up.

- **Constant** — after line 213 (`public static final String UNIT_DISPLAY_START_TABBED = "UnitDisplayStartTabbed";`):
  ```java
  public static final String UNIT_DISPLAY_CONTROL_LAYOUT = "UnitDisplayControlLayout";
  ```
- **Default** — in the constructor's `UNIT_DISPLAY_*` default block, after line 717 (`store.setDefault(UNIT_DISPLAY_START_TABBED, true);`):
  ```java
  store.setDefault(UNIT_DISPLAY_CONTROL_LAYOUT, false);
  ```
- **Getter** — after `getUnitDisplayStartTabbed()` (ends ~line 1079):
  ```java
  public boolean getUnitDisplayControlLayout() {
      return store.getBoolean(UNIT_DISPLAY_CONTROL_LAYOUT);
  }
  ```
- **Setter** — after `setUnitDisplayStartTabbed(...)` (ends ~line 1992):
  ```java
  public void setUnitDisplayControlLayout(boolean state) {
      store.setValue(UNIT_DISPLAY_CONTROL_LAYOUT, state);
  }
  ```

## 2. `CommonSettingsDialog.java` + messages

File: `megamek/src/megamek/client/ui/dialogs/buttonDialogs/CommonSettingsDialog.java`. Mirror the `showPilotPortraitTT` checkbox (all four steps).

- **Field** — after line 229 (the `showPilotPortraitTT` field):
  ```java
  private final JCheckBox unitDisplayControlLayout = new JCheckBox(Messages.getString(
        "CommonSettingsDialog.unitDisplayControlLayout"));
  ```
- **Layout add** — in `getUnitDisplayPanel()`, after `comps.add(checkboxEntry(showPilotPortraitTT, null));` (~line 1240):
  ```java
  comps.add(checkboxEntry(unitDisplayControlLayout,
        Messages.getString("CommonSettingsDialog.unitDisplayControlLayout.tooltip")));
  ```
- **Load** — in `setVisible(boolean)`, after `showPilotPortraitTT.setSelected(...)` (~line 2289):
  ```java
  unitDisplayControlLayout.setSelected(GUIP.getUnitDisplayControlLayout());
  ```
- **Save** — in `okAction()`, after `GUIP.setShowPilotPortraitTT(...)` (~line 2718):
  ```java
  GUIP.setUnitDisplayControlLayout(unitDisplayControlLayout.isSelected());
  ```
- **Messages** — add to `megamek/resources/megamek/client/messages.properties` (near the other `CommonSettingsDialog.*` keys):
  ```properties
  CommonSettingsDialog.unitDisplayControlLayout=Use experimental Control layout (3 tabs)
  CommonSettingsDialog.unitDisplayControlLayout.tooltip=Collapses Armor, Systems, Extras and Pilot into a single paperdoll-driven Control tab. Takes effect the next time the unit display is opened.
  ```

## 3. New: `AbstractLocationDiagram.java`

Create `megamek/src/megamek/client/ui/dialogs/unitEditor/AbstractLocationDiagram.java`. This is the shared scaffold pulled out of `DamageEditorDiagram`: a horizontal split with a clickable `ArmorPanel` paperdoll on the left and a subclass-supplied component on the right, plus screen-aware paperdoll enlargement. Keep it dependency-light.

```java
package megamek.client.ui.dialogs.unitEditor;

import java.awt.Component;
import java.awt.Toolkit;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import megamek.client.ui.dialogs.unitDisplay.ArmorPanel;
import megamek.client.ui.widget.picmap.LocationSelectListener;
import megamek.common.units.Entity;

/**
 * Shared scaffold for a clickable unit paperdoll beside a per-location panel: the GM damage editor
 * ({@link DamageEditorDiagram}) and the in-play Control tab both extend this. Owns the {@link ArmorPanel}
 * paperdoll, the split pane, and the paperdoll-enlargement logic; subclasses supply the right-hand component
 * and decide what selecting a location does.
 */
public abstract class AbstractLocationDiagram extends JSplitPane implements LocationSelectListener {

    /** How much the paperdoll is enlarged even when there is no panel height to fill. */
    private static final double MIN_PAPERDOLL_SCALE = 1.6;
    /** How far the paperdoll may be enlarged before it turns blocky. */
    private static final double MAX_PAPERDOLL_SCALE = 2.5;
    /** How much of the screen height the enlarged paperdoll may take. */
    private static final double MAX_SCREEN_FRACTION = 0.7;

    protected final Entity entity;
    protected final ArmorPanel paperdoll;

    protected AbstractLocationDiagram(Entity entity) {
        super(JSplitPane.HORIZONTAL_SPLIT);
        this.entity = entity;
        this.paperdoll = new ArmorPanel(entity.getGame(), this);
    }

    /** Subclasses call this once, after building their right component, to finish the split. */
    protected void assemble(Component rightComponent, String splitPaneName) {
        setLeftComponent(new JScrollPane(paperdoll));
        setRightComponent(rightComponent);
        setName(splitPaneName);
        setResizeWeight(0.0);
        setOneTouchExpandable(true);
    }

    /** Enlarge the paperdoll to fill the right component's height, bounded by scale and screen. */
    public void enlargeToFillDialog() {
        int drawnHeight = paperdoll.getPreferredSize().height;
        if (drawnHeight <= 0) {
            return;
        }
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        int rightHeight = (getRightComponent() == null) ? 0 : getRightComponent().getPreferredSize().height;
        double panelScale = (double) rightHeight / drawnHeight;
        double screenScale = (screenHeight * MAX_SCREEN_FRACTION) / drawnHeight;
        double scale = Math.max(panelScale, MIN_PAPERDOLL_SCALE);
        scale = Math.min(scale, Math.min(MAX_PAPERDOLL_SCALE, screenScale));
        if (scale > 1.0) {
            paperdoll.setDisplayScale(scale);
        }
    }

    /** The diagram exists to pick locations, so one click selects. */
    @Override
    public boolean selectsOnSingleClick() {
        return true;
    }

    /** Paperdoll click → subclass behavior. */
    @Override
    public void locationSelected(int location) {
        showLocation(location);
    }

    /** React to a location being chosen (card swap, systems selection, etc.). */
    protected abstract void showLocation(int location);

    /** Redraw after a pack; subclasses refresh their paperdoll/values here. */
    public abstract void refresh();
}
```

> Verify `Entity.getGame()` is non-null in the contexts you construct these (it is for in-play units and for the GM dialog's entity). If a caller can pass a gameless entity, pass the `Game` explicitly through the constructor instead.

## 4. Refactor `DamageEditorDiagram` to extend the base (Option A, recommended)

File: `dialogs/unitEditor/DamageEditorDiagram.java`. **Behavior must not change.** Steps:

1. Change the declaration to `public class DamageEditorDiagram extends AbstractLocationDiagram`.
2. **Delete** the now-inherited members: the `entity` field, the `paperdoll` field, the `MIN/MAX_PAPERDOLL_SCALE`/`MAX_SCREEN_FRACTION` constants, `enlargeToFillDialog()`, `selectsOnSingleClick()`, and the `locationSelected(int)` method (its body moves into `showLocation`).
3. In the constructor: call `super(entity)` first (the base builds `paperdoll`). Keep building `panCards`, `comboLocation`, `panPanels`, `panRight` as today. Replace the manual `setLeftComponent/ setRightComponent/ setName/ setResizeWeight/ setOneTouchExpandable` block with `assemble(panRight, SPLIT_PANE_NAME);`. Keep `wireDamageColoring()`.
4. Implement `@Override protected void showLocation(int location)` with the old `locationSelected` body:
   ```java
   if ((location >= 0) && (location < controls.locationPanels.length)
         && (controls.locationPanels[location] != null)) {
       comboLocation.setSelectedItem(new LocationChoice(location, entity.getLocationName(location)));
   }
   ```
5. Keep the existing `public void refresh() { refreshDamageDisplay(); }` — it now `@Override`s the abstract method (add `@Override`).
6. Everything else (damage coloring, `refreshPaperdoll`, `LocationChoice`) stays. `paperdoll` and `entity` now come from the base — remove the local re-declarations only.

**Verification for step 4:** open the GM damage dialog (`UnitEditorDialog`) in-game and confirm the paperdoll, location combo, spinners, coloring and Okay/Apply behave exactly as before, for a Mek and a vehicle.

> **Option B (fallback if the refactor feels risky under time pressure):** leave `DamageEditorDiagram` untouched and have `ControlPanel` (§6) contain its own copy of the split/paperdoll/enlarge logic instead of extending `AbstractLocationDiagram`. Ship the base later. Prefer Option A.

## 5. New: `UnitDisplayStyle.java` (small, optional polish)

Create `megamek/src/megamek/client/ui/dialogs/unitDisplay/control/UnitDisplayStyle.java` — generic themed-Swing helpers so the Control tab's sections read as one system. Campaign-free; adapted in spirit from MekHQ's `ImmersiveDialogStyle` (do not import anything from MekHQ). Keep it tiny:

```java
package megamek.client.ui.dialogs.unitDisplay.control;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;

/** Lightweight, theme-neutral section styling for the Control tab. */
public final class UnitDisplayStyle {
    private UnitDisplayStyle() { }

    /** A titled, padded section border. */
    public static Border sectionBorder(String title) {
        return BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder(title),
              BorderFactory.createEmptyBorder(4, 6, 4, 6));
    }

    /** Standard vertical spacing between stacked sections. */
    public static Border sectionSpacing() {
        return BorderFactory.createEmptyBorder(6, 0, 6, 0);
    }

    public static void asSection(JComponent c, String title) {
        c.setBorder(sectionBorder(title));
        c.setOpaque(false);
    }

    /** Accent color from the current L&F, falling back to a neutral cyan. */
    public static Color accent() {
        Color c = javax.swing.UIManager.getColor("Component.focusColor");
        return (c != null) ? c : new Color(0x57, 0xB6, 0xD6);
    }
}
```

Use it where convenient in `ControlPanel`; it is not load-bearing.

## 6. New: `ControlPanel.java`

Create `megamek/src/megamek/client/ui/dialogs/unitDisplay/control/ControlPanel.java`. It **owns a fresh paperdoll** (so clicks route through the base to this panel) and **reuses** the unit display's existing `SystemPanel`/`PilotPanel`/`ExtraPanel` scroll panes (hosted only here in control layout — single parent). The cockpit location shows the Pilot card; every other location shows the Systems card focused on that location; `ExtraPanel` is pinned at the bottom.

```java
package megamek.client.ui.dialogs.unitDisplay.control;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.client.ui.dialogs.unitDisplay.ExtraPanel;
import megamek.client.ui.dialogs.unitDisplay.PilotPanel;
import megamek.client.ui.dialogs.unitDisplay.SystemPanel;
import megamek.client.ui.dialogs.unitEditor.AbstractLocationDiagram;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;

/**
 * The body of the experimental "Control" tab: a clickable paperdoll driving a per-location control panel.
 * Reuses the unit display's SystemPanel (equipment mode + ammo dump), PilotPanel (shown on the cockpit
 * location) and ExtraPanel (pinned global controls). Selecting a location on the paperdoll focuses the
 * SystemPanel there; selecting the cockpit shows the pilot.
 */
public class ControlPanel extends AbstractLocationDiagram {

    private static final String CARD_SYSTEMS = "systems";
    private static final String CARD_PILOT = "pilot";

    private final SystemPanel systemPanel;
    private final CardLayout cards = new CardLayout();
    private final JPanel cardHost = new JPanel();

    public ControlPanel(Entity entity, SystemPanel systemPanel,
          JScrollPane systemsScroll, JScrollPane pilotScroll, JScrollPane extrasScroll) {
        super(entity);
        this.systemPanel = systemPanel;

        cardHost.setLayout(cards);
        cardHost.setOpaque(false);
        cardHost.add(systemsScroll, CARD_SYSTEMS);
        cardHost.add(pilotScroll, CARD_PILOT);

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(cardHost, BorderLayout.CENTER);
        right.add(extrasScroll, BorderLayout.SOUTH);

        assemble(right, "unitDisplayControlSplit");

        paperdoll.setToolTipText(megamek.client.ui.Messages.getString("UnitEditorDialog.paperdoll.tooltip"));
        cards.show(cardHost, CARD_SYSTEMS);
    }

    /** Refresh the paperdoll for the current entity (values come from the reused panels' own displayMek). */
    public void displayMek(Entity en) {
        if (paperdoll.isDisplayable()) {
            paperdoll.displayMek(en);
        }
    }

    @Override
    public void refresh() {
        if (paperdoll.isDisplayable()) {
            paperdoll.displayMek(entity);
        }
    }

    @Override
    protected void showLocation(int location) {
        systemPanel.selectLocation(location);
        cards.show(cardHost, (location == cockpitLocation(entity)) ? CARD_PILOT : CARD_SYSTEMS);
    }

    /** The location that houses the crew, or -1 for units with no cockpit location. */
    private static int cockpitLocation(Entity entity) {
        if (entity instanceof Mek) {
            return Mek.LOC_HEAD;
        } else if (entity instanceof ProtoMek) {
            return ProtoMek.LOC_HEAD;
        }
        return -1;
    }
}
```

Notes for the implementer:
- Verify constant names `Mek.LOC_HEAD` and `ProtoMek.LOC_HEAD` (adjust if different). For headless types `cockpitLocation` returns -1, so the Systems card always shows; the Pilot panel is still reachable — optionally append a "Crew" entry to `SystemPanel`'s location list later, out of scope for v1.
- The reused scroll panes (`systemsScroll`, `pilotScroll`, `extrasScroll`) are the existing `sPanScroll`/`pPanScroll`/`ePanScroll` from `UnitDisplayPanel`. They are added here **only** in control layout, so there is no dual-parent conflict (control vs classic is chosen once at construction).
- After the enclosing dialog first packs, call `enlargeToFillDialog()` once (see §8), mirroring `UnitEditorDialog`.

## 7. `MekPanelTabStrip.java` — make the tab set data-driven

File: `client/ui/widget/MekPanelTabStrip.java`. Convert the hardcoded 6-tab arrays to a configurable list so the same class renders both the classic 6-tab strip (default) and a 3-tab control strip. Keep the existing public `String` keys; **add** `CONTROL`. Full replacement of the class body is fine; preserve `drawIdleImage` corner logic verbatim (it already works for any tab count). Key changes:

- Add key + keep old ones:
  ```java
  public static final String CONTROL = "control";
  ```
- Replace `protected static final int NUM_TABS = 6;` and the `static final Image[]` arrays with **instance** arrays sized to the config.
- Add a tab spec and factories:
  ```java
  /** One tab: the panel key it selects and how to fetch its idle/active skin images. */
  public record TabSpec(String key,
        java.util.function.Function<UnitDisplaySkinSpecification, String> idle,
        java.util.function.Function<UnitDisplaySkinSpecification, String> active) { }

  public static java.util.List<TabSpec> classicTabs() {
      return java.util.List.of(
        new TabSpec(SUMMARY,  UnitDisplaySkinSpecification::getGeneralTabIdle,  UnitDisplaySkinSpecification::getGeneralTabActive),
        new TabSpec(PILOT,    UnitDisplaySkinSpecification::getPilotTabIdle,    UnitDisplaySkinSpecification::getPilotTabActive),
        new TabSpec(ARMOR,    UnitDisplaySkinSpecification::getArmorTabIdle,    UnitDisplaySkinSpecification::getArmorTabActive),
        new TabSpec(WEAPONS,  UnitDisplaySkinSpecification::getWeaponsTabIdle,  UnitDisplaySkinSpecification::getWeaponsTabActive),
        new TabSpec(SYSTEMS,  UnitDisplaySkinSpecification::getSystemsTabIdle,  UnitDisplaySkinSpecification::getSystemsTabActive),
        new TabSpec(EXTRAS,   UnitDisplaySkinSpecification::getExtrasTabIdle,   UnitDisplaySkinSpecification::getExtraTabActive));
  }

  /** 3-tab layout; Control reuses the Systems tab art as interim (no new asset needed). */
  public static java.util.List<TabSpec> controlTabs() {
      return java.util.List.of(
        new TabSpec(SUMMARY, UnitDisplaySkinSpecification::getGeneralTabIdle, UnitDisplaySkinSpecification::getGeneralTabActive),
        new TabSpec(WEAPONS, UnitDisplaySkinSpecification::getWeaponsTabIdle, UnitDisplaySkinSpecification::getWeaponsTabActive),
        new TabSpec(CONTROL, UnitDisplaySkinSpecification::getSystemsTabIdle, UnitDisplaySkinSpecification::getSystemsTabActive));
  }
  ```
- Constructors:
  ```java
  private final java.util.List<TabSpec> tabSpecs;
  public MekPanelTabStrip(UnitDisplayPanel md) { this(md, classicTabs()); }
  public MekPanelTabStrip(UnitDisplayPanel md, java.util.List<TabSpec> tabSpecs) {
      super(); this.md = md; this.tabSpecs = tabSpecs;
      this.idleImage = new Image[tabSpecs.size()];
      this.activeImage = new Image[tabSpecs.size()];
      this.tabs = new PMPicPolygonalArea[tabSpecs.size()];
  }
  public int indexOf(String key) {
      for (int i = 0; i < tabSpecs.size(); i++) { if (tabSpecs.get(i).key().equals(key)) return i; }
      return 0;
  }
  ```
- `setImages()`: replace the twelve hardcoded assignments with a loop:
  ```java
  UnitDisplaySkinSpecification udSpec = SkinXMLHandler.getUnitDisplaySkin();
  for (int i = 0; i < tabSpecs.size(); i++) {
      idleImage[i]   = tk.getImage(new MegaMekFile(Configuration.widgetsDir(), tabSpecs.get(i).idle().apply(udSpec)).toString());
      activeImage[i] = tk.getImage(new MegaMekFile(Configuration.widgetsDir(), tabSpecs.get(i).active().apply(udSpec)).toString());
  }
  // corners unchanged; MediaTracker loop over tabSpecs.size()
  ```
- `setListeners()`: loop:
  ```java
  for (int i = 0; i < tabSpecs.size(); i++) {
      final String key = tabSpecs.get(i).key();
      tabs[i].addActionListener(e -> {
          if (java.util.Objects.equals(e.getActionCommand(), PMHotArea.MOUSE_DOWN)) { md.showPanel(key); }
      });
  }
  ```
- `setTab(int i)`: clamp with `if (i >= tabSpecs.size()) i = tabSpecs.size() - 1;` (replaces the hardcoded `> 5`).
- `redrawImages()`/`setAreas()`/`drawIdleImage(int)`: change `NUM_TABS`/`idleImage.length` references to `tabSpecs.size()`; keep the drawing/corner logic identical.
- Keep `SUMMARY_INDEX … EXTRAS_INDEX` constants for classic callers, but new code should prefer `indexOf(key)`.

## 8. `UnitDisplayPanel.java` — build the control layout when the setting is on

File: `dialogs/unitDisplay/UnitDisplayPanel.java`. Additive; classic paths unchanged.

1. **Field**: add near the other panel fields (`:98–106`):
   ```java
   private ControlPanel controlPanel; // non-null only in control layout
   private final boolean controlLayout = GUI_PREFERENCES.getUnitDisplayControlLayout();
   ```
   Add import `megamek.client.ui.dialogs.unitDisplay.control.ControlPanel;`.
2. **Tab strip config**: change the tab strip creation (`:174`) to pick the config:
   ```java
   tabStrip = new MekPanelTabStrip(this,
         controlLayout ? MekPanelTabStrip.controlTabs() : MekPanelTabStrip.classicTabs());
   ```
3. **Choose layout** at the end of the constructor (`:324–328`):
   ```java
   if (controlLayout) {
       setDisplayControl();
   } else if (GUI_PREFERENCES.getUnitDisplayStartTabbed()) {
       setDisplayTabbed();
   } else {
       setDisplayNonTabbed();
   }
   ```
   Also hide the tabbed/non-tabbed switch button in control layout (v1 doesn't support non-tabbed there): after the button is created, `butSwitchView.setVisible(!controlLayout);`.
4. **New method** `setDisplayControl()` (model on `setDisplayTabbed`, `:334`):
   ```java
   private void setDisplayControl() {
       tabStrip.setVisible(true);
       displayP.removeAll();
       panA1.removeAll(); panA2.removeAll(); panB1.removeAll();
       panB2.removeAll(); panC1.removeAll(); panC2.removeAll();

       // Control tab reuses the Systems/Pilot/Extras scroll panes; they live only inside ControlPanel here.
       controlPanel = new ControlPanel(currentlyDisplayingOrPlaceholder(),
             sPan, sPanScroll, pPanScroll, ePanScroll);

       displayP.add(MekPanelTabStrip.SUMMARY, mPanScroll);
       displayP.add(MekPanelTabStrip.WEAPONS, wPanScroll);
       displayP.add(MekPanelTabStrip.CONTROL, controlPanel);

       tabStrip.setTab(tabStrip.indexOf(MekPanelTabStrip.SUMMARY));
       displayP.revalidate();
       displayP.repaint();
   }
   ```
   **Construction-order problem:** `ControlPanel`'s base builds `new ArmorPanel(entity.getGame(), this)`, which needs a non-null `Entity`. At construction time `currentlyDisplaying` may be null. Two options:
   - (Recommended) **Defer** creating `controlPanel` until the first `updateDisplay()` has a real entity. In `setDisplayControl()`, add only `mPanScroll`/`wPanScroll` and a lightweight placeholder card for `CONTROL`; then in `updateDisplay()` (see step 5) lazily create/replace `controlPanel` once `currentlyDisplaying != null`. Delete the `currentlyDisplayingOrPlaceholder()` idea.
   - Or make `AbstractLocationDiagram`/`ArmorPanel` tolerate a null entity/game until `displayMek` is called (ArmorPanel already accepts a null `Game`).
   Implement the deferred approach: keep a boolean `controlCardInstalled`; in `updateDisplay()`:
   ```java
   if (controlLayout && (currentlyDisplaying != null)
         && ((controlPanel == null) || (controlPanel.getEntity() != currentlyDisplaying))) {
       installControlCard();
   }
   ```
   where `installControlCard()` builds the `ControlPanel` for `currentlyDisplaying`, replaces the `CONTROL` card in `displayP`, packs, and calls `controlPanel.enlargeToFillDialog()`. (Rebuilding per displayed entity keeps the paperdoll bound to the right unit; it's cheap and mirrors how the GM dialog is per-entity. Add a `getEntity()` accessor to `ControlPanel` returning `entity`.)
5. **`updateDisplay()`** (`:505`): after the existing six `*.displayMek(...)` calls, add:
   ```java
   if (controlLayout) {
       // (see step 4) ensure the control card exists for the current entity, then refresh its paperdoll
       ensureControlCard();
       if (controlPanel != null) { controlPanel.displayMek(currentlyDisplaying); }
   }
   ```
   The six panel `displayMek` calls stay — `sPan/pPan/ePan` are the same instances hosted inside `controlPanel`, so they refresh normally.
6. **`showPanel(String s)`** (`:542`): make tab selection index-agnostic and map legacy keys to `CONTROL` in control layout:
   ```java
   public void showPanel(String s) {
       if (controlLayout) {
           // Pilot/Armor/Systems/Extras all collapse into the Control tab
           if (!(SUMMARY.equals(s) || WEAPONS.equals(s) || CONTROL.equals(s))) { s = CONTROL; }
       }
       if (GUI_PREFERENCES.getUnitDisplayStartTabbed() || controlLayout) {
           ((CardLayout) displayP.getLayout()).show(displayP, s);
       }
       tabStrip.setTab(tabStrip.indexOf(s));
       displayP.revalidate();
       displayP.repaint();
   }
   ```
   (Uses `MekPanelTabStrip.SUMMARY/WEAPONS/CONTROL` constants — import statically or qualify. Replaces the fixed `*_INDEX` chain.)
7. **`showSpecificSystem(int)`** (`:580`): in control layout, route to the control panel instead of a nonexistent Systems tab:
   ```java
   public void showSpecificSystem(int loc) {
       if (controlLayout) {
           showPanel(MekPanelTabStrip.CONTROL);
           if (controlPanel != null) { controlPanel.selectLocation(loc); } // add a public selectLocation delegating to showLocation
           return;
       }
       // ...existing classic body unchanged...
   }
   ```
   Add `public void selectLocation(int loc) { showLocation(loc); }` to `ControlPanel`.

> Only classic layout uses `setDisplayTabbed`/`setDisplayNonTabbed`; leave them and their non-tabbed reparenting untouched. Because control vs classic is decided once from the setting, the reused scroll panes never have two parents at once.

## 9. `KeyCommandBind.java` (optional)

File: `client/ui/util/KeyCommandBind.java` (`:105–110`). Either leave as-is (in control layout `showPanel` already maps Pilot/Armor/Systems/Extras F-keys onto Control), or add `UD_CONTROL("udControl", VK_F3)` and register it in `UnitDisplayPanel.registerKeyboardCommands` alongside the others with a guard. Lowest-effort: do nothing; the existing binds resolve to Control via §8.6.

---

## Compile & verify

1. `./gradlew :megamek:compileJava` — fix any signature/constant mismatches (esp. `Mek.LOC_HEAD`, skin-spec getter names, `Messages` import in `ControlPanel`).
2. Launch MegaMek, start/skip into a scenario with a Mek, a vehicle, an aero and infantry.
3. **Setting OFF (default):** confirm the 6-tab display is identical to before, including non-tabbed switch-view and the GM damage dialog.
4. **Setting ON** (Client Settings → Unit Display → "Use experimental Control layout", reopen the display): confirm three tabs; on Control, click paperdoll locations and verify the Systems content (equipment, mode combo, dump) follows; selecting the Head shows the Pilot; the Extras strip (sensors, heat sinks, activate hidden) works; the heat scale shows; General and Weapon tabs behave.
5. Repeat the Control checks for a vehicle, aero and infantry (paperdoll comes from each `*MapSet`).
6. Re-open the GM damage dialog and confirm no regression from the `DamageEditorDiagram` refactor.

## Risks / watch-list

- **Single-parent reparenting:** the reused scroll panes must be hosted in exactly one place. Because the layout is chosen once from the setting, this holds; do not add them to `displayP` cards in control layout.
- **Construction order:** don't build `ControlPanel` before a real `Entity` exists — use the deferred/lazy install in `updateDisplay()`.
- **`DamageEditorDiagram` refactor** is the one behavior-sensitive change; keep it mechanical and test the GM dialog. Fall back to Option B if needed.
- **Interim tab art:** Control reuses the Systems tab gif. Final `tab_control_*.gif` belongs in the separate assets repo (`data/images/widgets/`) and can land later via `UnitDisplaySkinSpecification` + skin editor.
- **Headless crew:** vehicles/aero/infantry have no cockpit location, so the Pilot card only auto-shows for Mek/ProtoMek in v1; a "Crew" location entry can be added later.

## Suggested commit sequence (single PR)

1. `GUIPreferences` + `CommonSettingsDialog` + messages (setting plumbing).
2. `AbstractLocationDiagram` + `DamageEditorDiagram` refactor (no UX change; verify GM dialog).
3. `UnitDisplayStyle` + `ControlPanel`.
4. `MekPanelTabStrip` data-driven.
5. `UnitDisplayPanel` wiring.
6. Compile fixes + smoke-test notes.

Commit message trailer to use:
```
Co-Authored-By: Claude <noreply@anthropic.com>
```
