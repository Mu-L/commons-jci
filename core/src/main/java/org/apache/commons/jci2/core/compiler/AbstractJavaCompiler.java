/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.jci2.core.compiler;

import org.apache.commons.jci2.core.problems.CompilationProblemHandler;
import org.apache.commons.jci2.core.readers.ResourceReader;
import org.apache.commons.jci2.core.stores.ResourceStore;

/**
 * Base class for compiler implementations. Provides just a few
 * convenience methods.
 */
public abstract class AbstractJavaCompiler implements JavaCompiler {

    protected CompilationProblemHandler problemHandler;

    @Override
    public void setCompilationProblemHandler( final CompilationProblemHandler pHandler ) {
        problemHandler = pHandler;
    }

    @Override
    public CompilationResult compile( final String[] pClazzNames, final ResourceReader pReader, final ResourceStore pStore ) {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        if (classLoader == null) {
            classLoader = this.getClass().getClassLoader();
        }

        return compile(pClazzNames, pReader, pStore, classLoader, createDefaultSettings());
    }

    @Override
    public CompilationResult compile( final String[] pClazzNames, final ResourceReader pReader, final ResourceStore pStore, final ClassLoader pClassLoader ) {
        return compile(pClazzNames, pReader, pStore, pClassLoader, createDefaultSettings());
    }

}
