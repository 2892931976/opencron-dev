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
package org.opencron.server.domain;

import com.alibaba.fastjson.annotation.JSONField;
import org.opencron.common.util.CommonUtils;
import org.opencron.common.util.DigestUtils;
import org.opencron.common.util.RSAUtils;
import org.opencron.server.job.OpencronTools;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by benjobs on 16/5/21.
 */

@Entity
@Table(name = "T_TERMINAL")
public class Terminal implements Serializable {

    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private Long userId;
    private String host;
    private int port;
    private String userName;
    private String theme;
    private Integer sshType;//0账户登录,1:sshKey登录

    @Lob
    @Column(columnDefinition = "BLOB")
    @JSONField(serialize = false)
    private byte[] privateKey;

    @Lob
    @Column(columnDefinition = "BLOB")
    @JSONField(serialize = false)
    private byte[] passphrase;

    @Lob
    @Column(columnDefinition = "BLOB")
    @JSONField(serialize = false)
    private byte[] authorization;

    private String status = AuthStatus.SUCCESS.status;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date logintime;

    @Transient
    @JSONField(serialize = false)
    private User user;

    @Transient
    @JSONField(serialize = false)
    private String password;

    @Transient
    @JSONField(serialize = false)
    private String phrase;

    @Transient
    private String clientId;

    @Transient
    private MultipartFile sshKeyFile;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        try {
            byte[] decodedData = RSAUtils.decryptByPrivateKey(this.authorization, OpencronTools.Auth.getPrivateKey());
            return new String(decodedData);
        } catch (Exception e) {
        }
        return null;
    }

    public void setPassword(String password) throws Exception {
        if (password != null) {
            password = DigestUtils.passBase64(password);
        }
        this.authorization = RSAUtils.encryptByPublicKey(password.getBytes(), OpencronTools.Auth.getPublicKey());
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getLogintime() {
        return logintime;
    }

    public void setLogintime(Date logintime) {
        this.logintime = logintime;
    }

    public byte[] getAuthorization() {
        return authorization;
    }

    public void setAuthorization(byte[] authorization) {
        this.authorization = authorization;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public Integer getSshType() {
        return sshType;
    }

    public void setSshType(Integer sshType) {
        this.sshType = sshType;
    }

    public byte[] getPrivateKey() {
        //拿本地的公钥解密ssh的私钥
        try {
            return RSAUtils.decryptByPrivateKey(this.privateKey, OpencronTools.Auth.getPrivateKey());
        } catch (Exception e) {
            throw new RuntimeException("[opencron] getPrivateKey error!");
        }
    }

    public void setPrivateKey(byte[] privateKey) throws Exception {
        //拿本地的公钥加密ssh的私钥
        this.privateKey = RSAUtils.encryptByPublicKey(privateKey, OpencronTools.Auth.getPublicKey());
    }

    public byte[] getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(byte[] passphrase) {
        this.passphrase = passphrase;
    }

    public String getPhrase() {
        try {
            byte[] decodedData = RSAUtils.decryptByPrivateKey(this.passphrase, OpencronTools.Auth.getPrivateKey());
            return new String(decodedData);
        } catch (Exception e) {
        }
        return null;
    }

    public void setPhrase(String phrase) throws Exception {
        if (phrase != null) {
            this.passphrase = RSAUtils.encryptByPublicKey(phrase.getBytes(), OpencronTools.Auth.getPublicKey());
        }
    }

    public String getPrivateKeyPath(){
        return OpencronTools.Auth.getKeyPath().concat("/").concat(this.getHost()).concat("@").concat(this.getUserName()).concat("_id_rsa");
    }

    public MultipartFile getSshKeyFile() {
        return sshKeyFile;
    }

    public void setSshKeyFile(MultipartFile sshKeyFile) {
        this.sshKeyFile = sshKeyFile;
    }

    @Override
    public String toString() {
        return "Terminal{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", userId=" + userId +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", userName='" + userName + '\'' +
                ", theme='" + theme + '\'' +
                ", sshType='" + sshType + '\'' +
                ", privateKey='" + privateKey + '\'' +
                ", passphrase='" + passphrase + '\'' +
                ", authorization=" + Arrays.toString(authorization) +
                ", status='" + status + '\'' +
                ", logintime=" + logintime +
                ", user=" + user +
                ", password='" + password + '\'' +
                ", clientId='" + clientId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Terminal terminal = (Terminal) o;

        if (userName != null ? !userName.equals(terminal.userName) : terminal.userName != null) return false;
        return user != null ? user.equals(terminal.user) : terminal.user == null;
    }

    @Override
    public int hashCode() {
        int result = userName != null ? userName.hashCode() : 0;
        result = 31 * result + (user != null ? user.hashCode() : 0);
        return result;
    }

    public enum AuthStatus {
        INITIAL("initial"),
        AUTH_FAIL("authfail"),
        PUBLIC_KEY_FAIL("keyauthfail"),
        GENERIC_FAIL("genericfail"),
        HOST_FAIL("hostfail"),
        SUCCESS("success");
        public String status;

        AuthStatus(String status) {
            this.status = status;
        }

    }


}
