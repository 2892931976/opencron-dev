
function Validata() {

    this.contextPath = arguments[0]||'';
    this.jobId = arguments[1]||null;

    var self = this;

    this.validata =  {

        status: true,

        jobNameRemote:false,

        cronExpRemote:false,

        init: function () {
            this.status = true;
            this.jobNameRemote = false;
            this.cronExpRemote = false;
        },

        jobName: function () {
            var prefix = arguments[0] || "";
            var _jobName = $("#jobName" + prefix).val();
            if (!_jobName) {
                opencron.tipError("#jobName" + prefix, "必填项,作业名称不能为空");
                this.status = false;
            } else {
                if (_jobName.length < 4 || _jobName.length > 17) {
                    opencron.tipError("#jobName" + prefix, "作业名称不能小于4个字符并且不能超过16个字符!");
                    this.status = false;
                } else {
                    var _this = this;
                    $.ajax({
                        type: "POST",
                        url: self.contextPath+"/job/checkname.do",
                        data: {
                            "jobId":self.jobId,
                            "name": _jobName,
                            "agentId": $("#agentId").val()
                        }
                    }).done(function (data) {
                        _this.jobNameRemote = true;
                        if (!data) {
                            opencron.tipError("#jobName" + prefix, "作业名称已存在!");
                            _this.status = false;
                        } else {
                            opencron.tipOk("#jobName" + prefix);
                        }
                    }).fail(function () {
                        _this.jobNameRemote = true;
                        _this.status = false;
                        opencron.tipError("#jobName" + prefix, "网络请求错误,请重试!");
                    });
                }
            }
        },

        cronExp: function () {
            var cronType = $('input[type="radio"][name="cronType"]:checked').val();

            if ( !arguments[0] && cronType == 0) {
                $("#cronTip").css("visibility","visible").html("crontab: unix/linux的时间格式表达式 ");
            }
            if ( !arguments[0] && cronType == 1) {
                $("#cronTip").css("visibility","visible").html('quartz: quartz框架的时间格式表达式');
            }

            var cronExp = $("#cronExp").val();
            if (!cronExp) {
                opencron.tipError($("#cronExp"),"时间规则不能为空,请填写时间规则");
                this.status = false;
            } else {
                var _this = this;
                $.ajax({
                    type: "POST",
                    url: self.contextPath+"/verify/exp.do",
                    data: {
                        "cronType": cronType,
                        "cronExp": cronExp
                    },
                    dataType:"json"
                }).done(function (data) {
                    _this.cronExpRemote = true;
                    if (data.status) {
                        opencron.tipOk($("#expTip"));
                    } else {
                        self.toggle.cronTip(cronType);
                        $("#expTip").css("visibility","visible");
                        opencron.tipError($("#expTip"), "时间规则语法错误!");
                        _this.status = false;
                    }
                }).fail(function () {
                    _this.cronExpRemote = true;
                    opencron.tipError($("#expTip"), "网络请求错误,请重试!");
                    _this.status = false;
                });
            }
        },

        command: function () {
            var prefix = arguments[0] || "";
            if ($("#cmd" + prefix).val().length == 0) {
                opencron.tipError("#cmd" + prefix, "执行命令不能为空,请填写执行命令");
                this.status = false;
            } else {
                opencron.tipOk("#cmd" + prefix);
            }
        },

        successExit: function () {
            var prefix = arguments[0] || "";
            var successExit = $("#successExit" + prefix).val();
            if (successExit.length == 0) {
                opencron.tipError("#successExit" + prefix, "自定义成功标识不能为空");
                this.status = false;
            } else if (isNaN(successExit)) {
                opencron.tipError("#successExit" + prefix, "自定义成功标识必须为数字");
                this.status = false;
            } else {
                opencron.tipOk("#successExit" + prefix);
            }
        },

        runCount: function () {
            var prefix = arguments[0] || "";
            var redo = prefix ? $("#itemRedo").val() : $('input[type="radio"][name="redo"]:checked').val();
            var reg = /^[0-9]*[1-9][0-9]*$/;
            if (redo == 1) {
                var _runCount = $("#runCount" + prefix).val();
                if (!_runCount) {
                    opencron.tipError("#runCount" + prefix, "请填写重跑次数!");
                    this.status = false;
                } else if (!reg.test(_runCount)) {
                    opencron.tipError("#runCount" + prefix, "截止重跑次数必须为正整数!");
                    this.status = false;
                } else {
                    opencron.tipOk("#runCount" + prefix);
                }
            }
        },

        subJob: function () {
            if ($('input[name="jobType"]:checked').val() == 1) {
                if ($("#subJobDiv:has(li)").length == 0) {
                    opencron.tipError($("#jobTypeTip"), "当前是流程作业,请至少添加一个子作业!");
                    this.status = false;
                }
            }
        },

        mobiles: function () {
            var mobiles = $("#mobiles").val();
            if (!mobiles) {
                opencron.tipError("#mobiles", "请填写手机号码!");
                this.status = false;
                return;
            }
            var mobs = mobiles.split(",");
            var verify = true;
            for (var i in mobs) {
                if (!opencron.testMobile(mobs[i])) {
                    this.status = false;
                    verify = false;
                    opencron.tipError("#mobiles", "请填写正确的手机号码!");
                    break;
                }
            }
            if (verify) {
                opencron.tipOk("#mobiles");
            }
        },

        email: function () {
            var emails = $("#email").val();
            if (!emails) {
                opencron.tipError("#email", "请填写邮箱地址!");
                this.status = false;
                return;
            }
            var emas = emails.split(",");
            var verify = true;
            for (var i in emas) {
                if (!opencron.testEmail(emas[i])) {
                    opencron.tipError("#email", "请填写正确的邮箱地址!");
                    this.status = false;
                    verify = false;
                    break;
                }
            }
            if (verify) {
                opencron.tipOk("#email");
            }
        },

        timeout: function () {
            var prefix = arguments[0] || "";
            var timeout = $("#timeout" + prefix).val();
            if (timeout.length > 0) {
                if (isNaN(timeout) || parseInt(timeout) < 0) {
                    opencron.tipError("#timeout" + prefix, "超时时间必须为正整数,请填写正确的超时时间!");
                    this.status = false;
                } else {
                    opencron.tipOk("#timeout" + prefix);
                }
            } else {
                this.status = false;
                opencron.tipError("#timeout" + prefix, "超时时间不能为空,请填写(0:忽略超时时间,分钟为单位!");
            }
        },

        warning: function () {
            var _warning = $('input[type="radio"][name="warning"]:checked').val();
            if (_warning == 1) {
                this.mobiles();
                this.email();
            }
        },

        verify: function () {
            this.init();
            this.jobName();
            this.cronExp();
            this.command();
            this.successExit();
            this.runCount();
            this.subJob();
            this.timeout();
            this.warning();
            return this.status;
        }
    };

    this.subJob = {

        tipDefault: function () {
            opencron.tipDefault("#jobName1");
            opencron.tipDefault("#cmd1");
            opencron.tipDefault("#successExit1");
            opencron.tipDefault("#timeout1");
            opencron.tipDefault("#runCount1");
        },

        add: function () {
            $("#subForm")[0].reset();
            self.toggle.redo(1);
            this.tipDefault();
            $("#subTitle").html("添加子作业").attr("action", "add");
        },

        edit: function (id) {
            this.tipDefault();
            $("#subTitle").html("编辑子作业").attr("action", "edit").attr("tid", id);
            $("#" + id).find("input").each(function (index, element) {
                if ($(element).attr("name") == "child.jobName") {
                    $("#jobName1").val(unEscapeHtml($(element).val()));
                }
                if ($(element).attr("name") == "child.agentId") {
                    $("#agentId1").val($(element).val());
                }
                if ($(element).attr("name") == "child.command") {
                    $("#cmd1").val(passBase64($(element).val()));
                }

                if ($(element).attr("name") == "child.successExit") {
                    $("#successExit1").val($(element).val());
                }

                if ($(element).attr("name") == "child.redo") {
                    self.toggle.redo($("#itemRedo").val() || $(element).val());
                }

                if ($(element).attr("name") == "child.runCount") {
                    $("#runCount1").val($(element).val());
                }

                if ($(element).attr("name") == "child.timeout") {
                    $("#timeout1").val($(element).val());
                }

                if ($(element).attr("name") == "child.comment") {
                    $("#comment1").val(unEscapeHtml($(element).val()));
                }
            });
        },

        remove: function (node) {
            $(node).parent().slideUp(300, function () {
                this.remove()
            });
        },

        close: function () {
            $("#subForm")[0].reset();
            $('#jobModal').modal('hide');
        },

        verify: function () {
            self.validata.init();
            self.validata.jobName("1");
            self.validata.command("1");
            self.validata.successExit("1");
            self.validata.timeout("1");
            self.validata.runCount("1");

            var valId = setInterval(function () {
                if (self.validata.jobNameRemote ) {
                    window.clearInterval(valId);
                    if (self.validata.status) {
                        //添加
                        var _jobName = $("#jobName1").val();
                        if ($("#subTitle").attr("action") === "add") {
                            var timestamp = Date.parse(new Date());
                            var addHtml =
                                "<li id='" + timestamp + "'>" +
                                "<span onclick='opencronValidata.subJob.edit(\"" + timestamp + "\")'>" +
                                "   <a data-toggle='modal' href='#jobModal' title='编辑'>" +
                                "       <i class='glyphicon glyphicon-pencil'></i>&nbsp;&nbsp;" +
                                "       <span id='name_" + timestamp + "'>" + escapeHtml(_jobName) + "</span>" +
                                "   </a>" +
                                "</span>" +
                                "<span class='delSubJob' onclick='opencronValidata.subJob.remove(this)'>" +
                                "   <a href='#' title='删除'>" +
                                "       <i class='glyphicon glyphicon-trash'></i>" +
                                "   </a>" +
                                "</span>" +
                                "<input type='hidden' name='child.jobId' value=''>" +
                                "<input type='hidden' name='child.jobName' value='" + escapeHtml(_jobName) + "'>" +
                                "<input type='hidden' name='child.agentId' value='" + $("#agentId1").val() + "'>" +
                                "<input type='hidden' name='child.command' value='" + toBase64($("#cmd1").val()) + "'>" +
                                "<input type='hidden' name='child.redo' value='" + $('#itemRedo').val() + "'>" +
                                "<input type='hidden' name='child.runCount' value='" + $("#runCount1").val() + "'>" +
                                "<input type='hidden' name='child.timeout' value='" + $("#timeout1").val() + "'>" +
                                "<input type='hidden' name='child.successExit' value='" + $("#successExit1").val() + "'>" +
                                "<input type='hidden' name='child.comment' value='" + escapeHtml($("#comment1").val()) + "'>" +
                                "</li>";
                            $("#subJobDiv").append($(addHtml));
                        } else if ($("#subTitle").attr("action") == "edit") {//编辑
                            var id = $("#subTitle").attr("tid");
                            $("#" + id).find("input").each(function (index, element) {

                                if ($(element).attr("name") == "child.jobName") {
                                    $(element).attr("value", _jobName);
                                }

                                if ($(element).attr("name") == "child.redo") {
                                    $(element).attr("value", $('#itemRedo').val());
                                }

                                if ($(element).attr("name") == "child.runCount") {
                                    $(element).attr("value", $("#runCount1").val());
                                }

                                if ($(element).attr("name") == "child.successExit") {
                                    $(element).attr("value", $("#successExit1").val());
                                }

                                if ($(element).attr("name") == "child.agentId") {
                                    $(element).attr("value", $("#agentId1").val());
                                }
                                if ($(element).attr("name") == "child.command") {
                                    $(element).attr("value", toBase64($("#cmd1").val()));
                                }

                                if ($(element).attr("name") == "child.timeout") {
                                    $(element).attr("value", $("#timeout1").val());
                                }

                                if ($(element).attr("name") == "child.comment") {
                                    $(element).attr("value", $("#comment1").val());
                                }
                            });
                            $("#name_" + id).html(escapeHtml(_jobName));
                        }
                        self.subJob.close();
                    }
                }
            },10);
        }
    };

    this.toggle = {
        redo: function (_toggle) {
            $("#itemRedo").val(_toggle);
            if (_toggle == 1) {
                $(".countDiv1").show();
                $("#redo1").prop("checked", true);
                $("#redo1").parent().removeClass("checked").addClass("checked");
                $("#redo1").parent().attr("aria-checked", true);
                $("#redo1").parent().prop("onclick", "showContact()");
                $("#redo0").parent().removeClass("checked");
                $("#redo0").parent().attr("aria-checked", false);
            } else {
                $(".countDiv1").hide();
                $("#redo0").prop("checked", true);
                $("#redo0").parent().removeClass("checked").addClass("checked");
                $("#redo0").parent().attr("aria-checked", true);
                $("#redo1").parent().removeClass("checked");
                $("#redo1").parent().attr("aria-checked", false);
            }
        },
        count: function (_toggle) {
            if (_toggle) {
                $("#redo").val(1);
                $(".countDiv").show()
            } else {
                $("#redo").val(0);
                $(".countDiv").hide();
            }
        },
        contact: function (_toggle) {
            if (_toggle) {
                $(".contact").show()
            } else {
                $(".contact").hide();
            }
        },
        cronTip: function (type) {
            if (type == 0) {
                $("#cronTip").css("visibility","visible").html("crontab: unix/linux的时间格式表达式 ");
            }
            if (type == 1) {
                $("#cronTip").css("visibility","visible").html('quartz: quartz框架的时间格式表达式');
            }
            if ( (arguments[1]||false) && $("#cronExp").val().length > 0) {
                self.validata.cronExp();
            }
        },

        runModel: function (type) {
            if (type == "0") {
                $("#runModelTip").html("串行: 流程任务里的每个任务按照定义的顺序依次执行");
            }
            if (type == "1") {
                $("#runModelTip").html('并行: 流程任务里的所有子任务同时执行');
            }
        },

        subJob: function (_toggle) {
            if (_toggle == "1") {
                $("#jobTypeTip").html("流程作业: 有多个作业组成一个作业组");
                $("#subJob").show();
                $("#runModel").show();
            } else {
                $("#jobTypeTip").html("单一作业: 当前定义作业为要执行的目前作业");
                $("#subJob").hide();
                $("#runModel").hide();
            }
        }
    };

    this.ready();
}

Validata.prototype.ready = function () {

    var _this = this;

    $("#cronType0").next().click(function () {
        _this.validata.cronExp();
    });
    $("#cronType0").parent().parent().click(function () {
        _this.validata.cronExp();
    });
    $("#cronType1").next().click(function () {
        _this.validata.cronExp();
    });
    $("#cronType1").parent().parent().click(function () {
        _this.validata.cronExp();
    });

    $("#runModel0").next().click(function () {
        _this.toggle.runModel(0);
    });
    $("#runModel0").parent().parent().click(function () {
        _this.toggle.runModel(0);
    });

    $("#runModel1").next().click(function () {
        _this.toggle.runModel(1);
    });
    $("#runModel1").parent().parent().click(function () {
        _this.toggle.runModel(1);
    });

    $("#redo01").next().click(function () {
        _this.toggle.count(true)
    });
    $("#redo01").parent().parent().click(function () {
        _this.toggle.count(true)
    });
    $("#redo00").next().click(function () {
        _this.toggle.count(false)
    });
    $("#redo00").parent().parent().click(function () {
        _this.toggle.count(false)
    });

    $("#redo1").next().click(function () {
        _this.toggle.redo(1);
    });
    $("#redo1").parent().parent().click(function () {
        _this.toggle.redo(1);
    })
    $("#redo0").next().click(function () {
        _this.toggle.redo(0);
    });
    $("#redo0").parent().parent().click(function () {
        _this.toggle.redo(0);
    });

    $("#jobType0").next().click(function () {
        _this.toggle.subJob(0);
    });
    $("#jobType0").parent().parent().click(function () {
        _this.toggle.subJob(0);
    });

    $("#jobType1").next().click(function () {
        _this.toggle.subJob(1);
    });
    $("#jobType1").parent().parent().click(function () {
        _this.toggle.subJob(1);
    });

    $("#warning0").next().click(function () {
        _this.toggle.contact(false);
    });
    $("#warning0").parent().parent().click(function () {
        _this.toggle.contact(false);
    });

    $("#warning1").next().click(function () {
        _this.toggle.contact(true);
    });
    $("#warning1").parent().parent().click(function () {
        _this.toggle.contact(true);
    });

    var redo = $('input[type="radio"][name="redo"]:checked').val();
    _this.toggle.count(redo == 1);

    var warning = $('input[type="radio"][name="warning"]:checked').val();
    _this.toggle.contact(warning == 1);

    _this.toggle.subJob($('input[type="radio"][name="jobType"]:checked').val());

    _this.toggle.cronTip($('input[type="radio"][name="cronType"]:checked').val());

    //子作业拖拽
    $("#subJobDiv").sortable({
        delay: 100
    });

    $("#save-btn").click(function () {
        _this.validata.verify();
        if(_this.validata.status){
            var valId = setInterval(function () {
                if (_this.validata.jobNameRemote ) {
                    var cleanFlag = false;
                    if(_this.validata.cronExpRemote){
                        cleanFlag = true;
                    }
                    if(cleanFlag){
                        window.clearInterval(valId);
                        if(_this.validata.status) {
                            var cmd = $("#cmd").val();
                            $("#command").val(toBase64(cmd));
                            $("#jobform").submit();
                        }
                    }
                }
            },10);
        }
    });

    $("#subjob-btn").click(function () {
        _this.subJob.verify();
    });

    $("#jobName").blur(function () {
        _this.validata.jobName();
    }).focus(function () {
        opencron.tipDefault("#jobName");
    });

    $("#cronExp").blur(function () {
        _this.validata.cronExp();
    }).focus(function () {
        var type = $('input[type="radio"][name="cronType"]:checked').val();
        if (type == 0) {
            $("#cronTip").css("visibility","visible").html("crontab: unix/linux的时间格式表达式 ");
            $("#expTip").css("visibility","visible").html('crontab: 请采用unix/linux的时间格式表达式,如 00 01 * * *');
        }
        if (type == 1) {
            $("#cronTip").css("visibility","visible").html('quartz: quartz框架的时间格式表达式');
            $("#expTip").css("visibility","visible").html('quartz: 请采用quartz框架的时间格式表达式,如 0 0 10 L * ?');
        }
    });


    $("#jobName1").blur(function () {
        _this.validata.jobName("1");
    }).focus(function () {
        opencron.tipDefault("#jobName1");
    });

    $("#cmd").blur(function () {
        _this.validata.command();
    }).focus(function () {
        opencron.tipDefault("#cmd");
    });
    $("#cmd1").blur(function () {
        _this.validata.command("1");
    }).focus(function () {
        opencron.tipDefault("#cmd1");
    });

    $("#runCount").blur(function () {
        _this.validata.runCount();
    }).focus(function () {
        opencron.tipDefault("#runCount");
    });
    $("#runCount1").blur(function () {
        _this.validata.runCount("1");
    }).focus(function () {
        opencron.tipDefault("#runCount1");
    });

    $("#successExit").blur(function () {
        _this.validata.successExit();
    }).focus(function () {
        opencron.tipDefault("#successExit");
    });
    $("#successExit1").blur(function () {
        _this.validata.successExit("1");
    }).focus(function () {
        opencron.tipDefault("#successExit1");
    });

    $("#timeout").blur(function () {
        _this.validata.timeout();
    }).focus(function () {
        opencron.tipDefault("#timeout");
    });
    $("#timeout1").blur(function () {
        _this.validata.timeout("1");
    }).focus(function () {
        opencron.tipDefault("#timeout1");
    });

    $("#mobiles").blur(function () {
        _this.validata.mobiles();
    }).focus(function () {
        opencron.tipDefault("#mobiles");
    });

    $("#email").blur(function () {
        _this.validata.email();
    }).focus(function () {
        opencron.tipDefault("#email");
    });
};

