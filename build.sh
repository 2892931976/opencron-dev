#!/bin/bash

#echo color
WHITE_COLOR="\E[1;37m";
RED_COLOR="\E[1;31m";
BLUE_COLOR='\E[1;34m';
GREEN_COLOR="\E[1;32m";
YELLOW_COLOR="\E[1;33m";
RES="\E[0m";

printf "${GREEN_COLOR}\n"
cat<<EOT

      --------------------------------------------
    /                                              \\
   /   ___  _ __   ___ _ __   ___ _ __ ___  _ __    \\
  /   / _ \| '_ \ / _ \ '_ \ / __| '__/ _ \| '_ \\    \\
 /   | (_) | |_) |  __/ | | | (__| | | (_) | | | |    \\
 \\    \___/| .__/ \___|_| |_|\___|_|  \___/|_| |_|    /
  \\        |_|                                       /
   \\                                                /
    \\       --opencron,Let's crontab easy!         /
      --------------------------------------------

EOT
printf "${RES}\n"

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

MAVEN_URL="http://mirror.bit.edu.cn/apache/maven/maven-3/3.5.2/binaries/apache-maven-3.5.2-bin.tar.gz";

MAVEN_NAME="apache-maven-3.5.2-bin"

UNPKG_MAVEN_NAME="apache-maven-3.5.2";

OPENCRON_VERSION="1.2.0-RELEASE";

MAVEN_PATH="/tmp";

[ ! -d "$MAVEN_PATH" ] && mkdir $MAVEN_PATH;

DIST_HOME="${WORKDIR}/dist"

USER="`id -un`"
LOGNAME="$USER"
if [ $UID -ne 0 ]; then
    echo_y "WARNING: Running as a non-root user, \"$LOGNAME\". Functionality may be unavailable. Only root can use some commands or options"
fi

#check maven exists
mvn >/dev/null 2>&1

if [ $? -ne 1 ]; then

    echo_y "WARNING:maven is not install!"

    if [ -x "/tmp/${UNPKG_MAVEN_NAME}" ] ; then
        echo_w "maven is already download,now config setting...";
        MVN=/tmp/${UNPKG_MAVEN_NAME}/bin/mvn
    else
        echo_w "download maven Starting..."
        echo_w "checking network connectivity ... "
        net_check_ip=114.114.114.114
        ping_count=2
        ping -c ${ping_count} ${net_check_ip} >/dev/null
        retval=$?
        if [ ${retval} -eq 0 ] ; then
            echo_w "network is connectioned,download maven Starting... "
            wget -P "/tmp" $MAVEN_URL && {
                echo_g "download maven successful!";
                echo_w "install maven Starting"
                tar -xzvf /tmp/${MAVEN_NAME}.tar.gz -C /tmp
                MVN=/tmp/${UNPKG_MAVEN_NAME}/bin/mvn
            }
        elif [ ${retval} -ne 0 ]; then
            echo_r "ERROR:network is blocked! download maven failed,please check your network!build error! bye!"
            exit 1
        fi
    fi

elif [ "$MVN"x = ""x ]; then
    MVN="mvn";
fi

echo_w "build opencron Starting...";

$MVN clean install -Dmaven.test.skip=true;

retval=$?

if [ ${retval} -eq 0 ] ; then
    [ ! -d "${DIST_HOME}" ] && mkdir ${DIST_HOME} || rm -rf  ${DIST_HOME}/* ;
    cp ${WORKDIR}/opencron-agent/target/opencron-agent-${OPENCRON_VERSION}.tar.gz ${DIST_HOME}
    cp ${WORKDIR}/opencron-server/target/opencron-server.war ${DIST_HOME}
    printf "[${BLUE_COLOR}opencron${RES}] ${WHITE_COLOR}build opencron @Version ${OPENCRON_VERSION} successfully! please goto${RES} ${GREEN_COLOR}${DIST_HOME}${RES}\n"
    exit 0
else
    echo_r "build opencron failed! please try again "
    exit 1
fi
