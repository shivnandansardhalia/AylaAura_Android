#!/bin/sh
# Note: this script needs to run under release/ folder.
# then go back to parent folder of release/ to do release commands

if command -v fastlane; then
    echo "You have fastlane installed".
else
    echo "Please install fastlane first."
    exit 1;
fi

if [ -d ../fastlane ]; then
    echo "Your release environment has set up."
    cd ..
else
    cd ..
    mkdir fastlane
fi
cp -r release/fastlane/* fastlane/
