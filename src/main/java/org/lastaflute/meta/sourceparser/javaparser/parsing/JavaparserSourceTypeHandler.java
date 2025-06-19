/*
 * Copyright 2015-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.lastaflute.meta.sourceparser.javaparser.parsing;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from JavaparserSourceParserReflector (2021/05/31 Monday)
 */
public class JavaparserSourceTypeHandler {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    /** The key is class name (FQCN). (NotNull) */
    protected static final Map<String, CachedCompilationUnit> cachedCompilationUnitMap = DfCollectionUtil.newHashMap();

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The list of source directory as string path can be uesd for File. (NotNull) */
    protected final List<String> srcDirList;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public JavaparserSourceTypeHandler(List<String> srcDirList) {
        this.srcDirList = srcDirList;
    }

    // ===================================================================================
    //                                                                         Parse Class
    //                                                                         ===========
    public OptionalThing<CompilationUnit> parseClass(Class<?> clazz) {
        final String fqcn = clazz.getName();
        final JavaParser javaParser = new JavaParser();
        for (String srcDir : srcDirList) {
            final String fileSeparatedFqcn = fqcn.replace('.', File.separatorChar);
            File file = new File(srcDir, fileSeparatedFqcn + ".java");
            if (!file.exists()) {
                file = new File(srcDir, fileSeparatedFqcn.replaceAll("\\$.*", "") + ".java");
                if (!file.exists()) {
                    continue; // not found, search it in the next directory
                }
            }
            if (cachedCompilationUnitMap.containsKey(fqcn)) {
                final CachedCompilationUnit foundUnit = cachedCompilationUnitMap.get(clazz.getName());
                if (foundUnit != null) { // cache hit!
                    if (isNoChangedFile(file, foundUnit)) { // can use the cache
                        return OptionalThing.of(foundUnit.compilationUnit);
                    }
                }
            }

            final CachedCompilationUnit cachedCompilationUnit = new CachedCompilationUnit();
            cachedCompilationUnit.fileLastModified = file.lastModified();
            cachedCompilationUnit.fileLength = file.length();
            try {
                // all meta data (that contains fields and methods and comments and ...)
                // is parsed here and the result has whole data (so heavy)
                // attention, to avoid big memory
                // // JavaparserSourceTypeHandler, cache big memory problem
                // https://github.com/lastaflute/lasta-meta/issues/24
                final ParseResult<CompilationUnit> result = javaParser.parse(file);
                result.getResult().ifPresent(compilationUnit -> { // basically present?
                    cachedCompilationUnit.compilationUnit = compilationUnit;
                });
            } catch (FileNotFoundException e) { // no way, already checked existence
                throw new IllegalStateException("Source file did not exist: file=" + file);
            }

            if (cachedCompilationUnit.compilationUnit != null) { // basically present?
                cachedCompilationUnitMap.put(fqcn, cachedCompilationUnit);
                return OptionalThing.of(cachedCompilationUnit.compilationUnit);
            }
            // not found by java parser?
            // so next anyway
        }

        // completely not found in the source directories
        return OptionalThing.ofNullable(null, () -> {
            String msg = "The source file did not exist: clazz=" + fqcn + ", srcDirList=" + srcDirList;
            throw new IllegalStateException(msg);
        });
    }

    protected boolean isNoChangedFile(File file, CachedCompilationUnit cachedCompilationUnit) {
        return cachedCompilationUnit.fileLastModified == file.lastModified() // updated?
                && cachedCompilationUnit.fileLength == file.length(); // size changed?
    }

    // ===================================================================================
    //                                                                               Cache
    //                                                                               =====
    /**
     * @author p1us2er0
     * @author jflute
     */
    protected static class CachedCompilationUnit {

        /** file last modified. */
        private long fileLastModified;

        /** file length. */
        private long fileLength;

        /** compilation unit. (NotNull: after cache setup) */
        private CompilationUnit compilationUnit;
    }
}
