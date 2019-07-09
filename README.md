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
- Move EMLauncher host's authentication information (host name and API key) from global settings to credentials.
- Added compatibility with Credentials Binding Plugin.

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

## How to use 'Credentials Binding Plugin'
- Enable 'Use secret text(s) or file(s)'.
- Added 'EM launcher Host Token Pair' to 'Bindings'.
- Specifies the name of the environment variable that stores the information stored in 'Credentials'.

 Then, you can use the value of the environment variable specified here by a shell script etc. after that.

