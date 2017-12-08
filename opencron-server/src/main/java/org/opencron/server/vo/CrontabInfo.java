package org.opencron.server.vo;


import java.io.Serializable;

public class CrontabInfo implements Serializable {

    private Integer id;
    private String exp;
    private String cmd;

    public CrontabInfo(Integer id, String exp, String cmd) {
        this.id = id;
        this.exp = exp;
        this.cmd = cmd;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
