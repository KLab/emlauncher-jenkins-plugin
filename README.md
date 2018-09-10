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

## Example Pipeline script
```
BUILD_TARGET = "TestApp"
OUTPUT_FILE_NAME = "${BUILD_TARGET}.ipa"

stage('upload') {
    // Upload IPA to a test site such as TestFlight.
    emlauncherUploader(
        hostTokenPairName: 'Sandbox',
        filePath: "build/Release-iphoneos/AdHoc/${OUTPUT_FILE_NAME}",
        dsymPath: "build/Release-iphoneos/AdHoc/${BUILD_TARGET}-dSYM.zip",
        title: 'My test app',
        description: 'Jenkins pipelineビルド',
        tags: 'test pipline jenkins',
        appendChangelog: true
    )
}
```
