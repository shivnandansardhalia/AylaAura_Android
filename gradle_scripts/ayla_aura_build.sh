#!/bin/bash
#
# The following environment variables are optional for you to control your build:
# AYLA_BUILD_BRANCH: default to the current branch
# AYLA_SDK_BRANCH: default to AYLA_BUILD_BRANCH
# if you want to build a branch other than your crrent branch, switch to that branch to build it
#
# The following are for rare cases when you use a git protocol other than https or a different remote
# AYLA_PUBLIC: "" internal, "_Public" public repo, script detects this automatically unless you specify
# AYLA_SDK_REPO: default to https://github.com/AylaNetworks/Android_AylaSDK(_Public).git
# AYLA_REMOTE: default to origin
#

# You can change the following variables to configure your build
AYLA_BUILD_BRANCH=${AYLA_BUILD_BRANCH:-}  # or: AYLA_BUILD_BRANCH=${AYLA_BUILD_BRANCH:develop}
AYLA_SDK_BRANCH=${AYLA_SDK_BRANCH:-release/5.6.02} #brian/localdevice} # or: -$AYLA_BUILD_BRANCH}
AYLA_REMOTE=${AYLA_REMOTE:-origin}
AYLA_SDK_REPO=${AYLA_SDK_REPO:-} # or: -https://github.com/AylaNetworks/Android_AylaSDK(_Public).git
AYLA_LD_SDK_REPO=${AYLA_LD_SDK_REPO:-} # or: -https://github.com/AylaNetworks/Android_AylaSDK(_Public).git
AYLA_PUBLIC=${AYLA_PUBLIC:-}

cur_path=`pwd`
parent_path=`dirname "$cur_path"`
public_repo_path_pattern=".*_Public$"
if [[ $parent_path =~ $public_repo_path_pattern ]]; then
    AYLA_PUBLIC=${AYLA_PUBLIC:-_Public}
else
    # internal developers default
    AYLA_PUBLIC=${AYLA_PUBLIC:-}
fi
[ "X$AYLA_PUBLIC" == "X" ] && repo_type="internal" || repo_type="public"
AYLA_SDK_REPO=${AYLA_SDK_REPO:-https://github.com/AylaNetworks/Android_AylaSDK${AYLA_PUBLIC}.git}

branch_string=`git branch |grep -E "\*"`
if [ $? -ne 0 ]; then
    echo "No current branch found"
    AYLA_BUILD_BRANCH=${AYLA_BUILD_BRANCH:-master}
else
   cur_branch=`echo "${branch_string}" | cut -d' ' -f 2`
   # default all branches to the current branch if variable not set or empty
   AYLA_BUILD_BRANCH=${AYLA_BUILD_BRANCH:-$cur_branch}
fi

# in case when AYLA_BUILD_BRANCH is default to current branch
AYLA_SDK_BRANCH=${AYLA_SDK_BRANCH:-$AYLA_BUILD_BRANCH}

# conext display: show value whenever related environment variables are set
build_var_name_list="AYLA_BUILD_BRANCH AYLA_SDK_BRANCH AYLA_SDK_REPO AYLA_REMOTE AYLA_PUBLIC"
for n in $build_var_name_list; do
    [ `printenv | grep "$n"` ] && echo -e "Your $n is set to \"${!n}\""
done

green=`tput setaf 2`
reset=`tput sgr0`
styled_repo_type=${green}${repo_type}${reset}
styled_branch=${green}${AYLA_BUILD_BRANCH}${reset}
styled_sdk_branch=${green}${AYLA_SDK_BRANCH}${reset}
styled_sdk_repo=${green}${AYLA_SDK_REPO}${reset}
echo -e "\n*** Building ${styled_repo_type} repo on branch ${styled_branch} with sdk branch ${styled_sdk_branch}***"
echo "*** sdk repo: ${styled_sdk_repo}***"

for n in $build_var_name_list; do
    echo -e "Now $n = \"${!n}\""
done
echo -e "\ncheckout git repo"

cd ..
git fetch $AYLA_REMOTE
git branch $AYLA_BUILD_BRANCH $AYLA_REMOTE/$AYLA_BUILD_BRANCH
git checkout $AYLA_BUILD_BRANCH
git pull

rm -rf libraries
mkdir libraries
cd libraries
git clone $AYLA_SDK_REPO

if [ "$AYLA_PUBLIC" == "" ]; then
    echo "Set up symbolic link for gradle project dependency"
    ln -sf Android_AylaSDK Android_AylaSDK_Public
fi

pushd Android_AylaSDK$AYLA_PUBLIC
echo Get Android_AylaSDK from branch $AYLA_SDK_BRANCH
git fetch $AYLA_REMOTE
git branch $AYLA_SDK_BRANCH $AYLA_REMOTE/$AYLA_SDK_BRANCH
git checkout $AYLA_SDK_BRANCH
if [ $? -ne 0 ]; then
    echo "checkout ${AYLA_SDK_BRANCH} failure. Please check if this branch exists on your repo."
    exit 1
fi

# not run unit tests here, just do build
../../gradlew build -x test
