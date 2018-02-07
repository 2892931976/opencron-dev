package org.hyperic.sigar.cmd;

import org.opencron.common.Constants;
import org.opencron.common.util.SystemPropertyUtils;

public class TestRunner {

    public static void main(String[] args) throws Exception {
        new CpuInfo().processCommand(args);
    }

}
