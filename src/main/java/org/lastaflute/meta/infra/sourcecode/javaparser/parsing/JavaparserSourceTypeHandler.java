/*
 * Copyright 2015-2021 the original author or authors.
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
package org.lastaflute.meta.infra.sourcecode.javaparser.parsing;

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
        JavaParser javaParser = new JavaParser();
        for (String srcDir : srcDirList) {
            File file = new File(srcDir, clazz.getName().replace('.', File.separatorChar) + ".java");
            if (!file.exists()) {
                file = new File(srcDir, clazz.getName().replace('.', File.separatorChar).replaceAll("\\$.*", "") + ".java");
                if (!file.exists()) {
                    continue;
                }
            }
            if (cachedCompilationUnitMap.containsKey(clazz.getName())) {
                CachedCompilationUnit cachedCompilationUnit = cachedCompilationUnitMap.get(clazz.getName());
                if (cachedCompilationUnit != null && cachedCompilationUnit.fileLastModified == file.lastModified()
                        && cachedCompilationUnit.fileLength == file.length()) {
                    return OptionalThing.of(cachedCompilationUnit.compilationUnit);
                }
            }

            CachedCompilationUnit cachedCompilationUnit = new CachedCompilationUnit();
            cachedCompilationUnit.fileLastModified = file.lastModified();
            cachedCompilationUnit.fileLength = file.length();
            try {
                ParseResult<CompilationUnit> parse = javaParser.parse(file);
                parse.getResult().ifPresent(compilationUnit -> {
                    cachedCompilationUnit.compilationUnit = compilationUnit;
                });
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("Source file don't exist.");
            }

            cachedCompilationUnitMap.put(clazz.getName(), cachedCompilationUnit);
            return OptionalThing.of(cachedCompilationUnit.compilationUnit);
        }
        return OptionalThing.ofNullable(null, () -> {
            throw new IllegalStateException("Source file don't exist.");
        });
    }

    /**
     * @author p1us2er0
     */
    private static class CachedCompilationUnit {

        /** file last modified. */
        private long fileLastModified;

        /** file length. */
        private long fileLength;

        /** compilation unit. */
        private CompilationUnit compilationUnit;
    }
}
