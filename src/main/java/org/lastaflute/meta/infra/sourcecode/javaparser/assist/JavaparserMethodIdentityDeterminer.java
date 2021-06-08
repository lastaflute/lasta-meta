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
package org.lastaflute.meta.infra.sourcecode.javaparser.assist;

import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.ast.body.MethodDeclaration;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from JavaparserSourceParserReflector (2021/06/04 Friday)
 */
public class JavaparserMethodIdentityDeterminer {

    public boolean matchesMethod(Method method, MethodDeclaration dec) {
        return buildMethodIdentityNative(method).equals(buildMethodIdentitySource(dec));
    }

    public String buildMethodIdentityNative(Method method) { // e.g. get$index(ProductsSearchForm)
        final String methodName = method.getName();
        final String paramExp = Stream.of(method.getParameters()).map(pr -> pr.getType().getSimpleName()).collect(Collectors.joining(","));
        return doBuildMethodIdentity(methodName, paramExp);
    }

    public String buildMethodIdentitySource(MethodDeclaration dec) { // e.g. get$index(ProductsSearchForm)
        final String methodName = dec.getNameAsString();
        final String paramExp = dec.getParameters().stream().map(pr -> pr.getType().toString()).collect(Collectors.joining(","));
        return doBuildMethodIdentity(methodName, paramExp);
    }

    protected String doBuildMethodIdentity(String methodName, String paramExp) {
        return methodName + "(" + paramExp + ")";
    }
}
