# LaunchDarkly IntelliJ IDEA plugin

![Build](https://github.com/launchdarkly/ld-intellij/workflows/Build/badge.svg)

<!-- Plugin description -->
Interact with your LaunchDarkly Feature Flags directly in your IDE:

- Toggle a flag status
- Visualize flag information

[View in the JetBrains Marketplace](https://plugins.jetbrains.com/plugin/15159-launchdarkly)

<!-- Plugin description end -->

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
