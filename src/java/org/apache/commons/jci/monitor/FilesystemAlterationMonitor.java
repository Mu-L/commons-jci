/*
 * Copyright 1999-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jci.monitor;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author tcurdt
 */
public final class FilesystemAlterationMonitor implements Runnable {

    private final static Log log = LogFactory.getLog(FilesystemAlterationMonitor.class);

    public class Entry {

        private final File root;
        private final File file;
        private long lastModified;
        private Set paths = new HashSet();
        private Set childs = new HashSet();
        private final boolean isDirectory;


        public Entry(final File pRoot, final File pFile) {
            root = pRoot;
            file = pFile;
            lastModified = -1;
            isDirectory = file.isDirectory();
        }


        public boolean hasChanged() {
            return file.lastModified() != lastModified;
        }


        public boolean isDelected() {
            return !file.exists();
        }


        public boolean isDirectory() {
            return isDirectory;
        }


        public Entry[] getChilds() {
            final Entry[] r = new Entry[childs.size()];
            childs.toArray(r);
            return r;
        }


        private FileFilter getFileFilter() {
            return new FileFilter() {

                public boolean accept( final File pathname ) {
                    final String p = pathname.getAbsolutePath();
                    return !paths.contains(p);
                }
            };
        }


        public Entry[] getNonChilds() {
            final File[] newFiles = file.listFiles(getFileFilter());
            final Entry[] r = new Entry[newFiles.length];
            for (int i = 0; i < newFiles.length; i++) {
                r[i] = new Entry(root, newFiles[i]);
            }
            return r;
        }


        public void add( final Entry entry ) {
            childs.add(entry);
            paths.add(entry.toString());
            onCreate(root, entry);
        }


        private void deleteChilds() {
            final Entry[] childs = this.getChilds();
            for (int i = 0; i < childs.length; i++) {
                final Entry child = childs[i];
                delete(child);
            }
        }


        public void delete( final Entry entry ) {
            childs.remove(entry);
            paths.remove(entry.toString());
            entry.deleteChilds();
            onDelete(root, entry);
        }


        public File getFile() {
            return file;
        }


        public File getRoot() {
            return root;
        }


        public void markNotChanged() {
            lastModified = file.lastModified();
        }


        public String toString() {
            return file.getAbsolutePath();
        }
    }

    private Map listeners = new MultiHashMap();
    private Map directories = new MultiHashMap();
    private Map entries = new HashMap();

    private final Object mutex = new Object();
    
    private long delay = 3000;
    private boolean running = true;

    public FilesystemAlterationMonitor() {
    }


    public void setInterval( final long pDelay ) {
        delay = pDelay;
    }


    public void addListener( final FilesystemAlterationListener listener, final File directory ) {
        synchronized (mutex) {
            // listerner -> dir1, dir2, dir3

            final MultiHashMap newListeners = new MultiHashMap(listeners);
            newListeners.put(listener, directory);
            listeners = newListeners;
            
            // directory -> listener1, listener2, listener3
            final MultiHashMap newDirectories = new MultiHashMap(directories);
            newDirectories.put(directory, listener);
            directories = newDirectories;
        }
    }


    public void removeListener( final FilesystemAlterationListener listener ) {
        synchronized (mutex) {
            // listerner -> dir1, dir2, dir3
            final MultiHashMap newListeners = new MultiHashMap(listeners);
            Collection d = (Collection) newListeners.remove(listener);
            listeners = newListeners;

            if (d != null) {
                // directory -> listener1, listener2, listener3
                final MultiHashMap newDirectories = new MultiHashMap(directories);
                for (Iterator it = d.iterator(); it.hasNext();) {
                    newDirectories.remove(it.next());
                    entries.remove(d);
                }
                directories = newDirectories;
            }            
        }
    }

    private void onStart( final File root ) {
        log.debug("* start checking " + root);

        Map directories;
        synchronized(mutex) {
            directories = this.directories;
        }
        final Collection l = (Collection) directories.get(root);
        if (l != null) {
            for (Iterator it = l.iterator(); it.hasNext();) {
                final FilesystemAlterationListener listener = (FilesystemAlterationListener) it.next();
                listener.onStart();
            }            
        }
    }


    private void onStop( final File root ) {
        log.debug("* stop checking " + root);
                
        Map directories;
        synchronized(mutex) {
            directories = this.directories;
        }
        final Collection l = (Collection) directories.get(root);
        if (l != null) {
            for (Iterator it = l.iterator(); it.hasNext();) {
                final FilesystemAlterationListener listener = (FilesystemAlterationListener) it.next();
                listener.onStop();
            }            
        }
    }


    private void onCreate( final File root, final Entry entry ) {
        
        log.debug("* created " + ((entry.isDirectory())?"dir ":"file ") + entry);
        
        Map directories;
        synchronized(mutex) {
            directories = this.directories;
        }

        final Collection l = (Collection) directories.get(root);
        if (l != null) {
            if (entry.isDirectory()) {
                for (Iterator it = l.iterator(); it.hasNext();) {
                    final FilesystemAlterationListener listener = (FilesystemAlterationListener) it.next();
                    listener.onCreateDirectory(entry.getFile());
                }
            } else {
                for (Iterator it = l.iterator(); it.hasNext();) {
                    final FilesystemAlterationListener listener = (FilesystemAlterationListener) it.next();
                    listener.onCreateFile(entry.getFile());
                }
            }
        }

        entry.markNotChanged();
    }


    private void onChange( final File root, final Entry entry ) {
        
        log.debug("* changed " + ((entry.isDirectory())?"dir ":"file ") + entry);
        
        Map directories;
        synchronized(mutex) {
            directories = this.directories;
        }

        final Collection l = (Collection) directories.get(root);
        if (l != null) {
            if (entry.isDirectory()) {
                for (Iterator it = l.iterator(); it.hasNext();) {
                    final FilesystemAlterationListener listener = (FilesystemAlterationListener) it.next();
                    listener.onChangeDirectory(entry.getFile());
                }
            } else {
                for (Iterator it = l.iterator(); it.hasNext();) {
                    final FilesystemAlterationListener listener = (FilesystemAlterationListener) it.next();
                    listener.onChangeFile(entry.getFile());
                }
            }
        }

        entry.markNotChanged();
    }


    private void onDelete( final File root, final Entry entry ) {
        
        log.debug("* deleted " + ((entry.isDirectory())?"dir ":"file ") + entry);
        
        Map directories;
        synchronized(mutex) {
            directories = this.directories;
        }

        final Collection l = (Collection) directories.get(root);
        if (l != null) {
            if (entry.isDirectory()) {
                for (Iterator it = l.iterator(); it.hasNext();) {
                    final FilesystemAlterationListener listener = (FilesystemAlterationListener) it.next();
                    listener.onDeleteDirectory(entry.getFile());
                }
            } else {
                for (Iterator it = l.iterator(); it.hasNext();) {
                    final FilesystemAlterationListener listener = (FilesystemAlterationListener) it.next();
                    listener.onDeleteFile(entry.getFile());
                }
            }
        }

        entry.markNotChanged();
    }


    private void check( final File root, final Entry entry, final boolean create ) {
        if (entry.isDirectory()) {
            final Entry[] currentChilds = entry.getChilds();
            if (entry.hasChanged() || create) {
                if (!create) {
                    onChange(root, entry);
                    for (int i = 0; i < currentChilds.length; i++) {
                        final Entry child = currentChilds[i];
                        if (child.isDelected()) {
                            entry.delete(child);
                            currentChilds[i] = null;
                        }
                    }
                }
                final Entry[] newChilds = entry.getNonChilds();
                for (int i = 0; i < newChilds.length; i++) {
                    final Entry child = newChilds[i];
                    entry.add(child);
                }
                if (!create) {
                    for (int i = 0; i < currentChilds.length; i++) {
                        final Entry child = currentChilds[i];
                        if (child != null) {
                            check(root, child, false);
                        }
                    }
                }
                for (int i = 0; i < newChilds.length; i++) {
                    final Entry child = newChilds[i];
                    check(root, child, true);
                }
            } else {
                for (int i = 0; i < currentChilds.length; i++) {
                    final Entry child = currentChilds[i];
                    check(root, child, false);
                }
            }
        } else {
            if (entry.isDelected()) {
                onDelete(root, entry);
            } else if (entry.hasChanged()) {
                onChange(root, entry);
            }
        }
    }


    public void run() {
        
        while (running) {
            Map directories;
            
            synchronized (mutex) {
                directories = this.directories;
            }

            for (Iterator it = directories.keySet().iterator(); it.hasNext();) {
                final File directory = (File) it.next();
                if (directory.exists()) {
                    onStart(directory);
                    
                    Entry root;
                    synchronized (mutex) {
                        root = (Entry)entries.get(directory);
                        if (root == null) {
                            root = new Entry(directory, directory);
                            entries.put(directory, root);
                        }
                    }
                    
                    check(directory, root, false);
                    onStop(directory);
                }
            }

            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
            }
        }
    }
}