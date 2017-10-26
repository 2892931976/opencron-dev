package org.opencron.common.util;

import io.netty.util.internal.SystemPropertyUtil;

public class Constants {

    public static final int WRITER_IDLE_TIME_SECONDS = SystemPropertyUtil.getInt("opencron.io.writer.idle.time.seconds", 30);

    /** Server链路read空闲检测, 默认60秒, 60秒没读到任何数据会强制关闭连接 */
    public static final int READER_IDLE_TIME_SECONDS =
            SystemPropertyUtil.getInt("opencron.io.reader.idle.time.seconds", 60);


}
