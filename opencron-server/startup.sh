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
                                                      __ _ _
  /\     ____  ____  ___  ____  ______________  ____  \ \ \ \
 (())   / __ \/ __ \/ _ \/ __ \/ ___/ ___/ __ \/ __ \  \ \ \ \
  \/   / /_/ / /_/ /  __/ / / / /__/ /  / /_/ / / / /   ) ) ) )
       \____/ .___/\___/_/ /_/\___/_/   \____/_/ /_/   / / / /
           /_/     ::opencron::(v1.2.0 RELEASE)       /_/_/_/

EOT
printf "${RES}\n"

echo_r () {
    # Color red: Error, Failed
    [ $# -ne 1 ] && return 1
    printf "[${GREEN_COLOR}opencron${RES}] ${RED_COLOR}$1${RES}\n"
}

echo_g () {
    # Color green: Success
    [ $# -ne 1 ] && return 1
    printf "[${GREEN_COLOR}opencron${RES}] ${GREEN_COLOR}$1${RES}\n"
}

echo_y () {
    # Color yellow: Warning
    [ $# -ne 1 ] && return 1
    printf "[${GREEN_COLOR}opencron${RES}] ${YELLOW_COLOR}$1${RES}\n"
}

echo_w () {
    # Color yellow: White
    [ $# -ne 1 ] && return 1
    printf "[${GREEN_COLOR}opencron${RES}] ${WHITE_COLOR}$1${RES}\n"
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

LIB_PATH="$WORKDIR"/WEB-INF/lib

LOG_PATH="$WORKDIR"/work/logs

# Add jars to classpath
if [ ! -z "$CLASSPATH" ] ; then
  CLASSPATH="$CLASSPATH":
fi
CLASSPATH="$CLASSPATH""$WORKDIR"/WEB-INF/classes

for jar in ${LIB_PATH}/*
do
  CLASSPATH="$CLASSPATH":"$jar"
done

#start server....
printf "[${BLUE_COLOR}opencron${RES}] ${WHITE_COLOR} server Starting.... ${RES}\n"

eval "\"$RUNJAVA\"" \
        -classpath "\"$CLASSPATH\"" \
        -Dserver.launcher=tomcat \
        org.opencron.server.bootstrap.Startup $1 \
        >/dev/null 2>&1 "&";

printf "[${BLUE_COLOR}opencron${RES}] ${WHITE_COLOR} please see log for more detail:${RES}${GREEN_COLOR} $LOG_PATH/opencron.out ${RES}\n"

exit $?


