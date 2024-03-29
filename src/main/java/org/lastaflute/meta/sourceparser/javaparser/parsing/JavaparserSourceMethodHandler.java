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

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfCollectionUtil.AccordingToOrderResource;
import org.lastaflute.meta.sourceparser.javaparser.assist.JavaparserMethodIdentityDeterminer;

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
        // #needs_fix jflute anonymous classes headache (2024/02/28)
        /*
        e.g. SwaggerAction
        op.derivedPathSummary(new SwaggerPathSummaryDeriver() {
        public String derive(ActionDocReference actionDocReference, String defaultSummary) {
            return defaultSummary;
        }
        });
        op.derivedPathDescription(new SwaggerPathDescriptionDeriver() {
        public String derive(ActionDocReference actionDocReference, String defaultDescription) {
            return defaultDescription;
        }
        });
        
        java.lang.IllegalStateException: The id was duplicated: id=derive(ActionDocReference, String) orderedUniqueIdList=[index(), json(), derive(ActionDocReference, String), derive(ActionDocReference, String), appjson(), diff(SwaggerDiffForm), targetJson(SwaggerTargetJsonForm), limitedTargetJson(), verifySwaggerAllowed()]
         at org.dbflute.util.DfCollectionUtil.orderAccordingTo(DfCollectionUtil.java:499)
         at org.lastaflute.meta.sourceparser.javaparser.parsing.JavaparserSourceMethodHandler.orderMethodListBySource(JavaparserSourceMethodHandler.java:70)
         at org.lastaflute.meta.sourceparser.javaparser.parsing.JavaparserSourceMethodHandler.getMethodListOrderByDefinition(JavaparserSourceMethodHandler.java:56)
         */
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
