/*
 * Copyright (c) 2015 The Opencron Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencron.transport.api.channel;


import org.opencron.transport.api.Directory;
import org.opencron.transport.api.UnresolvedAddress;

import java.util.List;


public interface ChannelGroup {

    /**
     * Returns the remote address of this group.
     */
    UnresolvedAddress remoteAddress();

    /**
     * Returns the next {@link Channel} in the group.
     */
    Channel next();

    /**
     * Returns all {@link Channel}s in the group.
     */
    List<? extends Channel> channels();

    /**
     * Returns true if this group contains no {@link Channel}.
     */
    boolean isEmpty();

    /**
     * Adds the specified {@link Channel} to this group.
     */
    boolean add(Channel channel);

    /**
     * Removes the specified {@link Channel} from this group.
     */
    boolean remove(Channel channel);

    /**
     * Returns the number of {@link Channel}s in this group (its cardinality).
     */
    int size();

    /**
     * Sets the capacity of this group.
     */
    void setCapacity(int capacity);

    /**
     * The capacity of this group.
     */
    int getCapacity();

    /**
     * If available return true, otherwise return false.
     */
    boolean isAvailable();

    /**
     * Wait until the {@link Channel}s are available or timeout,
     * if available return true, otherwise return false.
     */
    boolean waitForAvailable(long timeoutMillis);

    /**
     * Weight of service.
     */
    int getWeight(Directory directory);

    /**
     * Sets the weight of service.
     */
    void setWeight(Directory directory, int weight);

    /**
     * Removes the weight of service.
     */
    void removeWeight(Directory directory);

    /**
     * Warm-up time.
     */
    int getWarmUp();

    /**
     * Sets warm-up time.
     */
    void setWarmUp(int warmUp);

    /**
     * Returns {@code true} if warm up to complete.
     */
    boolean isWarmUpComplete();

    /**
     * Time of birth.
     */
    long timestamp();

    /**
     * Deadline millis.
     */
    long deadlineMillis();
}
