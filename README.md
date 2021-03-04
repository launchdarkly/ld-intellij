# LaunchDarkly Intellij

![Build](https://github.com/launchdarkly/ld-intellij/workflows/Build/badge.svg)

<!-- Plugin description -->
Interact with your LaunchDarkly Feature Flags directly in your IDE.

- Ability to toggle a flag enabled status.
- Visualize Flag information.

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

- Generate a [LaunchDarkly Personal Access Token](https://app.launchdarkly.com/settings/authorization/tokens/new) with a
  writer role(if toggling flags). Example policy is below:

```
[
  {
    "resources": ["proj/*"],
    "actions": ["viewProject"],
    "effect": "allow"
  },
  {
    "resources": ["proj/*:env/*:flag/*"],
    "actions": ["updateOn", "updateFallthrough", "updateOffVariation"],
    "effect": "allow"
  }
]
```

- Under `Preferences` -> `LaunchDarkly Plugin` add the Access Token then click apply. A list of Projects and
  Environments should populate if the key has the correct permissions or an error show in the drop down. The environment
  list will auto update based on the Project selected.
