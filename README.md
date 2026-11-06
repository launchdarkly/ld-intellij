# LaunchDarkly IntelliJ IDEA plugin

> [!WARNING]
> **This plugin is no longer supported.** LaunchDarkly does not maintain this plugin and will not release new versions. It only installs on IntelliJ 2024.1 (build 241) and uses a LaunchDarkly REST API version that is being retired. Issues and pull requests will not be reviewed.
>
> To work with feature flags from your editor, use the [LaunchDarkly MCP server](https://launchdarkly.com/docs/home/getting-started/mcp) or the [VSCode extension](https://launchdarkly.com/docs/home/getting-started/vscode).

![Build](https://github.com/launchdarkly/ld-intellij/workflows/Build/badge.svg)

<!-- Plugin description -->
Interact with your LaunchDarkly feature flags directly in your IDE.

- Autocomplete feature flag keys
- View a tooltip with feature flag details when you hover over a flag key in your source code
- Open to specific feature flags in LaunchDarkly
- View a list of feature flags and their settings in the LaunchDarkly tool window
- Update a feature flag's targeting status and default rule and value when targeting is on or off

<!-- Plugin description end -->

[Available in the JetBrains Marketplace](https://plugins.jetbrains.com/plugin/15159-launchdarkly)

## Installation

- Using IDE built-in plugin system:

  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for LaunchDarkly</kbd> >
  <kbd>Install Plugin</kbd>

- Manually:

  Download the [latest release](https://github.com/launchdarkly/ld-intellij/releases/latest) and install it manually
  using
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Setup

- Generate a [LaunchDarkly Personal Access Token](https://docs.launchdarkly.com/home/account-security/api-access-tokens) with a writer role (if toggling flags). Example policy is below:

```
[
  {
    "effect": "allow",
    "actions": ["viewProject"],
    "resources": ["proj/*"]
  },
  {
    "effect": "allow",
    "actions": [
      "updateOn",
      "updateFallthrough",
      "updateOffVariation"
    ],
    "resources": ["proj/*:env/*:flag/*"]
  }
]

```

- Under <kbd>Preferences</kbd> > <kbd>LaunchDarkly</kbd> add the Access Token and then click <kbd>Apply</kbd>.
  A list of projects and environments
  will populate if the token has the correct permissions. The `Environment` list will automatically update based on the `Project` selected.
