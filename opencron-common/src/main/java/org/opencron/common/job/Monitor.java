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

package org.opencron.common.job;

import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;

import java.io.Serializable;
import java.util.List;

/**
 * Created by benjobs on 16/4/7.
 */
public class Monitor implements Serializable {

    private List<CPU> cpu;

    private Mem mem;

    public List<CPU> getCpu() {
        return cpu;
    }

    public void setCpu(List<CPU> cpu) {
        this.cpu = cpu;
    }

    public Mem getMem() {
        return mem;
    }

    public void setMem(Mem mem) {
        this.mem = mem;
    }

    public static class CPU implements Serializable {

        private int index;
        private String vendor = null;
        private String model = null;
        private int mhz = 0;
        private long cacheSize = 0L;
        private int totalCores = 0;
        private int totalSockets = 0;
        private int coresPerSocket = 0;
        private double user;
        private double sys;
        private double nice;
        private double idle;
        private double wait;
        private double irq;
        private double softIrq;
        private double stolen;
        private double combined;

        public CPU(int index, CpuInfo info, CpuPerc perc) {
            this.index = index;
            this.cacheSize = info.getCacheSize();
            this.coresPerSocket = info.getCoresPerSocket();
            this.totalCores = info.getTotalCores();
            this.totalSockets = info.getTotalSockets();
            this.mhz = info.getMhz();
            this.model = info.getModel();
            this.vendor = info.getVendor();

            this.user = perc.getUser();
            this.sys = perc.getSys();
            this.nice = perc.getNice();
            this.idle = perc.getIdle();
            this.wait = perc.getWait();
            this.irq = perc.getIrq();
            this.softIrq = perc.getSoftIrq();
            this.stolen = perc.getStolen();
            this.combined = perc.getCombined();
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getVendor() {
            return vendor;
        }

        public void setVendor(String vendor) {
            this.vendor = vendor;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getMhz() {
            return mhz;
        }

        public void setMhz(int mhz) {
            this.mhz = mhz;
        }

        public long getCacheSize() {
            return cacheSize;
        }

        public void setCacheSize(long cacheSize) {
            this.cacheSize = cacheSize;
        }

        public int getTotalCores() {
            return totalCores;
        }

        public void setTotalCores(int totalCores) {
            this.totalCores = totalCores;
        }

        public int getTotalSockets() {
            return totalSockets;
        }

        public void setTotalSockets(int totalSockets) {
            this.totalSockets = totalSockets;
        }

        public int getCoresPerSocket() {
            return coresPerSocket;
        }

        public void setCoresPerSocket(int coresPerSocket) {
            this.coresPerSocket = coresPerSocket;
        }

        public double getUser() {
            return user;
        }

        public void setUser(double user) {
            this.user = user;
        }

        public double getSys() {
            return sys;
        }

        public void setSys(double sys) {
            this.sys = sys;
        }

        public double getNice() {
            return nice;
        }

        public void setNice(double nice) {
            this.nice = nice;
        }

        public double getIdle() {
            return idle;
        }

        public void setIdle(double idle) {
            this.idle = idle;
        }

        public double getWait() {
            return wait;
        }

        public void setWait(double wait) {
            this.wait = wait;
        }

        public double getIrq() {
            return irq;
        }

        public void setIrq(double irq) {
            this.irq = irq;
        }

        public double getSoftIrq() {
            return softIrq;
        }

        public void setSoftIrq(double softIrq) {
            this.softIrq = softIrq;
        }

        public double getStolen() {
            return stolen;
        }

        public void setStolen(double stolen) {
            this.stolen = stolen;
        }

        public double getCombined() {
            return combined;
        }

        public void setCombined(double combined) {
            this.combined = combined;
        }

        @Override
        public String toString() {
            return "CPU{" +
                    "index=" + index +
                    ", vendor='" + vendor + '\'' +
                    ", model='" + model + '\'' +
                    ", mhz=" + mhz +
                    ", cacheSize=" + cacheSize +
                    ", totalCores=" + totalCores +
                    ", totalSockets=" + totalSockets +
                    ", coresPerSocket=" + coresPerSocket +
                    ", user=" + user +
                    ", sys=" + sys +
                    ", nice=" + nice +
                    ", idle=" + idle +
                    ", wait=" + wait +
                    ", irq=" + irq +
                    ", softIrq=" + softIrq +
                    ", stolen=" + stolen +
                    ", combined=" + combined +
                    '}';
        }
    }

    public static class Mem implements Serializable {
        private long total = 0L;
        private  long ram = 0L;
        private long used = 0L;
        private long free = 0L;
        private long actualUsed = 0L;
        private long actualFree = 0L;
        private double usedPercent = 0.0D;
        private double freePercent = 0.0D;
        private String unitName = "K";
        public Mem(org.hyperic.sigar.Mem mem){
            this.total = mem.getTotal()/1024;
            this.ram = mem.getRam()/1024;
            this.used = mem.getUsed()/1024;
            this.free = mem.getFree()/1024;
            this.actualUsed = mem.getActualUsed()/1024;
            this.actualFree = mem.getActualFree()/1024;
            this.usedPercent = mem.getUsedPercent()/1024;
            this.freePercent = mem.getFreePercent()/1024;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getRam() {
            return ram;
        }

        public void setRam(long ram) {
            this.ram = ram;
        }

        public long getUsed() {
            return used;
        }

        public void setUsed(long used) {
            this.used = used;
        }

        public long getFree() {
            return free;
        }

        public void setFree(long free) {
            this.free = free;
        }

        public long getActualUsed() {
            return actualUsed;
        }

        public void setActualUsed(long actualUsed) {
            this.actualUsed = actualUsed;
        }

        public long getActualFree() {
            return actualFree;
        }

        public void setActualFree(long actualFree) {
            this.actualFree = actualFree;
        }

        public double getUsedPercent() {
            return usedPercent;
        }

        public void setUsedPercent(double usedPercent) {
            this.usedPercent = usedPercent;
        }

        public double getFreePercent() {
            return freePercent;
        }

        public void setFreePercent(double freePercent) {
            this.freePercent = freePercent;
        }

        public String getUnitName() {
            return unitName;
        }

        public void setUnitName(String unitName) {
            this.unitName = unitName;
        }
    }


}
