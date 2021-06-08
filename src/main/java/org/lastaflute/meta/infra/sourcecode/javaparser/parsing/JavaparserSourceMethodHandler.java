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

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfCollectionUtil.AccordingToOrderResource;
import org.lastaflute.meta.infra.sourcecode.javaparser.assist.JavaparserMethodIdentityDeterminer;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from JavaparserSourceParserReflector (2021/05/31 Monday)
 */
public class JavaparserSourceMethodHandler {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final JavaparserSourceTypeHandler sourceTypeHandler; // not null
    protected final JavaparserMethodIdentityDeterminer methodIdentityDeterminer; // not null

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public JavaparserSourceMethodHandler(JavaparserSourceTypeHandler sourceTypeHandler,
            JavaparserMethodIdentityDeterminer methodIdentityDeterminer) {
        this.sourceTypeHandler = sourceTypeHandler;
        this.methodIdentityDeterminer = methodIdentityDeterminer;
    }

    // ===================================================================================
    //                                                         Orderd by Source Definition
    //                                                         ===========================
    public List<Method> getMethodListOrderByDefinition(Class<?> clazz) {
        final List<Method> nativeMethodList = DfCollectionUtil.newArrayList(clazz.getMethods()); // mutable for ordering
        orderMethodListBySource(clazz, nativeMethodList); // order by source
        return nativeMethodList;
    }

    protected void orderMethodListBySource(Class<?> clazz, List<Method> nativeMethodList) {
        // identity is e.g. get$index(ProductsSearchForm)
        // parameter type is not FQCN because it is hard to get FQCN from source code
        // so not perfect however almost no problem
        final List<String> sourceIdentityList = extractMethodDeclarationList(clazz).stream().map(dec -> {
            return buildMethodIdentitySource(dec); // e.g. get$index(ProductsSearchForm)
        }).collect(Collectors.toList()); // order master

        final AccordingToOrderResource<Method, String> resource = new AccordingToOrderResource<>();
        resource.setupResource(sourceIdentityList, method -> buildMethodIdentityNative(method));
        DfCollectionUtil.orderAccordingTo(nativeMethodList, resource); // as source order
    }

    protected String buildMethodIdentityNative(Method method) { // e.g. get$index(ProductsSearchForm)
        return methodIdentityDeterminer.buildMethodIdentityNative(method);
    }

    protected String buildMethodIdentitySource(MethodDeclaration dec) { // e.g. get$index(ProductsSearchForm)
        return methodIdentityDeterminer.buildMethodIdentitySource(dec);
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected List<MethodDeclaration> extractMethodDeclarationList(Class<?> clazz) {
        List<MethodDeclaration> methodDeclarationList = DfCollectionUtil.newArrayList();
        sourceTypeHandler.parseClass(clazz).ifPresent(compilationUnit -> {
            VoidVisitorAdapter<Void> adapter = new VoidVisitorAdapter<Void>() {
                public void visit(final MethodDeclaration methodDeclaration, final Void arg) {
                    methodDeclarationList.add(methodDeclaration);
                    super.visit(methodDeclaration, arg);
                }
            };
            adapter.visit(compilationUnit, null);
        });
        return methodDeclarationList;
    }
}
