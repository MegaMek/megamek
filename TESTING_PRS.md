# Testing Pull Requests Locally in IntelliJ IDEA

This guide explains how to test pull requests (PRs) from the MegaMek repository in your local IntelliJ IDEA installation.

## Prerequisites

- IntelliJ IDEA installed (Community or Ultimate edition)
- Git installed and configured
- Java 17 or newer installed (see [README.md](README.md) for Java installation instructions)
- A local clone of the MegaMek repository

**Note:** If you cloned MegaMek from GitHub, IntelliJ should handle all dependencies automatically when you open the project. 
The build system will download any required data files during the first build.

## Quick Start: Testing PR #7483

PR #7483 fixes laser heat sink bugs. Here's how to test it:

### Method 1: Using Git Command Line (Recommended)

1. **Navigate to your MegaMek repository:**
   ```bash
   cd /path/to/your/megamek
   ```

2. **Fetch the PR branch:**
   ```bash
   git fetch origin pull/7483/head:pr-7483
   ```
   
   This creates a local branch called `pr-7483` from the PR.

3. **Checkout the PR branch:**
   ```bash
   git checkout pr-7483
   ```

4. **Open/Refresh the project in IntelliJ IDEA:**
   - If IntelliJ is already open, it will detect the branch change
   - Or, open IntelliJ and use `File → Open` to open the MegaMek directory

5. **Build the project:**
   - In IntelliJ, open the Gradle tool window (View → Tool Windows → Gradle)
   - Run the build task: `megamek → Tasks → build → build`
   - Or use the terminal in IntelliJ: `./gradlew build`

6. **Run MegaMek:**
   - In IntelliJ, navigate to `megamek/src/megamek/MegaMek.java`
   - Right-click on the file and select "Run 'MegaMek.main()'"
   - Or run from terminal: `./gradlew run`

### Method 2: Using GitHub Desktop or IntelliJ's Git Integration

#### Using IntelliJ's Git Integration:

1. **Open your MegaMek project in IntelliJ**

2. **Fetch the PR:**
   - Go to `Git → Fetch`
   - Or use `VCS → Git → Fetch`

3. **Checkout the PR branch:**
   - Go to `Git → Branches`
   - Select `origin/issue-7377-laser-heatsinks` (the branch name for PR #7483)
   - Choose "Checkout as new local branch"

4. **Build and run** (see steps 5-6 from Method 1)

## Testing PR #7483: Laser Heat Sink Fixes

PR #7483 addresses two specific laser heat sink bugs. To test these changes:

### What Changed:

1. **Water Immersion Fix:** Laser heat sinks now correctly receive +6 heat capacity bonus when underwater
2. **Temperature Immunity Fix:** Laser heat sinks are now correctly affected by extreme temperatures

### Test Scenarios:

#### Test 1: Water Immersion Bonus

1. **Setup:**
   - Host a new game
   - Create a mech with laser heat sinks (e.g., Notos Prime with 20 laser heat sinks)
   - Set up a map with water hexes at depth 4 or more

2. **Test:**
   - Move the mech into deep water (depth 4+)
   - Fire weapons to generate heat (e.g., 20 heat from weapons)
   - Walk (+1 heat) for total of 21 heat generated
   - End turn in the water

3. **Expected Result:**
   - With 20 laser heat sinks underwater, you should sink: 20 (base) + 6 (water bonus) = 26 heat
   - Starting at 0 heat + 21 generated - 26 dissipated = Should end at 0 heat (not 1 heat)
   - The heat capacity display should show the +6 bonus

#### Test 2: Extreme Temperature Effects

1. **Setup:**
   - Host a new game
   - Create a mech with laser heat sinks
   - Set planetary conditions to extreme temperature (e.g., 100°C or very cold)
   - Use: `Game Options → Planetary Conditions → Temperature`

2. **Test:**
   - Fire weapons without moving to generate known amount of heat
   - Example: Notos Prime fires all weapons for 20 heat

3. **Expected Result:**
   - At 100°C temperature (+5 heat from extreme temp):
     - Should gain 20 (weapons) + 5 (temperature) = 25 heat total
     - Should dissipate 20 heat (from 20 heat sinks)
     - Should end turn at 5 heat (not 0 heat)
   - The heat summary report should show the temperature modifier

#### Test 3: Night/Dusk Modifiers (Already Working)

1. **Setup:**
   - Host a new game with Night or Dusk lighting conditions
   - Create a mech with laser heat sinks

2. **Expected Result:**
   - Should already work correctly (not changed by this PR)
   - Modifier reduced by 1 when generating heat
   - Modifier removed completely when overheating

## General Tips for Testing PRs

### Before Testing:
- Always create a backup of your game data if testing significant changes
- Note the current branch you're on so you can return to it: `git branch`

### After Testing:
- Return to your original branch:
  ```bash
  git checkout main
  # or
  git checkout <your-branch-name>
  ```

### Providing Feedback:
- Test the specific scenarios mentioned in the PR description
- Take screenshots of any issues or unexpected behavior
- Comment on the PR with your findings, including:
  - What you tested
  - Expected vs. actual results
  - Screenshots or logs if relevant
  - Your system information (OS, Java version, IntelliJ version)

## Building from Command Line

If you prefer to build without IntelliJ:

```bash
# Build the project
./gradlew build

# Run MegaMek
./gradlew run

# Clean and rebuild
./gradlew clean build
```

## Troubleshooting

### Issue: "Permission denied" when running gradlew

**Solution:**
```bash
chmod +x gradlew
./gradlew build
```

### Issue: Build fails with Java version error

**Solution:** Ensure you have Java 17 or newer installed. Check with:
```bash
java -version
```

### Issue: IntelliJ doesn't recognize the project structure

**Solution:**
1. Close the project
2. Delete the `.idea` folder in your MegaMek directory
3. Reopen the project in IntelliJ
4. IntelliJ will re-index and import the Gradle project

### Issue: Can't find the PR branch

**Solution:**
```bash
# Fetch all remote branches
git fetch --all

# List all remote branches
git branch -r | grep pull

# If the branch isn't there, use the fetch command from Method 1
git fetch origin pull/7483/head:pr-7483
```

### Issue: Build fails with "Included build does not exist" error

**Solution:** This typically means the project is looking for dependencies in sibling directories. IntelliJ IDEA handles 
this automatically when you open the project. If building from command line fails, use IntelliJ's built-in Gradle 
integration instead:
1. Open the project in IntelliJ IDEA
2. Let IntelliJ sync the Gradle project (it will show a notification)
3. Use the Gradle tool window to build and run

Alternatively, you can download a pre-built development release from the [MegaMek releases page](https://github.com/MegaMek/megamek/releases) 
and only test specific features.

## Additional Resources

- [MegaMek Wiki](https://github.com/MegaMek/megamek/wiki)
- [Working with Gradle](https://github.com/MegaMek/megamek/wiki/Working-With-Gradle)
- [MegaMek Coding Style Guide](https://github.com/MegaMek/megamek/wiki/MegaMek-Coding-Style-Guide)
- [GitHub: Checking out PRs locally](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/reviewing-changes-in-pull-requests/checking-out-pull-requests-locally)

## For Developers: Running Tests

To run the test suite:

```bash
# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :megamek:test

# Run tests with detailed output
./gradlew test --info
```

## Questions?

If you have questions about testing PRs or encounter issues not covered here:
- Visit the [MegaMek Discord](https://discord.gg/megamek)
- Check the [GitHub Issues](https://github.com/MegaMek/megamek/issues)
- Ask in the relevant PR discussion thread
