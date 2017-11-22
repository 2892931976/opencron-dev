package org.opencron.server.vo;


import java.io.Serializable;

public class CrontabVo implements Serializable {

    private String exp;
    private String cmd;

    public CrontabVo(String exp, String cmd) {
        this.exp = exp == null?null:exp.trim();
        this.cmd = cmd == null?null:cmd.trim();
    }

    public String getExp() {
        return exp;
    }

    public void setExp(String exp) {
        this.exp = exp;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }
}
