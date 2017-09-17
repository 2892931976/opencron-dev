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

package org.opencron.transport.api;

import java.util.List;

public interface Config {

    /**
     * Return all set {@link Option}'s.
     */
    List<Option<?>> getOptions();

    /**
     * Return the value of the given {@link Option}.
     */
    <T> T getOption(Option<T> option);

    /**
     * Sets a configuration property with the specified name and value.
     *
     * @return {@code true} if and only if the property has been set
     */
    <T> boolean setOption(Option<T> option, T value);
}
