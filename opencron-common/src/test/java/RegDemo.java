/*
 * Copyright (c) 2015 The Opencron Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import org.opencron.common.util.CommonUtils;

import java.util.Scanner;

/**
 * Created by benjobs on 15/12/5.
 */
public class RegDemo {

    public static void main(String[] args) {
        String str = " */5 * * * * /usr/sbin/ntpdate stdtime.gov.hk > /dev/null 2>&1\n" +
                "  * * * * */1 /usr/sbin/hwclock -w > /dev/null 2>&1\n" +
                "\n" +
                "  40 00 * * *   /home/wanghj/workspace/bigdata-spider/bin/doeach.sh\n" +
                "\n" +
                "  #00 12 * * *   /home/wanghj/workspace/bigdata-spider/bin/startup.sh -ss1\n" +
                "\n" +
                "  00 08 * * *  /home/wanghj/workspace/bigdata-spider/bin/startup.sh -ss1 -pday\n" +
                "\n" +
                "  00 10 * * *  /home/wanghj/workspace/bigdata-spider/bin/startup.sh -ss1 -pday\n" +
                "\n" +
                "  30 11 * * *  /home/wanghj/workspace/bigdata-spider/bin/startup.sh -ss1 -pday\n" +
                "\n" +
                "  00 01 * * *  /home/wanghj/workspace/bigdata-spider/bin/startup.sh -ss2\n" +
                "  00 04 * * *  /home/wanghj/workspace/bigdata-spider/bin/startup.sh -ss6\n" +
                "  #00 05 * * *  /home/wanghj/workspace/bigdata-spider/bin/startup.sh -ss7";

        Scanner scanner = new Scanner(str);
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            if (CommonUtils.notEmpty(line)) {
                line = line.trim();
                //已注释打头...
                if (line.startsWith("#")) {
                    continue;
                }

                String cron[] = line.split("\\s+");
                StringBuilder cronbuilder = new StringBuilder();
                StringBuilder cmdBuilder = new StringBuilder();

                for(int i=0;i<cron.length;i++){
                    if (i<=4) {
                        cronbuilder.append(cron[i]).append(" ");
                    }else {
                        cmdBuilder.append(cron[i]).append(" ");
                    }
                }

                System.out.println(cronbuilder.toString().trim()+"====>"+cmdBuilder.toString().trim());
            }
        }

    }
}
