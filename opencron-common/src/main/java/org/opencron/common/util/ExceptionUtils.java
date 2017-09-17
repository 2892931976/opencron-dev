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

package org.opencron.common.util;

import org.opencron.common.util.internal.Unsafe;
import org.opencron.common.util.internal.UnsafeReferenceFieldUpdater;
import org.opencron.common.util.internal.UnsafeUpdater;

import java.io.*;

public class ExceptionUtils {

    private static final UnsafeReferenceFieldUpdater<Throwable, Throwable> cause_updater =
            UnsafeUpdater.newReferenceFieldUpdater(Throwable.class, "cause");

    /**
     * Raises an exception bypassing compiler checks for checked exceptions.
     */
    public static void throwException(Throwable t) {
        sun.misc.Unsafe unsafe = Unsafe.getUnsafe();
        if (unsafe != null) {
            unsafe.throwException(t);
        } else {
            ExceptionUtils.<RuntimeException>throwException0(t);
        }
    }

    /**
     * 类型转换只是骗过前端javac编译器, 泛型只是个语法糖, 在javac编译后会解除语法糖将类型擦除,
     * 也就是说并不会生成checkcast指令, 所以在运行期不会抛出ClassCastException异常
     *
     * private static <E extends java/lang/Throwable> void throwException0(java.lang.Throwable) throws E;
     *      flags: ACC_PRIVATE, ACC_STATIC
     *      Code:
     *      stack=1, locals=1, args_size=1
     *          0: aload_0
     *          1: athrow // 注意在athrow之前并没有checkcast指令
     *      ...
     *  Exceptions:
     *      throws java.lang.Throwable
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwException0(Throwable t) throws E {
        throw (E) t;
    }

    public static <T extends Throwable> T cutCause(T cause) {
        Throwable rootCause = cause;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        if (rootCause != cause) {
            cause.setStackTrace(rootCause.getStackTrace());
            cause_updater.set(cause, cause);
        }
        return cause;
    }

    public static String stackTrace(Throwable t) {
        if (t == null) {
            return "null";
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out);
        t.printStackTrace(ps);
        ps.flush();
        try {
            return new String(out.toByteArray());
        } finally {
            try {
                out.close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * 将CheckedException转换为UncheckedException.
     */
    public static RuntimeException unchecked(Throwable ex) {
        if (ex instanceof RuntimeException) {
            return (RuntimeException) ex;
        } else {
            return new RuntimeException(ex);
        }
    }


    /**
     * 获取组合本异常信息与底层异常信息的异常描述, 适用于本异常为统一包装异常类，底层异常才是根本原因的情况。
     */
    public static String getErrorMessageWithNestedException(Throwable ex) {
        Throwable nestedException = ex.getCause();
        return new StringBuilder().append(ex.getMessage()).append(" nested exception is ")
                .append(nestedException.getClass().getName()).append(":").append(nestedException.getMessage())
                .toString();
    }

    /**
     * 获取异常的Root Cause.
     */
    public static Throwable getRootCause(Throwable ex) {
        Throwable cause;
        while ((cause = ex.getCause()) != null) {
            ex = cause;
        }
        return ex;
    }

    /**
     * 判断异常是否由某些底层的异常引起.
     */
    public static boolean isCausedBy(Exception ex, Class<? extends Exception>... causeExceptionClasses) {
        Throwable cause = ex;
        while (cause != null) {
            for (Class<? extends Exception> causeClass : causeExceptionClasses) {
                if (causeClass.isInstance(cause)) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

}
