#!/bin/bash

#echo color
WHITE_COLOR="\E[1;37m";
RED_COLOR="\E[1;31m";
BLUE_COLOR='\E[1;34m';
GREEN_COLOR="\E[1;32m";
YELLOW_COLOR="\E[1;33m";
RES="\E[0m";

echo_r () {
    # Color red: Error, Failed
    [ $# -ne 1 ] && return 1
    printf "[${BLUE_COLOR}opencron${RES}] ${RED_COLOR}$1${RES}\n"
}

echo_g () {
    # Color green: Success
    [ $# -ne 1 ] && return 1
    printf "[${BLUE_COLOR}opencron${RES}] ${GREEN_COLOR}$1${RES}\n"
}

echo_y () {
    # Color yellow: Warning
    [ $# -ne 1 ] && return 1
    printf "[${BLUE_COLOR}opencron${RES}] ${YELLOW_COLOR}$1${RES}\n"
}

echo_w () {
    # Color yellow: White
    [ $# -ne 1 ] && return 1
    printf "[${BLUE_COLOR}opencron${RES}] ${WHITE_COLOR}$1${RES}\n"
}

if [ -z "$JAVA_HOME" -a -z "$JRE_HOME" ]; then
    echo_r "Neither the JAVA_HOME nor the JRE_HOME environment variable is defined"
    echo_r "At least one of these environment variable is needed to run this program"
    exit 1
fi

# Set standard commands for invoking Java, if not already set.
if [ -z "$RUNJAVA" ]; then
  RUNJAVA="$JAVA_HOME"/bin/java
fi

#check java exists.
$RUNJAVA >/dev/null 2>&1

if [ $? -ne 1 ];then
  echo_r "ERROR: java is not install,please install java first!"
  exit 1;
fi

#check openjdk
if [ "`${RUNJAVA} -version 2>&1 | head -1|grep "openjdk"|wc -l`"x == "1"x ]; then
  echo_r "ERROR: please uninstall OpenJDK and install jdk first"
  exit 1;
fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
darwin=false
os400=false
case "`uname`" in
CYGWIN*) cygwin=true;;
Darwin*) darwin=true;;
OS400*) os400=true;;
esac

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

WORKDIR=`cd "$PRGDIR" >/dev/null; pwd`;

APP_ARTIFACT=opencron-server

APP_VERSION="1.2.0-RELEASE";

APP_WAR_NAME=${APP_ARTIFACT}.war

MAVEN_TARGET_WAR="${WORKDIR}"/${APP_ARTIFACT}/target/${APP_WAR_NAME}

DIST_PATH=${WORKDIR}/dist/

[ ! -d "${DIST_PATH}" ] && mkdir -p "${DIST_PATH}"

DEPLOY_PATH=${WORKDIR}/dist/opencron-server

STARTUP_SHELL=${WORKDIR}/${APP_ARTIFACT}/startup.sh

#先检查dist下是否有war包
if [ ! -f "${DIST_PATH}/${APP_WAR_NAME}" ] ; then
    #dist下没有war包则检查server的target下是否有war包.
   if [ ! -f "${MAVEN_TARGET_WAR}" ] ; then
      echo_w "[opencron] please build project first!"
      exit 0;
   else
      cp ${MAVEN_TARGET_WAR} ${DIST_PATH};
   fi
fi

[ -d "${DEPLOY_PATH}" ] && rm -rf ${DEPLOY_PATH}/* || mkdir -p ${DEPLOY_PATH}

# unpackage war to dist
cp ${DIST_PATH}/${APP_WAR_NAME} ${DEPLOY_PATH} && cd ${DEPLOY_PATH} && jar xvf ${APP_WAR_NAME} >/dev/null 2>&1 && rm -rf ${DEPLOY_PATH}/${APP_WAR_NAME}

#copy jars...
cp -r ${WORKDIR}/${APP_ARTIFACT}/work ${DEPLOY_PATH}

#copy startup.sh
cp  ${STARTUP_SHELL} ${DEPLOY_PATH}

#startup
/bin/bash +x "${DEPLOY_PATH}/startup.sh" "$@"

