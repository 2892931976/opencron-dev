package org.opencron.server.vo;

public class Status {

    private boolean status;

    public Status(boolean status){
        this.status = status;
    }

    public static Status FALSE = new Status(false);

    public static Status TRUE = new Status(true);

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public static Status getFALSE() {
        return FALSE;
    }

    public static void setFALSE(Status FALSE) {
        Status.FALSE = FALSE;
    }

    public static Status getTRUE() {
        return TRUE;
    }

    public static void setTRUE(Status TRUE) {
        Status.TRUE = TRUE;
    }
}
