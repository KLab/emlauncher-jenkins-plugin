# This is EMLauncher Jenkins Plugin.

Based on TestFlight Plugin (https://github.com/jenkinsci/testflight-plugin).

## Build
Need Maven, JDK
"mvn package" on project root.
- This command generate target/emlauncher.hpi
- From Jenkins Plugin Settings, install this .hpi.

In Jenkins General Settings, Setting EMLauncher Server configuration.
And In your Build configuration, Add EMLauncher Plugin to post build process and set some parameter.

## Additional Furture
- Compatibility with Jenkins pipeline.
- Japanese help file added.
- Message translated into Japanese.
- Compatibility with JEP-200 security logic.
- Supports uploading to servers not configure in "Global Settings".
- Added macro expansion by "TokenMacro" to description sent to EMlauncher.
 Like:
    ${ENV ,var="description"}
