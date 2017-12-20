#!/bin/bash

#echo color
RED_COLOR="\E[1;31m";
BLUE_COLOR='\E[1;34m';
RES="\E[0m";

echo_r () {
    # Color red: Error, Failed
    [ $# -ne 1 ] && return 1
    printf "[${BLUE_COLOR}opencron${RES}] ${RED_COLOR}$1${RES}\n"
}

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

APP_ARTIFACT=opencron-agent

APP_VERSION="1.2.0-RELEASE";

APP_TAR_NAME=${APP_ARTIFACT}-${APP_VERSION}.tar.gz

MAVEN_TARGET_TAR="${WORKDIR}"/${APP_ARTIFACT}/target/${APP_TAR_NAME}

DIST_PATH=${WORKDIR}/dist/

[ ! -d "${DIST_PATH}" ] && mkdir -p "${DIST_PATH}"

DEPLOY_PATH=${WORKDIR}/dist/opencron-agent

#先检查dist下是否有war包
if [ ! -f "${DIST_PATH}/${APP_TAR_NAME}" ] ; then
    #dist下没有tar包则检查agent的target下是否有tar包.
   if [ ! -f "${MAVEN_TARGET_TAR}" ] ; then
      echo_r "[opencron] please build project first!"
      exit 0;
   else
      cp ${MAVEN_TARGET_TAR} ${DIST_PATH};
   fi
fi

[ -d "${DEPLOY_PATH}" ] && rm -rf ${DEPLOY_PATH}/* || mkdir -p ${DEPLOY_PATH}

tar -xzvf ${DIST_PATH}/${APP_TAR_NAME} -C ${DEPLOY_PATH}/../ >/dev/null 2>&1

#startup
/bin/bash +x "${DEPLOY_PATH}/bin/startup.sh" "$@"

