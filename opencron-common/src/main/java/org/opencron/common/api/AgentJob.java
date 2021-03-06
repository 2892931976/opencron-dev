package org.opencron.common.api;

import org.opencron.common.job.Request;
import org.opencron.common.job.Response;

public interface AgentJob {

    Response ping(Request request);

    Response path(Request request);

    Response monitor(Request request);

    Response execute(Request request);

    Response password(Request request);

    Response kill(Request request);

    Response proxy(Request request);

    Response guid(Request request);

    /**
     * 扫描Agent里crontab已经定义的job
     *
     * @param request
     * @return
     */
    Response scanCrontab(Request request);

    void restart(Request request);

}
