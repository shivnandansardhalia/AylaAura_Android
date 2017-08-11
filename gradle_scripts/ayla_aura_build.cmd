echo checkout git repo
cd ..
rmdir /s /q libraries
mkdir libraries
cd libraries
git clone https://github.com/AylaNetworks/Android_AylaSDK_Public.git
cd Android_AylaSDK_Public
gradle build
