/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.jci2.core.compiler;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.jci2.core.utils.ConversionUtils;

/**
 * Creates JavaCompilers
 *
 * TODO use META-INF discovery mechanism
 */
public final class JavaCompilerFactory {

    /**
     * @deprecated will be remove after the next release, please create an instance yourself
     */
    @Deprecated
    private static final JavaCompilerFactory INSTANCE = new JavaCompilerFactory();

    private final Map<String, Class<?>> classCache = new HashMap<>();

    /**
     * @deprecated will be remove after the next release, please create an instance yourself
     */
    @Deprecated
    public static JavaCompilerFactory getInstance() {
        return JavaCompilerFactory.INSTANCE;
    }

    /**
     * Tries to guess the class name by convention. So for compilers
     * following the naming convention
     *
     *   org.apache.commons.jci2.core.core.compilers.SomeJavaCompiler
     *
     * you can use the short-hands "some"/"Some"/"SOME". Otherwise
     * you have to provide the full class name. The compiler is
     * getting instanciated via (cached) reflection.
     *
     * @param pHint
     * @return JavaCompiler or null
     */
    public JavaCompiler createCompiler(final String pHint) {

        final String className;
        if (pHint.indexOf('.') < 0) {
            final String javaCasing = ConversionUtils.toJavaCasing(pHint);
            className = "org.apache.commons.jci2.compiler." + javaCasing.toLowerCase(Locale.ROOT) + "." + javaCasing
                    + "JavaCompiler";
        } else {
            className = pHint;
        }

        Class<?> clazz = classCache.get(className);

        if (clazz == null) {
            try {
                clazz = Class.forName(className);
                classCache.put(className, clazz);
            } catch (final ClassNotFoundException e) {
                clazz = null;
            }
        }

        if (clazz == null) {
            return null;
        }

        try {
            return (JavaCompiler) clazz.getConstructor().newInstance();
        } catch (final Throwable t) {
            return null;
        }
    }

}
