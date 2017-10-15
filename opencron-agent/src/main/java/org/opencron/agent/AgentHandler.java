/**
 * Copyright (c) 2015 The Opencron Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.opencron.agent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.exec.*;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.opencron.common.job.*;
import org.opencron.common.job.RpcType;
import org.opencron.common.logging.LoggerFactory;
import org.opencron.common.util.*;
import org.slf4j.Logger;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

import static org.opencron.common.util.CommonUtils.*;
import static org.opencron.common.util.ReflectUtils.isPrototype;

public class AgentHandler extends SimpleChannelInboundHandler<Request> {

    private Logger logger = LoggerFactory.getLogger(AgentHandler.class);

    private final String REPLACE_REX = "%s:\\sline\\s[0-9]+:";

    private String EXITCODE_KEY = "exitCode";

    private String EXITCODE_SCRIPT = String.format("\n\necho %s:$?", EXITCODE_KEY);

    private ThreadPoolExecutor pool;

    private AgentMonitor agentMonitor;

    private String password;

    public AgentHandler(ThreadPoolExecutor pool,AgentMonitor agentMonitor){
        this.pool = pool;
        this.agentMonitor = agentMonitor;
        this.password = SystemPropertyUtils.get("opencron.password","opencron");
    }

    @Override
    public void channelActive(ChannelHandlerContext handlerContext) {
        logger.info("[opencron] agent channelActive Active...");
        handlerContext.fireChannelActive();
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext handlerContext,final Request request) throws Exception {

        this.pool.execute(new Runnable() {

            @Override
            public void run() {

                Action action = request.getAction();

                //verify password...
                if (!password.equalsIgnoreCase(request.getPassword())) {
                    Response response = Response.response(request)
                            .setSuccess(false)
                            .setExitCode(Opencron.StatusCode.ERROR_PASSWORD.getValue())
                            .setMessage(Opencron.StatusCode.ERROR_PASSWORD.getDescription())
                            .end();

                    handlerContext.writeAndFlush(response);
                    return;
                }

                Response response = null;

                switch (action) {
                    case PING:
                        response = ping(request);
                        break;
                    case EXECUTE:
                        response = execute(request);
                        break;
                    case PASSWORD:
                        response = password(request);
                        break;
                    case KILL:
                        response = kill(request);
                        break;
                    case GUID:
                        response = guid(request);
                        break;
                    case PATH:
                        response = path(request);
                        break;
                    case PROXY:
                        response = proxy(request);
                        break;
                    case MONITOR:
                        response = monitor(request);
                        break;
                    case RESTART:
                        restart(request);
                        break;
                    default:
                        break;
                }

                if(request.getRpcType()!= RpcType.ONE_WAY){    //非单向调用
                    handlerContext.writeAndFlush(response);
                }
                logger.info("[opencron] agent process done,request:{},action:", request.getId(), request.getAction());
            }

        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.error("[opencron] agent channelInactive");
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    //@Override
    public Response ping(Request request) {
        return Response.response(request).setSuccess(true).setExitCode(Opencron.StatusCode.SUCCESS_EXIT.getValue()).end();
    }

    //@Override
    public Response path(Request request) {
        //返回密码文件的路径...
        return Response.response(request).setSuccess(true)
                .setExitCode(Opencron.StatusCode.SUCCESS_EXIT.getValue())
                .setMessage(Configuration.OPENCRON_HOME)
                .end();
    }

    //@Override
    public Response monitor(Request request) {
        Opencron.ConnType connType = Opencron.ConnType.getByName(request.getParams().get("connType"));
        Response response = Response.response(request);
        switch (connType) {
            case PROXY:
                Monitor monitor = agentMonitor.monitor();
                Map<String, String> map  = serializableToMap(monitor);
                response.setResult(map);
                return response;
            default:
                return null;
        }
    }

    //@Override
    public Response execute(final Request request) {
        String command = request.getParams().get("command");

        String pid = request.getParams().get("pid");
        //以分钟为单位
        Long timeout = CommonUtils.toLong(request.getParams().get("timeout"), 0L);

        boolean timeoutFlag = timeout > 0;

        logger.info("[opencron]:execute:{},pid:{}", command, pid);

        File shellFile = CommandUtils.createShellFile(command,pid,request.getParams().get("runAs"),EXITCODE_SCRIPT);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        final Response response = Response.response(request);

        final ExecuteWatchdog watchdog = new ExecuteWatchdog(Integer.MAX_VALUE);

        final Timer timer = new Timer();

        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

        Integer exitValue;

        String successExit = request.getParams().get("successExit");
        if (CommonUtils.isEmpty(successExit)) {
            exitValue = 0;//标准退住值:0
        }else {
            exitValue = Integer.parseInt(successExit);
        }

        try {

            CommandLine commandLine = CommandLine.parse(String.format("/bin/bash +x %s",shellFile.getAbsoluteFile()));

            final DefaultExecutor executor = new DefaultExecutor();

            ExecuteStreamHandler stream = new PumpStreamHandler(outputStream, outputStream);
            executor.setStreamHandler(stream);
            response.setStartTime(new Date().getTime());
            //成功执行完毕时退出值为0,shell标准的退出
            executor.setExitValue(exitValue);

            if (timeoutFlag) {
                //设置监控狗...
                executor.setWatchdog(watchdog);
                //监控超时的计时器
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        //超时,kill...
                        if (watchdog.isWatching()) {
                            /**
                             * 调用watchdog的destroyProcess无法真正kill进程...
                             * watchdog.destroyProcess();
                             */
                            timer.cancel();
                            watchdog.stop();
                            //call  kill...
                            request.setAction(Action.KILL);
                            try {
                                kill(request);
                                response.setExitCode(Opencron.StatusCode.TIME_OUT.getValue());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }, timeout * 60 * 1000);

                //正常执行完毕则清除计时器
                resultHandler = new DefaultExecuteResultHandler() {
                    @Override
                    public void onProcessComplete(int exitValue) {
                        super.onProcessComplete(exitValue);
                        timer.cancel();
                    }

                    @Override
                    public void onProcessFailed(ExecuteException e) {
                        super.onProcessFailed(e);
                        timer.cancel();
                    }
                };
            }

            executor.execute(commandLine, resultHandler);

            resultHandler.waitFor();

        } catch (Exception e) {
            if (e instanceof ExecuteException) {
                exitValue = ((ExecuteException) e).getExitValue();
            } else {
                exitValue = Opencron.StatusCode.ERROR_EXEC.getValue();
            }
            if (Opencron.StatusCode.KILL.getValue().equals(exitValue)) {
                if (timeoutFlag) {
                    timer.cancel();
                    watchdog.stop();
                }
                logger.info("[opencron]:job has be killed!at pid :{}", request.getParams().get("pid"));
            } else {
                logger.info("[opencron]:job execute error:{}", e.getCause().getMessage());
            }
        } finally {

            exitValue = resultHandler.getExitValue();

            if (CommonUtils.notEmpty(outputStream.toByteArray())) {
                try {
                    outputStream.flush();
                    String text = outputStream.toString();
                    if (notEmpty(text)) {
                        try {
                            text = text.replaceAll(String.format(REPLACE_REX, shellFile.getAbsolutePath()), "");
                            response.setMessage(text.substring(0, text.lastIndexOf(EXITCODE_KEY)));
                            exitValue = Integer.parseInt(text.substring(text.lastIndexOf(EXITCODE_KEY) + EXITCODE_KEY.length() + 1).trim());
                        } catch (IndexOutOfBoundsException e) {
                            response.setMessage(text);
                        }
                    }
                    outputStream.close();
                } catch (Exception e) {
                    logger.error("[opencron]:error:{}", e);
                }
            }

            if (Opencron.StatusCode.TIME_OUT.getValue() == response.getExitCode()) {
                response.setSuccess(false).end();
            } else {
                if (CommonUtils.isEmpty(successExit)) {
                    response.setExitCode(exitValue).setSuccess(exitValue == Opencron.StatusCode.SUCCESS_EXIT.getValue()).end();
                }else {
                    response.setExitCode(exitValue).setSuccess(successExit.equals(exitValue.toString())).end();
                }
            }

        }

        if (CommonUtils.notEmpty(shellFile)) {
            shellFile.delete();
        }

        logger.info("[opencron]:execute result:{}", response.toString());

        watchdog.stop();

        return response;
    }

   // @Override
    public Response password(Request request) {

        String newPassword = request.getParams().get("newPassword");
        Response response = Response.response(request);

        if (isEmpty(newPassword)) {
            return response.setSuccess(false).setExitCode(Opencron.StatusCode.SUCCESS_EXIT.getValue()).setMessage("密码不能为空").end();
        }

        this.password = newPassword.toLowerCase().trim();
        SystemPropertyUtils.setProperty("opencron.password",password);
        IOUtils.writeText(Configuration.OPENCRON_PASSWORD_FILE, password, "UTF-8");
        return response.setSuccess(true).setExitCode(Opencron.StatusCode.SUCCESS_EXIT.getValue()).end();
    }

   // @Override
    public Response kill(Request request) {
        String pid = request.getParams().get("pid");
        logger.info("[opencron]:kill pid:{}", pid);

        Response response = Response.response(request);
        String text = CommandUtils.executeShell(Configuration.OPENCRON_KILL_SHELL, pid, EXITCODE_SCRIPT);
        String message = "";
        Integer exitVal = 0;

        if (notEmpty(text)) {
            try {
                message = text.substring(0, text.lastIndexOf(EXITCODE_KEY));
                exitVal = Integer.parseInt(text.substring(text.lastIndexOf(EXITCODE_KEY) + EXITCODE_KEY.length() + 1).trim());
            } catch (StringIndexOutOfBoundsException e) {
                message = text;
            }
        }

        response.setExitCode(Opencron.StatusCode.ERROR_EXIT.getValue().equals(exitVal) ? Opencron.StatusCode.ERROR_EXIT.getValue() : Opencron.StatusCode.SUCCESS_EXIT.getValue())
                .setMessage(message)
                .end();

        logger.info("[opencron]:kill result:{}" + response);
        return response;
    }

    //@Override
    public Response proxy(Request request) {
        String proxyHost = request.getParams().get("proxyHost");
        String proxyPort = request.getParams().get("proxyPort");
        String proxyAction = request.getParams().get("proxyAction");
        String proxyPassword = request.getParams().get("proxyPassword");

        //其他参数....
        String proxyParams = request.getParams().get("proxyParams");
        Map<String, String> params = new HashMap<String, String>(0);
        if (CommonUtils.notEmpty(proxyParams)) {
            params = (Map<String, String>) JSON.parse(proxyParams);
        }

        Request proxyReq = Request.request(proxyHost, toInt(proxyPort), Action.findByName(proxyAction), proxyPassword).setParams(params);

        logger.info("[opencron]proxy params:{}", proxyReq.toString());

        TTransport transport;
        /**
         * ping的超时设置为5毫秒,其他默认
         */
        if (proxyReq.getAction().equals(Action.PING)) {
            proxyReq.getParams().put("proxy","true");
            transport = new TSocket(proxyReq.getHostName(), proxyReq.getPort(), 1000 * 5);
        } else {
            transport = new TSocket(proxyReq.getHostName(), proxyReq.getPort());
        }
       /* TProtocol protocol = new TBinaryProtocol(transport);
        Opencron.Client client = new Opencron.Client(protocol);
        transport.open();

        Response response = null;
        for (Method method : client.getClass().getMethods()) {
            if (method.getName().equalsIgnoreCase(proxyReq.getAction().name())) {
                try {
                    response = (Response) method.invoke(client, proxyReq);
                } catch (Exception e) {
                    //proxy 执行失败,返回失败信息
                    response = Response.response(request);
                    response.setExitCode(Opencron.StatusCode.ERROR_EXIT.getValue())
                            .setMessage("[opencron]:proxy error:"+e.getLocalizedMessage())
                            .setSuccess(false)
                            .end();
                }
                break;
            }
        }
        transport.flush();
        transport.close();*/
        return null;
    }

   // @Override
    public Response guid(Request request) {
        String macId = null;
        try {
            //多个网卡地址,按照字典顺序把他们连接在一块,用-分割.
            List<String> macIds = MacUtils.getMacAddressList();
            if (CommonUtils.notEmpty(macIds)) {
                TreeSet<String> macSet = new TreeSet<String>(macIds);
                macId = StringUtils.joinString(macSet,"-");
            }
        } catch (IOException e) {
            logger.error("[opencron]:getMac error:{}",e);
        }

        Response response = Response.response(request).end();
        if (notEmpty(macId)) {
            return response.setMessage(macId).setSuccess(true).setExitCode(Opencron.StatusCode.SUCCESS_EXIT.getValue());
        }
        return response.setSuccess(false).setExitCode(Opencron.StatusCode.ERROR_EXIT.getValue());
    }


    /**
     *重启前先检查密码,密码不正确返回Response,密码正确则直接执行重启
     * @param request
     * @return
     * @throws TException
     * @throws InterruptedException
     */
    //@Override
    public void restart(Request request) {

    }

    private Map<String, String> serializableToMap(Object obj) {
        if (isEmpty(obj)) {
            return Collections.EMPTY_MAP;
        }

        Map<String, String> resultMap = new HashMap<String, String>(0);
        // 拿到属性器数组
        try {
            PropertyDescriptor[] pds = Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors();
            for (int index = 0; pds.length > 1 && index < pds.length; index++) {
                if (Class.class == pds[index].getPropertyType() || pds[index].getReadMethod() == null) {
                    continue;
                }
                Object value = pds[index].getReadMethod().invoke(obj);
                if (notEmpty(value)) {
                    if (isPrototype(pds[index].getPropertyType())//java里的原始类型(去除自己定义类型)
                            || pds[index].getPropertyType().isPrimitive()//基本类型
                            //|| ReflectUtils.isPrimitivePackageType(pds[index].getPropertyType())
                            || pds[index].getPropertyType() == String.class) {

                        resultMap.put(pds[index].getName(), value.toString());

                    } else {
                        resultMap.put(pds[index].getName(), JSON.toJSONString(value));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultMap;
    }

    public boolean register() {
        if (CommonUtils.notEmpty(Configuration.OPENCRON_SERVER)) {
            String url = Configuration.OPENCRON_SERVER+"/agent/autoreg.do";
            String mac = MacUtils.getMacAddress();
            String agentPassword = IOUtils.readText(Configuration.OPENCRON_PASSWORD_FILE, "UTF-8").trim().toLowerCase();

            Map<String,Object> params = new HashMap<String, Object>(0);
            params.put("machineId",mac);
            params.put("password",agentPassword);
            params.put("port", Configuration.OPENCRON_PORT);
            params.put("key", Configuration.OPENCRON_REGKEY);

            logger.info("[opencron]agent auto register staring:{}", Configuration.OPENCRON_SERVER);
            try {
                String result = HttpClientUtils.httpPostRequest(url,params);
                if (result==null) {
                    return false;
                }
                JSONObject jsonObject = JSON.parseObject(result);
                if (jsonObject.get("status").toString().equals("200")) {
                    return true;
                }
                logger.error("[opencron:agent auto regsiter error:{}]",jsonObject.get("message"));
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

}