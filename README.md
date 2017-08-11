Aura
====
Ayla Engineering Demo Application

Build project using Android Studio 2.3+
======================================
$ export AYLA_PUBLIC=_Public

$git clone https://github.com/AylaNetworks/Android_Aura_Public.git  

$cd Android_Aura_Public/gradle_scripts  
Optionally set environment variables AYLA_BUILD_BRANCH to the branch to be built (default to your current branch), and AYLA_SDK_BRANCH to the Ayla SDK branch. Only change these if you are doing custom development.

$../gradlew -q execTasks  
Ayla SDK will be downloaded to 'libraries' folder and built in the project structure. In case you run into errors in this step, do not worry. We can rerun this command later. 

Upgrade your Android Studio to 2.3+

Then open Android Studio -> Import Project -> Select build.gradle file in project folder(Android_Aura). Android Studio may remind you to install extra packages to build the project. When reminded, install those required packages.  

In addition, this project requires gradle 3.3 which is installed by Android Studio 2.3. In Android Studio 2.3, click on "Preferences" then "Build, Execution, Deployment" then "Build Tools" then "Gradle" to find its full path. Now you can set your environment variable PATH to use this gradle 3.3.

If your previous "../gradlew -q execTasks" step fails, ran it again:
$cd Android_Aura_Public/gradle_scripts  
$../gradlew -q execTasks

To build the project in Android Studio, click 'Build' and select 'Make Project'

Contribute your code
====================

If you would like to contribute your own code change to our project, please submit pull requests against the "incoming" branch on Github. We will review and approve your pull requests if appropriate.

==================
Dependencies

Google Volley		http://developer.android.com/license.html  
Google Gson		http://developer.android.com/license.html  
NanoHttpd		https://github.com/NanoHttpd/nanohttpd/blob/master/LICENSE.md  
Autobahn Websockets	https://github.com/crossbario/autobahn-android/blob/master/LICENSE	
SpongyCastle		http://www.bouncycastle.org/licence.html  
Joda			http://www.joda.org/joda-time/license.html  
Robolectric 		https://github.com/robolectric/robolectric/blob/master/LICENSE.txt  

Documentation
=============
supported Android version: 4.4 and higher
JavaDoc for the libraries is available. Follow the instructions in at:
  <repo>/librariers/Android_AylaSDK/README.md

Version of major build tools used
=================================
Android Studio: 2.3.2
Gradle: 4.0

Releases
====================
v5.6.02    07/19/2017

- Built using Andriod AylaSDK 5.6.02
- Add a method to get user UUID

v5.6.01    06/19/2017

- Built using Andriod AylaSDK 5.6.01
- connectToNewDevice() support for WPA, WPA2, and WEP security

v5.6.00    06/14/2017

- Built using SDK v5.6
- New push notification support
- New device notification support
- New support for connectivity and datapoint ack event types
- Improved set-up flows
- New app notes for schedules, sharing, and notifications

v5.5.01    04/11/2017

- Support Android AylaSDK 5.5.01
- Fix lifecycle bug
- Add minimum time interval for refreshing authorization

v5.5.00    03/28/2017

New & Improved
- Support for Android_AylaSDK 5.5.00
- Local Device alpha
- Improved Setup Wizard

Bug Fixes & Chores
- Add a delay between starting a device scan, and asking for scan results
- Add more diagnostic info to About page and email logs
- Use gradlew instead of gradle when building SDK
- Miscellaneous bug fixes and UI improvements
- Includes all prior hot-fixes

v5.4.00    01/27/2017
- Move Baidu push notification support to a variant: GCM is the default
- Check if session manager is available before displaying app screens
- New About menu item and screen with version and debug info
- Check if fragment is attached to activity before updating UI
- Add file property preview support using new AylaDatapointBlob API
- Re-initialize the SDK after a configuration change
- Setup Wizard Improvements
- Bug fixes and UI improvements

v5.3.01    11/11/2016
- Wifi setup improvements
- support Android_AylaSDK 5.3.01

v5.3.00    10/25/2016

New and Improved
- Support for Android_AylaSDK 5.3.00
- New Android 10 support
- New Aura OEM Config: Just for IoT HW Engineers - Use core Aura features without knowing Objective-C, Java, or Swift!
- New Aura Test Runner: Network Profiler - Easily monitor round-trip network times
- Improved feedback during WiFi Setup

Chores and Bug Fixes
- More and improved unit tests
- Improved Fastlane support
- All 5.2.xx hot-fixes

Special Notes
In version 5.3 of the Ayla Mobile SDK, changes were made related to SSO sign-in that will require additions to classes that implement AylaAuthProvider.

Because SSO providers manage user profiles as well as authenticating users, the AylaAuthProvider interface has been augmented with interfaces to update
or delete the SSO user. The Ayla SDK will call these methods on the AylaAuthProvider that was used to sign in when requested to update or delete the session's user account.

The Ayla implementations of AylaAuthProvider (e.g. UsernameAuthProvider, CachedAuthProvider) have been updated to implement these methods to change the
profile information on the Ayla user service. External identity providers will need to implement these methods to update or delete the account information
on the identity provider's service. The following methods will need to be implemented in the mobile application on any class that implements the
AylaAuthProvider interface:

  // Updates the user profile information with the contents of the AylaUser parameter
  AylaAPIRequest updateUserProfile(AylaUser user, Listener<AylaUser> successListener,ErrorListener errorListener);

   // Deletes the current user account
  AylaAPIRequest deleteUser(AylaSessionManagersessionManager, Listener<EmptyResponse> successListener, ErrorListener errorListener);

v5.2.00    08/19/2016
New & Improved
- New Test Runner setup and registration module
- New Test Runner test configuration dialog framework object
- New LAN OTA platform feature support

Bug Fixes & Chores
- Built using the latest SDK
- Add Factory Reset to the device menu
- Staging Service Support
- Improved layout of the device details page
- Automated testing via Jenkins and Robolectric with test cases via Zephyr
- Using Fastlane for automated build and release

Known Issues
- Wifi setup will not work with Android N Beta. Scanning for WLAN access points fails. See https://code.google.com/p/android/issues/detail?id=219258.

v5.1.00    06/22/2016
New Features:
- New setup wizard for WiFi setup and registration
- Offline (LAN) sign-in and LAN device connectivity using cached data
- Generic Gateway and Node registration
- Developer Options activity to configure developer settings
- Additional test cases for TestRunner
- Baidu and GCM Push notification support
- Update account email address
- Device Detail Provider support for additional device types including Ayla smart plugs, generic gateways
- Change device time zones
- Device Sharing
- Device Schedules

Enhancements and Bug Fixes:
- Code updates to support 5.1.00 Ayla Mobile SDK
- WiFi setup flow fixes
- UI improvements

v5.0.04   06/08/2016
- WiFi Setup improvements
- Add LAN mode datapoint timestamps when missing
- work with SDK 5.0.04

v5.0.02   05/16/2016
- WiFi AP regular expression may be modified from within Aura
- Updated API calls to use new connectDeviceToService response

v5.0.00 	04/19/2016
Initial release


