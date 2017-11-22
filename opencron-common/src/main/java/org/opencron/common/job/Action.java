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

public enum Action  {
  PING(0),
  PATH(1),
  MONITOR(2),
  EXECUTE(3),
  PASSWORD(4),
  KILL(5),
  PROXY(6),
  GUID(7),
  CRONTAB(8),
  RESTART(9);

  private final int value;

  Action(int value) {
    this.value = value;
  }

  /**
   * Get the integer value of this enum value, as defined in the Thrift IDL.
   */
  public int getValue() {
    return value;
  }


  public static Action findByName(String name) {
    for (Action action: Action.values()) {
      if (action.name().equalsIgnoreCase(name)) {
        return action;
      }
    }
    return null;
  }

}