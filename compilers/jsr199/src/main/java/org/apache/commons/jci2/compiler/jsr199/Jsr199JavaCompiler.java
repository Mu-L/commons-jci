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
package org.apache.commons.jci2.compiler.jsr199;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import org.apache.commons.jci2.core.compiler.AbstractJavaCompiler;
import org.apache.commons.jci2.core.compiler.CompilationResult;
import org.apache.commons.jci2.core.compiler.JavaCompilerSettings;
import org.apache.commons.jci2.core.problems.CompilationProblem;
import org.apache.commons.jci2.core.readers.ResourceReader;
import org.apache.commons.jci2.core.stores.ResourceStore;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class Jsr199JavaCompiler extends AbstractJavaCompiler {

    private final Log log = LogFactory.getLog(Jsr199JavaCompiler.class);

    private final class CompilationUnit extends SimpleJavaFileObject {
        final private ResourceReader reader;
        final private String name;

        public CompilationUnit( final String pName, final ResourceReader pReader ) {
            super(URI.create("reader:///" + pName), Kind.SOURCE);
            reader = pReader;
            name = pName;
        }

        @Override
        public boolean delete() {
            log.debug("delete");
            return super.delete();
        }

        @Override
        public CharSequence getCharContent(final boolean encodingErrors) throws IOException {
            log.debug("getCharContent of " + name);
            final byte[] content = reader.getBytes(name);
            return new String(content);
        }

        @Override
        public long getLastModified() {
            log.debug("getLastModified");
            return super.getLastModified();
        }

        @Override
        public String getName() {
            log.debug("getName " + super.getName());
            return super.getName();
        }

        @Override
        public boolean isNameCompatible(final String simpleName, final Kind kind) {
            log.debug("isNameCompatible " + simpleName + " " + kind);
            // return super.isNameCompatible(simpleName, kind);
            return true;
        }

        @Override
        public InputStream openInputStream() throws IOException {
            log.debug("openInputStream");
            return super.openInputStream();
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            log.debug("openOutputStream");
            return super.openOutputStream();
        }

        @Override
        public Reader openReader(final boolean arg0) throws IOException {
            log.debug("openReader");
            return super.openReader(arg0);
        }

        @Override
        public Writer openWriter() throws IOException {
            log.debug("openWriter");
            return super.openWriter();
        }

        @Override
        public URI toUri() {
            // log.debug("toUri " + super.toUri());
            return super.toUri();
        }
    }

    private final class JciJavaFileManager implements JavaFileManager  {
        // private final ResourceStore store;
        final Collection<JavaFileObject> units;

        public JciJavaFileManager( final Collection<JavaFileObject> pUnits, final ResourceStore pStore ) {
          // store = pStore;
          units = pUnits;
        }

        @Override
        public void close() {
            log.debug("close");
        }
        @Override
        public void flush() {
            log.debug("flush");
        }
        @Override
        public ClassLoader getClassLoader(final JavaFileManager.Location location) {
            log.debug("getClassLoader");
            return null;
        }
        @Override
        public FileObject getFileForInput(final JavaFileManager.Location location, final String packageName, final String relativeName) {
            log.debug("getFileForInput");
            return null;
        }
        @Override
        public FileObject getFileForOutput(final JavaFileManager.Location location, final String packageName, final String relativeName, final FileObject sibling) {
            log.debug("getFileForOutput");
            return null;
        }
        @Override
        public JavaFileObject getJavaFileForInput(final JavaFileManager.Location location, final String className, final JavaFileObject.Kind kind) {
            log.debug("getJavaFileForInput");
            return null;
        }
        @Override
        public JavaFileObject getJavaFileForOutput(final JavaFileManager.Location location, final String className, final JavaFileObject.Kind kind, final FileObject sibling) {
            log.debug("getJavaFileForOutput");
            return null;
        }
        @Override
        public int isSupportedOption(final String option) {
            log.debug("isSupportedOption " + option);
            return 0;
        }
        @Override
        public boolean handleOption(final String current, final Iterator<String> remaining) {
            log.debug("handleOption " + current);
            return false;
        }
        @Override
        public boolean hasLocation(final JavaFileManager.Location location) {
            log.debug("hasLocation " + location);
            return false;
        }
        @Override
        public String inferBinaryName(final JavaFileManager.Location location, final JavaFileObject file) {
            final String s = file.getName().replaceFirst(".java", ".class");
            log.debug("inferBinaryName " + file.getName() + " -> " + s);
            return s;
        }
        @Override
        public Iterable<JavaFileObject> list(final JavaFileManager.Location location, final String packageName, final Set<JavaFileObject.Kind> kinds, final boolean recurse) {
            if (packageName.startsWith("java.")) {
                return new ArrayList<>();
            }
            log.debug("list " + location + packageName + kinds + recurse);
            return units;
        }
        @Override
        public boolean isSameFile(final FileObject fileobject, final FileObject fileobject1) {
          return false;
        }
    }

    private final Jsr199JavaCompilerSettings settings;

    public Jsr199JavaCompiler() {
        settings = new Jsr199JavaCompilerSettings();
    }

    public Jsr199JavaCompiler( final Jsr199JavaCompilerSettings pSettings ) {
        settings = pSettings;
    }

    @Override
    public CompilationResult compile( final String[] pResourcePaths, final ResourceReader pReader, final ResourceStore pStore, final ClassLoader classLoader, final JavaCompilerSettings settings) {

        final Collection<JavaFileObject> units = new ArrayList<>();
        for (final String sourcePath : pResourcePaths) {
            log.debug("compiling " + sourcePath);
            units.add(new CompilationUnit(sourcePath, pReader));
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        if (compiler == null) {
            final ServiceLoader<javax.tools.JavaCompiler> loader = ServiceLoader.load(javax.tools.JavaCompiler.class);
            compiler = loader.iterator().next();
        }

        if (compiler == null) {
            throw new IllegalStateException("No java compiler in class path");
        }

        final JavaFileManager fileManager = new JciJavaFileManager(units, pStore);
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        final CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, units);

        if (task.call().booleanValue()) {
            log.debug("compiled");
        }

        final List<Diagnostic<? extends JavaFileObject>> jsrProblems = diagnostics.getDiagnostics();
        final CompilationProblem[] problems = new CompilationProblem[jsrProblems.size()];
        int i = 0;
        for (final Diagnostic<? extends JavaFileObject> jsrProblem : jsrProblems) {
            problems[i++] = new Jsr199CompilationProblem(jsrProblem);
        }

        return new CompilationResult(problems);
    }

    @Override
    public JavaCompilerSettings createDefaultSettings() {
        return this.settings;
    }

}
