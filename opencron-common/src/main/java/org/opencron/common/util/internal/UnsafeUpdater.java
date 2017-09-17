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

package org.opencron.common.util.internal;


import org.opencron.common.util.ExceptionUtils;


public class UnsafeUpdater {

    /**
     * Creates and returns an updater for objects with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <U> UnsafeIntegerFieldUpdater<U> newIntegerFieldUpdater(Class<? super U> tClass, String fieldName) {
        try {
            return new UnsafeIntegerFieldUpdater<U>(Unsafe.getUnsafe(), tClass, fieldName);
        } catch (Throwable t) {
            ExceptionUtils.throwException(t);
        }
        return null;
    }

    /**
     * Creates and returns an updater for objects with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <U> UnsafeLongFieldUpdater<U> newLongFieldUpdater(Class<? super U> tClass, String fieldName) {
        try {
            return new UnsafeLongFieldUpdater<U>(Unsafe.getUnsafe(), tClass, fieldName);
        } catch (Throwable t) {
            ExceptionUtils.throwException(t);
        }
        return null;
    }

    /**
     * Creates and returns an updater for objects with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <U, W> UnsafeReferenceFieldUpdater<U, W> newReferenceFieldUpdater(Class<? super U> tClass, String fieldName) {
        try {
            return new UnsafeReferenceFieldUpdater<U, W>(Unsafe.getUnsafe(), tClass, fieldName);
        } catch (Throwable t) {
            ExceptionUtils.throwException(t);
        }
        return null;
    }
}
