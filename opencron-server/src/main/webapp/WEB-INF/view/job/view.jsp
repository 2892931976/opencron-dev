<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="cron" uri="http://www.opencron.org" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html>
<html lang="en">
<head>

    <style type="text/css">
        .none {
            text-align: center;
        }
    </style>

    <script type="text/javascript" src="${contextPath}/static/js/job.validata.js"></script>

    <script type="text/javascript">

        $(document).ready(function () {

            window.opencronValidata = new Validata('${contextPath}');

            $("#redo1").next().on("click",toggle.count.show);
            $("#redo0").next().on("click",toggle.count.hide);
            $("#warning1").next().on("click",toggle.contact.show);
            $("#warning0").next().on("click",toggle.contact.hide);

            $("#command").blur(function () {
                if ($("#command").val().length == 0) {
                    opencron.tipError("#command", "执行命令不能为空,请填写执行命令");
                } else {
                    opencron.tipOk("#command");
                }
            }).focus(function () {
                opencron.tipDefault("#command");
            });

            $("#runCount").blur(function () {
                var redo =  $('input[type="radio"][name="itemRedo"]:checked').val();
                var reg = /^[0-9]*[1-9][0-9]*$/;
                if (redo == 1) {
                    var _runCount = $("#runCount").val();
                    if (!_runCount) {
                        opencron.tipError("#runCount", "请填写重跑次数!");
                        this.status = false;
                    } else if (!reg.test(_runCount)) {
                        opencron.tipError("#runCount", "截止重跑次数必须为正整数!");
                        this.status = false;
                    } else {
                        opencron.tipOk("#runCount");
                    }
                }
            });

            $("#size").change(function () {
                jobObj.changeUrl();
            });
            $("#sagentId").change(function () {
                jobObj.changeUrl();
            });
            $("#cronType").change(function () {
                jobObj.changeUrl();
            });
            $("#jobType").change(function () {
                jobObj.changeUrl();
            });
            $("#sredo").change(function () {
                jobObj.changeUrl();
            });
        });

        var toggle = {
            count:{
                show:function () {
                    $(".countDiv1").show();
                },
                hide:function () {
                    $(".countDiv1").hide();
                }
            },
            contact:{
                show:function () {
                    $(".contact").show()
                },
                hide:function () {
                    $(".contact").hide()
                }
            }
        };

        var jobObj = {
            save:function (job) {
                var jobId = $("#id").val();
                var jobVerify = new Validata('${contextPath}',jobId);
                jobVerify.validata.init();
                jobVerify.validata.jobName();
                jobVerify.validata.cronExp();
                jobVerify.validata.command();
                jobVerify.validata.successExit();
                jobVerify.validata.timeout();
                jobVerify.validata.runCount();
                jobVerify.validata.warning();
                var valId = setInterval(function () {
                    if (jobVerify.validata.jobNameRemote ) {
                        window.clearInterval(valId);
                        if (jobVerify.validata.status) {
                            var jobName = $("#jobName").val();
                            var jobId = $("#id").val();
                            var agentId = $("#agentId").val();
                            var cronType = $('input[type="radio"][name="cronType"]:checked').val();
                            var cronExp = $("#cronExp").val();
                            var command = $("#cmd").val();
                            var successExit = $("#successExit").val();
                            var timeout = $("#timeout").val();
                            var redo = $('input[type="radio"][name="redo"]:checked').val();
                            var runCount = $("#runCount").val();
                            var warning = $('input[type="radio"][name="warning"]:checked').val();
                            var jobData = {
                                "id": jobId,
                                "name": jobName,
                                "jobId": jobId,
                                "cronType": cronType,
                                "cronExp": cronExp,
                                "agentId": agentId,
                                "command": toBase64(command),
                                "runAs":$("#runAs").val(),
                                "successExit":$("#successExit").val(),
                                "timeout": timeout,
                                "jobName": jobName,
                                "redo": redo,
                                "runCount": runCount,
                                "warning": warning,
                                "mobiles": $("#mobiles").val(),
                                "emailAddress": $("#email").val(),
                                "comment": $("#comment").val()
                            };
                            ajax({
                                type: "post",
                                url: "${contextPath}/job/edit.do",
                                data: jobData
                            },function (data) {
                                if (data.status) {
                                    $('#jobModal').modal('hide');
                                    alertMsg("修改成功");
                                    $("#jobName_" + job.jobId).html(escapeHtml(job.jobName));
                                    $("#command_" + job.jobId).html(escapeHtml(passBase64(job.command)));

                                    $("#cronType_" + job.jobId).html(job.cronType == "0" ? '<img class="text-center" width="70px" src="${contextPath}/static/img/crontab_ico.png">' : '<img  class="text-center" width="70px" src="${contextPath}/static/img/quartz_ico.png">');
                                    $("#cronExp_" + job.jobId).html(escapeHtml(job.cronExp));

                                    if (job.redo == "0") {
                                        $("#redo_" + job.jobId).html('<span color="green">否</span>');
                                    } else {
                                        $("#redo_" + job.jobId).html('<span color="red">是</span>');
                                    }
                                    $("#runCount_" + job.jobId).html(job.runCount);
                                } else {
                                    alert("修改失败");
                                }
                                return false;
                            });
                        }
                    }
                },10);
            },

            edit:function (id) {
                ajax({
                    type: "post",
                    url: "${contextPath}/job/editsingle.do",
                    data: {"id": id}
                },function (obj) {
                    $("#jobform")[0].reset();
                    $("#jobModal").find(".ok").remove();
                    if (obj != null) {
                        $("#id").val(obj.jobId);
                        $("#agentId").val(obj.agentId);
                        $("#jobName").val(unEscapeHtml(obj.jobName));
                        $("#agent").val(obj.agentName + "   " + obj.host);
                        $("#cronExp").val(obj.cronExp);
                        $("#cmd").val(obj.command);
                        $(".cronExpDiv").find(".tips").css("visibility","hidden");
                        if (obj.cronType == 1) {
                            $("#cronType1").prop("checked", true);
                            $("#cronType1").parent().removeClass("checked").addClass("checked");
                            $("#cronType1").parent().attr("aria-checked", true);
                            $("#cronType0").parent().removeClass("checked");
                            $("#cronType0").parent().attr("aria-checked", false);
                        } else {
                            $("#cronType0").prop("checked", true);
                            $("#cronType0").parent().removeClass("checked").addClass("checked");
                            $("#cronType0").parent().attr("aria-checked", true);
                            $("#cronType1").parent().removeClass("checked");
                            $("#cronType1").parent().attr("aria-checked", false);
                        }
                        console.log(obj.redo);
                        if (obj.redo == 1) {
                            toggle.count.show();
                            $("#redo1").prop("checked", true);
                            $("#redo1").parent().removeClass("checked").addClass("checked");
                            $("#redo1").parent().attr("aria-checked", true);
                            $("#redo0").parent().removeClass("checked");
                            $("#redo0").parent().attr("aria-checked", false);
                        } else {
                            toggle.count.hide();
                            $("#redo0").prop("checked", true);
                            $("#redo0").parent().removeClass("checked").addClass("checked");
                            $("#redo0").parent().attr("aria-checked", true);
                            $("#redo1").parent().removeClass("checked");
                            $("#redo1").parent().attr("aria-checked", false);
                        }
                        $("#runCount").val(obj.runCount);
                        if (obj.warning == true) {
                            toggle.contact.show();
                            $("#warning1").prop("checked", true);
                            $("#warning1").parent().removeClass("checked").addClass("checked");
                            $("#warning1").parent().attr("aria-checked", true);
                            $("#warning1").parent().bind("click",toggle.contact.show);
                            $("#warning0").parent().removeClass("checked");
                            $("#warning0").parent().attr("aria-checked", false);
                        } else {
                            toggle.contact.hide();
                            $("#warning0").prop("checked", true);
                            $("#warning0").parent().removeClass("checked").addClass("checked");
                            $("#warning0").parent().attr("aria-checked", true);
                            $("#warning1").parent().removeClass("checked");
                            $("#warning1").parent().attr("aria-checked", false);
                        }
                        $("#mobiles").val(obj.mobiles);
                        $("#email").val(obj.emailAddress);
                        $("#comment").val(unEscapeHtml(obj.comment));
                        $("#runAs").val(obj.runAs);
                        $("#successExit").val(obj.successExit);
                        $("#timeout").val(obj.timeout);
                        $('#jobModal').modal('show');
                        return;
                    }
                });
            },
            changeUrl:function () {
                var pageSize = $("#size").val();
                var agentId = $("#sagentId").val();
                var cronType = $("#cronType").val();
                var jobType = $("#jobType").val();
                var redo = $("#sredo").val();
                window.location.href = "${contextPath}/job/view.htm?agentId=" + agentId + "&cronType=" + cronType + "&jobType=" + jobType + "&redo=" + redo + "&pageSize=" + pageSize ;
            },
            pauseJob:function (id,status) {
                var msg = status?"暂停":"恢复";
                swal({
                    title: "",
                    text: "您确定要"+msg+"这个作业吗？",
                    type: "warning",
                    showCancelButton: true,
                    closeOnConfirm: false,
                    confirmButtonText: msg
                }, function () {
                    ajax({
                        type: "post",
                        url: "${contextPath}/job/pause.do",
                        data: {
                            "jobId": id,
                            "pause":status
                        }
                    },function (data) {
                        var pauseElem = $("#pause_"+id);
                        if(data.status) {
                            if (status){
                                pauseElem.attr("title","恢复");
                                pauseElem.click(function () {
                                    pauseJob(id,false);
                                });
                                pauseElem.find("i").removeClass("fa-pause-circle-o").addClass("fa-history");
                            }else {
                                pauseElem.attr("title","暂停");
                                pauseElem.click(function () {
                                    pauseJob(id,true);
                                });
                                pauseElem.find("i").addClass("fa-pause-circle-o").removeClass("fa-history");
                            }
                            alertMsg(msg+"成功!");
                        }
                    })
                });
            },
            executeJob:function (id) {
                ajax({
                    type: "post",
                    url: "${contextPath}/job/running.do",
                    data: {"id": id}
                },function (data) {
                    swal({
                        title: "",
                        text: data.status?"该作业已经在运作中,您确定要再次执行吗?":"您确定要执行这个作业吗",
                        type: "warning",
                        showCancelButton: true,
                        closeOnConfirm: false,
                        confirmButtonText:data.status?"再次执行":"执行"
                    }, function () {
                        ajax({
                            type: "post",
                            url: "${contextPath}/job/execute.do",
                            data: {"id": id}
                        },function (data) {
                            if (data.status) {
                                alertMsg("该作业已启动,正在执行中.");
                            }else {
                                alertMsg("执行作业错误!")
                            }
                        });

                    });
                });
            },
            showChild:function (id, flowId) {
                var open = $("#job_" + id).attr("childOpen");
                if (open == "off") {
                    $("#icon" + id).removeClass("fa-angle-double-down").addClass("fa-angle-double-up");
                    $(".child" + id).show();
                    $(".trGroup" + flowId).css("background-color", "rgba(0,0,0,0.1)");
                    $("#job_" + id).attr("childOpen", "on");
                    $(".name_" + id + "_1").hide();
                    $(".name_" + id + "_2").show();
                } else {
                    $(".trGroup" + flowId).css("background-color", "");
                    $("#icon" + id).removeClass("fa-angle-double-up").addClass("fa-angle-double-down");
                    $(".child" + id).hide();
                    $("#job_" + id).attr("childOpen", "off");
                    $(".name_" + id + "_2").hide();
                    $(".name_" + id + "_1").show();
                }
            },
            editCmd:function (id) {
                $("#command").parent().find(".tips").css("visibility","hidden");
                $("#command").parent().find(".ok").remove();
                ajax({
                    type: "post",
                    url: "${contextPath}/job/editsingle.do",
                    data: {"id": id}
                },function (obj) {
                    $("#cmdform")[0].reset();
                    if (obj != null) {
                        $("#cmdId").val(obj.jobId);
                        $("#command").val(obj.command);
                        $('#cmdModal').modal('show');
                    }
                });
            },
            saveCmd:function () {
                var jobId = $("#cmdId").val();
                if (!jobId) {
                    alert("页面异常，请刷新重试!");
                    return false;
                }
                var command = $("#command").val();
                if (command.length == 0) {
                    opencron.tipError("#command", "执行命令不能为空,请填写执行命令");
                    return false;
                } else {
                    ajax({
                        type: "post",
                        url: "${contextPath}/job/editcmd.do",
                        data: {
                            "jobId": jobId,
                            "command": toBase64(command)
                        }
                    },function (data) {
                        if (data.status) {
                            $('#cmdModal').modal('hide');
                            alertMsg("修改成功");
                            $("#command_" + jobId).attr("title", command);
                            if (command.length > 50) {
                                command = command.substring(0, 50) + "...";
                            }
                            $("#command_" + jobId).html(escapeHtml(command));
                        } else {
                            alert("修改失败");
                        }
                    });
                }
            },

            remove:function (id) {
                swal({
                    title: "",
                    text: "确定要删除该作业吗？",
                    type: "warning",
                    showCancelButton: true,
                    closeOnConfirm: false,
                    confirmButtonText: "删除"
                }, function () {
                    ajax({
                        type: "post",
                        url: "${contextPath}/job/checkdel.do",
                        data: {"id": id}
                    },function (data) {
                        if (data == "error") {
                            alert("该作业不存在,删除失败!")
                        } else if (data == "false") {
                            alert("该作业正在运行中,删除失败!")
                        } else {
                            ajax({
                                type: "post",
                                url: "${contextPath}/job/delete.do",
                                data: {"id": id}
                            },function (data) {
                                if (data.status) {
                                    alertMsg("删除作业成功");
                                    location.reload();
                                } else {
                                    alertMsg("删除作业失败");
                                }
                            });
                        }
                    })
                });
            }
        }

    </script>
</head>

<body>

<section id="content" class="container">

    <!-- Messages Drawer -->
    <jsp:include page="/WEB-INF/layouts/message.jsp"/>

    <!-- Breadcrumb -->
    <ol class="breadcrumb hidden-xs">
        <li class="icon">&#61753;</li>
        当前位置：
        <li><a href="">opencron</a></li>
        <li><a href="">作业管理</a></li>
        <li><a href="">作业列表</a></li>
    </ol>
    <h4 class="page-title"><i class="fa fa-tasks" aria-hidden="true"></i>&nbsp;作业列表</h4>

    <!-- Deafult Table -->
    <div class="block-area" id="defaultStyle">
        <div>
            <div style="float: left">
                <label>
                    每页 <select size="1" class="select-opencron" id="size" style="width: 50px;">
                    <option value="15">15</option>
                    <option value="30" ${pageBean.pageSize eq 30 ? 'selected' : ''}>30</option>
                    <option value="50" ${pageBean.pageSize eq 50 ? 'selected' : ''}>50</option>
                    <option value="100" ${pageBean.pageSize eq 100 ? 'selected' : ''}>100</option>
                </select> 条记录
                </label>
            </div>

            <div style="float: right;margin-top: -9px">
                <label for="sagentId">执行器：</label>
                <select id="sagentId" name="sagentId" class="select-opencron" style="width: 110px;">
                    <option value="">全部</option>
                    <c:forEach var="d" items="${agents}">
                        <option value="${d.agentId}" ${d.agentId eq agentId ? 'selected' : ''}>${d.name}</option>
                    </c:forEach>
                </select>

                &nbsp;&nbsp;&nbsp;
                <label for="cronType">规则类型：</label>
                <select id="cronType" name="cronType" class="select-opencron" style="width: 80px;">
                    <option value="">全部</option>
                    <option value="0" ${cronType eq 0 ? 'selected' : ''}>crontab</option>
                    <option value="1" ${cronType eq 1 ? 'selected' : ''}>quartz</option>
                </select>

                &nbsp;&nbsp;&nbsp;
                <label for="jobType">作业类型：</label>
                <select id="jobType" name="jobType" class="select-opencron" style="width: 90px;">
                    <option value="">全部</option>
                    <option value="0" ${jobType eq 0 ? 'selected' : ''}>单一作业</option>
                    <option value="1" ${jobType eq 1 ? 'selected' : ''}>流程作业</option>
                </select>

                <label for="sredo">重跑：</label>
                <select id="sredo" name="sredo" class="select-opencron" style="width: 80px;">
                    <option value="">全部</option>
                    <option value="1" ${redo eq 1 ? 'selected' : ''}>是</option>
                    <option value="0" ${redo eq 0 ? 'selected' : ''}>否</option>
                </select>

                <a href="${contextPath}/job/add.htm" class="btn btn-sm m-t-10"
                   style="margin-left: 20px;margin-bottom: 8px;margin-top: -3px;"><i class="icon">&#61943;</i>添加</a>
            </div>
        </div>

        <table class="table tile textured" style="font-size: 13px;">
            <thead>
            <tr>
                <th>名称</th>
                <th>执行器</th>
                <th>作业人</th>
                <th>执行命令</th>
                <th>作业类型</th>
                <th>规则类型</th>
                <th>时间规则</th>
                <th class="text-center">
                     <i class="icon-time bigger-110 hidden-480"></i>操作
                </th>
            </tr>
            </thead>
            <tbody>
            <%--父作业--%>
            <c:forEach var="r" items="${pageBean.result}" varStatus="index">
                <tr class="trGroup${r.flowId}">
                    <c:if test="${r.jobType eq 0}">
                        <td id="jobName_${r.jobId}" title="${r.jobName}">${cron:substr(r.jobName, 0,20 ,"..." )}</td>
                    </c:if>
                    <c:if test="${r.jobType eq 1}">
                        <td class="name_${r.flowId}_1" title="${r.jobName}">${cron:substr(r.jobName, 0,20 ,"..." )}</td>
                        <td style="display: none;"
                            class="name_${r.flowId}_2"
                            rowspan="${fn:length(r.children)+1}"
                            title="${r.jobName}">${cron:substr(r.jobName, 0,20 ,"..." )}

                            <c:forEach var="c" items="${r.children}" varStatus="index">
                                <div class="down">
                                    <i aria-hidden="true" style="font-size:14px" class="fa fa-arrow-down"></i>
                                </div>
                                <span title="${c.jobName}">${cron:substr(c.jobName, 0,20 ,"..." )}</span>
                            </c:forEach>

                        </td>
                    </c:if>
                    <td><a href="${contextPath}/agent/detail/${r.agentId}.htm">${r.agentName}</a></td>
                    <c:if test="${permission eq true}">
                        <td><a href="${contextPath}/user/detail/${r.userId}.htm">${r.operateUname}</a>
                        </td>
                    </c:if>
                    <c:if test="${permission eq false}">
                        <td>${r.operateUname}</td>
                    </c:if>
                    <td style="width: 25%">
                        <div class="opencron_command">
                            <a href="#" title="${cron:escapeHtml(r.command)}" class="dot-ellipsis dot-resize-update" onclick="jobObj.editCmd('${r.jobId}')" id="command_${r.jobId}">
                                    ${cron:escapeHtml(r.command)}
                            </a>
                        </div>
                    </td>
                    <td>
                        <c:if test="${r.jobType eq 0}">单一作业</c:if>
                        <c:if test="${r.jobType eq 1}">流程作业</c:if>
                    </td>

                    <td id="cronType_${r.jobId}">
                        <c:if test="${r.cronType eq 0}">
                            <img class="text-center" width="70px" src="${contextPath}/static/img/crontab_ico.png">
                        </c:if>
                        <c:if test="${r.cronType eq 1}">
                            <img class="text-center" width="70px" src="${contextPath}/static/img/quartz_ico.png">
                        </c:if>
                    </td>

                    <td id="cronExp_${r.jobId}">
                        ${r.cronExp}
                    </td>
                    <td class="text-center">
                        <div class="visible-md visible-lg hidden-sm hidden-xs action-buttons">
                            <c:if test="${r.jobType eq 1}">
                                <a href="#" title="流程作业" id="job_${r.jobId}" childOpen="off" onclick="jobObj.showChild('${r.jobId}','${r.flowId}')"> <i style="font-size:14px;" class="fa fa-angle-double-down" id="icon${r.jobId}"></i></a>&nbsp;&nbsp;
                            </c:if>
                            <c:if test="${r.jobType eq 0}">
                                <a href="#" title="编辑" onclick="jobObj.edit('${r.jobId}')">
                                    <i class="glyphicon glyphicon-pencil"></i>
                                </a>
                            </c:if>

                            <c:if test="${r.jobType eq 1}">
                                <a title="编辑" href="${contextPath}/job/editflow.htm?id=${r.jobId}">
                                    <i class="glyphicon glyphicon-pencil"></i>
                                </a>
                            </c:if>&nbsp;

                            <c:choose>
                                <c:when test="${r.pause eq null or r.pause eq false}">
                                <span>
                                <a id="pause_${r.jobId}" href="#" title="暂停" onclick="jobObj.pauseJob('${r.jobId}',true)">
                                   <i aria-hidden="true" class="fa fa-pause-circle-o"></i>
                                </a>
                                </span>
                                </c:when>
                                <c:otherwise>
                               <span>
                                <a id="pause_${r.jobId}" href="#" title="恢复" onclick="jobObj.pauseJob('${r.jobId}',false)">
                                   <i aria-hidden="true" class="fa fa-history"></i>
                                </a>
                                </span>
                                </c:otherwise>
                            </c:choose>
                            &nbsp;

                            <span id="execButton_${r.jobId}">
                                <a href="#" title="执行" onclick="jobObj.executeJob('${r.jobId}')">
                                   <i aria-hidden="true" class="fa fa-play"></i>
                                </a>
                            </span>&nbsp;

                            <a href="#" onclick="jobObj.remove('${r.jobId}')" title="删除">
                                <i aria-hidden="true" class="fa fa-times"></i>
                            </a>&nbsp;

                            <a href="${contextPath}/job/detail/${r.jobId}.htm" title="查看详情">
                                <i class="glyphicon glyphicon-eye-open"></i>
                            </a>
                        </div>
                    </td>
                </tr>
                <%--子作业--%>
                <c:if test="${r.jobType eq 1}">
                    <c:forEach var="c" items="${r.children}" varStatus="index">
                        <tr class="child${r.jobId} trGroup${r.flowId}" style="display: none;">
                            <td><a href="${contextPath}/agent/detail/${c.agentId}.htm">${c.agentName}</a></td>
                            <c:if test="${permission eq true}">
                                <td>
                                    <a href="${contextPath}/user/detail/${c.userId}.htm">${c.operateUname}</a>
                                </td>
                            </c:if>
                            <c:if test="${permission eq false}">
                                <td>${c.operateUname}</td>
                            </c:if>
                            <td style="width: 25%">
                                <div class="opencron_command">
                                    <a href="#" title="${cron:escapeHtml(c.command)}" class="dot-ellipsis dot-resize-update" onclick="jobObj.editCmd('${c.jobId}')" id="command_${c.jobId}">
                                            ${cron:escapeHtml(c.command)}
                                    </a>
                                </div>
                            </td>
                            <td class="text-center">
                                流程作业
                            </td>
                            <td>
                                <div class="none">--</div>
                            </td>
                            <td>
                                <div class="none">--</div>
                            </td>
                            <td>
                                <div class="none">--</div>
                            </td>
                            <td class="text-center">
                                <div class="visible-md visible-lg hidden-sm hidden-xs action-buttons">
                                    <a href="${contextPath}/job/detail/${c.jobId}.htm" title="查看详情">
                                        <i class="glyphicon glyphicon-eye-open"></i>
                                    </a>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                </c:if>
            </c:forEach>
            </tbody>
        </table>

        <cron:pager href="${contextPath}/job/view.htm?agentId=${agentId}&redo=${redo}" id="${pageBean.pageNo}" size="${pageBean.pageSize}" total="${pageBean.totalCount}"/>

    </div>

    <div class="modal fade" id="jobModal" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button class="close btn-float" data-dismiss="modal" aria-hidden="true"><i class="md md-close"></i>
                    </button>
                    <h4 id="subTitle" action="add" tid="">编辑作业</h4>
                </div>
                <div class="modal-body">
                    <form class="form-horizontal" role="form" id="jobform">
                        <input type="hidden" id="redo" value="1"/>
                        <input type="hidden" id="id">
                        <input type="hidden" name="agentId" id="agentId">
                        <div class="form-group">
                            <label for="agent" class="col-lab control-label wid100" title="要执行此作业的机器名称和IP地址">执&nbsp;&nbsp;&nbsp;行&nbsp;&nbsp;器&nbsp;&nbsp;</label>
                            <div class="col-md-9">
                                <input type="text" class="form-control " id="agent" readonly>&nbsp;
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="jobName" class="col-lab control-label wid100" title="作业名称必填">作业名称&nbsp;<b>*</b></label>
                            <div class="col-md-9">
                                <input type="text" class="form-control " id="jobName">
                                <span class="tips none" tip="必填项,该作业的名称">必填项,该作业的名称</span>
                            </div>
                        </div>
                        <div class="form-group cronExpDiv">
                            <label class="col-lab control-label wid100">规则类型&nbsp;&nbsp;&nbsp;</label>
                            <div class="col-md-9">
                                <label for="cronType0" class="radio-label"><input type="radio" name="cronType" value="0" id="cronType0" checked>crontab&nbsp;&nbsp;&nbsp;</label>
                                <label for="cronType1" class="radio-label"><input type="radio" name="cronType" value="1" id="cronType1">quartz</label>&nbsp;&nbsp;&nbsp;
                                </br><span class="tips none" id="cronTip" tip="crontab: unix/linux的时间格式表达式">crontab: unix/linux的时间格式表达式</span>
                            </div>
                        </div>
                        <div class="form-group cronExpDiv">
                            <label for="cronExp" class="col-lab control-label wid100">时间规则&nbsp;<b>*</b></label>
                            <div class="col-md-9">
                                <input type="text" class="form-control" id="cronExp" name="cronExp">
                                <span class="tips none" id="expTip" tip="请采用unix/linux的时间格式表达式,如 00 01 * * *">请采用unix/linux的时间格式表达式,如 00 01 * * *</span>
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="cmd" class="col-lab control-label wid100" title="请采用unix/linux的shell支持的命令">执行命令&nbsp;<b>*</b></label>
                            <div class="col-md-9">
                                <textarea class="form-control" id="cmd" name="cmd" style="height:100px;resize:vertical"></textarea>
                                <span class="tips none" tip="请采用unix/linux的shell支持的命令">请采用unix/linux的shell支持的命令</span>
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="runAs" class="col-lab control-label wid100">运行身份&nbsp;<b>*</b></label>
                            <div class="col-md-9">
                                <input type="text" class="form-control" id="runAs" name="runAs" value="root">
                                <span class="tips none" tip="该任务以哪个身份执行(默认是root)">该任务以哪个身份执行(默认是root)</span>
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="successExit" class="col-lab control-label wid100">成功标识&nbsp;<b>*</b></label>
                            <div class="col-md-9">
                                <input type="text" class="form-control" id="successExit" value="0">
                                <span class="tips none" tip="自定义作业执行成功的返回标识(默认执行成功是0)">自定义作业执行成功的返回标识(默认执行成功是0)</span>
                            </div>
                        </div>
                        <div class="form-group">
                            <label class="col-lab control-label wid100" title="执行失败时是否自动重新执行">失败重跑&nbsp;&nbsp;&nbsp;</label>&nbsp;&nbsp;
                            <label for="redo1" class="radio-label"><input value="1" type="radio" name="redo" id="redo1" checked> 是&nbsp;&nbsp;&nbsp;</label>
                            <label for="redo0" class="radio-label"><input value="0" type="radio" name="redo" id="redo0">否</label><br>
                        </div>
                        <div class="form-group countDiv1">
                            <label for="runCount" class="col-lab control-label wid100" title="执行失败时自动重新执行的截止次数">重跑次数&nbsp;&nbsp;&nbsp;</label>&nbsp;&nbsp;
                            <div class="col-md-9">
                                <input type="text" class="form-control " id="runCount"/>
                                <span class="tips none" tip="执行失败时自动重新执行的截止次数">执行失败时自动重新执行的截止次数</span>
                            </div>
                        </div>
                        <div class="form-group" style="margin-top: 0px;margin-bottom: 22px">
                            <label class="col-lab control-label" title="任务执行失败时是否发信息报警">失败报警：</label>&nbsp;&nbsp;
                            <label onclick="toggle.contact.show()" for="warning1" class="radio-label"><input type="radio" name="warning" value="1" id="warning1">是&nbsp;&nbsp;&nbsp;</label>
                            <label onclick="toggle.contact.hide()" for="warning0" class="radio-label"><input type="radio" name="warning" value="0" id="warning0">否</label>
                        </div>
                        <div class="form-group contact">
                            <label for="mobiles" class="col-lab control-label" title="任务执行失败时将发送短信给此手机">报警手机：</label>
                            <div class="col-md-9">
                                <input type="text" class="form-control " id="mobiles"/>&nbsp;
                            </div>
                        </div>
                        <div class="form-group contact">
                            <label for="email" class="col-lab control-label" title="任务执行失败时将发送报告给此邮箱">报警邮箱：</label>
                            <div class="col-md-9">
                                <input type="text" class="form-control " id="email"/>&nbsp;
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="timeout" class="col-lab control-label wid100">超时时间&nbsp;<b>*</b></label>
                            <div class="col-md-9">
                                <input type="text" class="form-control" id="timeout" value="0">
                                <span class="tips none" tip="执行作业允许的最大时间,超过则为超时(0:忽略超时时间,分钟为单位)">执行作业允许的最大时间,超过则为超时(0:忽略超时时间,分钟为单位)</span>
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="comment" class="col-lab control-label wid100" title="此作业内容的描述">描&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;述&nbsp;&nbsp;&nbsp;</label>&nbsp;&nbsp;
                            <div class="col-md-9">
                                <input type="text" class="form-control " id="comment"/>&nbsp;
                            </div>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <center>
                        <button type="button" class="btn btn-sm" id="edit-btn" onclick="jobObj.save()">保存</button>&nbsp;&nbsp;
                        <button type="button" class="btn btn-sm" data-dismiss="modal">关闭</button>
                    </center>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="cmdModal" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button class="close btn-float" data-dismiss="modal" aria-hidden="true"><i class="md md-close"></i></button>
                    <h4>修改命令</h4>
                </div>
                <div class="modal-body">
                    <form class="form-horizontal" role="form" id="cmdform">
                        <input type="hidden" id="cmdId">
                        <div class="form-group">
                            <label for="command" class="col-lab control-label" title="请采用unix/linux的shell支持的命令">执行命令：</label>
                            <div class="col-md-9">
                                <textarea class="form-control " id="command" name="command" style="height: 120px;"></textarea>&nbsp;
                                <span class="tips none" tip="请采用unix/linux的shell支持的命令">请采用unix/linux的shell支持的命令</span>
                            </div>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <center>
                        <button type="button" class="btn btn-sm" onclick="jobObj.saveCmd()">保存</button>&nbsp;&nbsp;
                        <button type="button" class="btn btn-sm" data-dismiss="modal">关闭</button>
                    </center>
                </div>
            </div>
        </div>
    </div>

</section>

</body>

</html>






