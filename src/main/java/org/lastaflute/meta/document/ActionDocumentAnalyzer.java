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
package org.lastaflute.meta.document;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.meta.document.docmeta.ActionDocMeta;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.document.parts.action.ExecuteMethodCollector;
import org.lastaflute.meta.document.parts.action.FormFieldNameAdjuster;
import org.lastaflute.meta.document.parts.type.NativeDataTypeProvider;
import org.lastaflute.meta.document.zone.formtype.ExecuteFormTypeAnalyzer;
import org.lastaflute.meta.document.zone.parameter.ExecuteParameterAnalyzer;
import org.lastaflute.meta.document.zone.returntype.ExecuteReturnTypeAnalyzer;
import org.lastaflute.meta.infra.json.MetauseJsonEngineProvider;
import org.lastaflute.meta.sourceparser.SourceParserReflector;
import org.lastaflute.web.UrlChain;
import org.lastaflute.web.path.ActionPathResolver;
import org.lastaflute.web.ruts.config.ActionExecute;

// package of this class should be under lastaflute but no fix for compatible
/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.0-sp9 of UTFlute (2015/09/18 Friday)
 */
public class ActionDocumentAnalyzer extends BaseDocumentAnalyzer {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The list of source directory. (NotNull) */
    protected final List<String> srcDirList;

    /** depth of analyzed target, to avoid cyclic analyzing. */
    protected final int depth;

    /** The optional reflector of source parser, e.g. java parser. (NotNull, EmptyAllowed) */
    protected final OptionalThing<SourceParserReflector> sourceParserReflector;

    // parts
    protected final MetauseJsonEngineProvider metauseJsonEngineProvider;
    protected final NativeDataTypeProvider nativeDataTypeProvider;
    protected final FormFieldNameAdjuster formFieldNameAdjuster;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionDocumentAnalyzer(List<String> srcDirList, int depth, OptionalThing<SourceParserReflector> sourceParserReflector) {
        this.srcDirList = srcDirList;
        this.depth = depth;
        this.sourceParserReflector = sourceParserReflector;

        // parts
        this.metauseJsonEngineProvider = newMetauseJsonEngineProvider();
        this.nativeDataTypeProvider = newDataNativeTypeProvider();
        this.formFieldNameAdjuster = newFormFieldNameAdjuster(metauseJsonEngineProvider);
    }

    protected MetauseJsonEngineProvider newMetauseJsonEngineProvider() {
        return new MetauseJsonEngineProvider();
    }

    protected NativeDataTypeProvider newDataNativeTypeProvider() {
        return new NativeDataTypeProvider();
    }

    protected FormFieldNameAdjuster newFormFieldNameAdjuster(MetauseJsonEngineProvider metauseJsonEngineProvider) {
        return new FormFieldNameAdjuster(metauseJsonEngineProvider);
    }

    // ===================================================================================
    //                                                                            Generate
    //                                                                            ========
    public List<ActionDocMeta> analyzeAction() { // the list is per execute method
        return createExecuteMethodCollector().collectActionExecuteList().stream().map(execute -> {
            return createActionDocMeta(execute);
        }).collect(Collectors.toList());
    }

    protected ExecuteMethodCollector createExecuteMethodCollector() {
        return new ExecuteMethodCollector(srcDirList, sourceParserReflector, execute -> {
            return exceptsActionExecute(execute);
        });
    }

    protected boolean exceptsActionExecute(ActionExecute actionExecute) { // may be overridden
        return false;
    }

    // ===================================================================================
    //                                                                      Action DocMeta
    //                                                                      ==============
    protected ActionDocMeta createActionDocMeta(ActionExecute execute) {
        final ActionDocMeta actionDocMeta = new ActionDocMeta(); // per execute method
        final Class<?> actionClass = execute.getActionMapping().getActionDef().getComponentClass();
        final UrlChain urlChain = prepareUrlChain(execute, actionClass);

        setupActionItem(actionDocMeta, actionClass, urlChain);

        final Method executeMethod = execute.getExecuteMethod();
        final Class<?> methodDeclaringClass = executeMethod.getDeclaringClass(); // basically same as componentClass

        setupClassItem(actionDocMeta, methodDeclaringClass);
        setupFieldItem(actionDocMeta, methodDeclaringClass);
        setupMethodItem(actionDocMeta, execute, executeMethod);
        setupAnnotationItem(actionDocMeta, executeMethod, methodDeclaringClass);
        setupInOutItem(actionDocMeta, execute, executeMethod);

        // extension item (url, return, comment...)
        sourceParserReflector.ifPresent(sourceParserReflector -> {
            sourceParserReflector.reflect(actionDocMeta, executeMethod);
        });

        return actionDocMeta;
    }

    protected UrlChain prepareUrlChain(ActionExecute execute, Class<?> actionClass) {
        final UrlChain urlChain = new UrlChain(actionClass);
        final String urlPattern = execute.getPreparedUrlPattern().getResolvedUrlPattern();
        if (!"index".equals(urlPattern)) {
            urlChain.moreUrl(urlPattern);
        }
        return urlChain;
    }

    // -----------------------------------------------------
    //                                          Action/Class
    //                                          ------------
    protected void setupActionItem(ActionDocMeta actionDocMeta, Class<?> actionClass, UrlChain urlChain) {
        final ActionPathResolver actionPathResolver = ContainerUtil.getComponent(ActionPathResolver.class);
        actionDocMeta.setUrl(actionPathResolver.toActionUrl(actionClass, urlChain));
    }

    protected void setupClassItem(ActionDocMeta actionDocMeta, Class<?> methodDeclaringClass) {
        actionDocMeta.setType(methodDeclaringClass);
        actionDocMeta.setTypeName(adjustTypeName(methodDeclaringClass));
        actionDocMeta.setSimpleTypeName(adjustSimpleTypeName(methodDeclaringClass));
    }

    // -----------------------------------------------------
    //                                          Field/Method
    //                                          ------------
    protected void setupFieldItem(ActionDocMeta actionDocMeta, Class<?> methodDeclaringClass) {
        // #thinking jflute does it contain private DI fields? needed? (2021/06/26)
        actionDocMeta.setFieldTypeDocMetaList(Arrays.stream(methodDeclaringClass.getDeclaredFields()).map(field -> {
            final TypeDocMeta typeDocMeta = new TypeDocMeta();

            // #thinking jflute maybe this is for sastruts style, already unneeded? (2021/06/26)
            typeDocMeta.setName(formFieldNameAdjuster.adjustFieldName(methodDeclaringClass, field));
            typeDocMeta.setPublicName(formFieldNameAdjuster.adjustPublicFieldName(/*clazz*/null, field)); // why null?

            typeDocMeta.setType(field.getType());
            typeDocMeta.setTypeName(adjustTypeName(field.getGenericType()));
            typeDocMeta.setSimpleTypeName(adjustSimpleTypeName((field.getGenericType())));
            typeDocMeta.setAnnotationTypeList(Arrays.asList(field.getAnnotations()));
            typeDocMeta.setAnnotationList(arrangeAnnotationList(typeDocMeta.getAnnotationTypeList()));

            sourceParserReflector.ifPresent(sourceParserReflector -> {
                sourceParserReflector.reflect(typeDocMeta, field.getType());
            });
            return typeDocMeta;
        }).collect(Collectors.toList()));
    }

    protected void setupMethodItem(ActionDocMeta actionDocMeta, ActionExecute execute, Method executeMethod) {
        actionDocMeta.setActionExecute(execute);
        actionDocMeta.setMethodName(executeMethod.getName());
    }

    // -----------------------------------------------------
    //                                            Annotation
    //                                            ----------
    protected void setupAnnotationItem(ActionDocMeta actionDocMeta, Method executeMethod, Class<?> methodDeclaringClass) {
        final List<Annotation> annotationList = DfCollectionUtil.newArrayList();
        annotationList.addAll(Arrays.asList(methodDeclaringClass.getAnnotations()));
        annotationList.addAll(Arrays.asList(executeMethod.getAnnotations()));
        actionDocMeta.setAnnotationTypeList(annotationList); // contains both action and execute method
        actionDocMeta.setAnnotationList(arrangeAnnotationList(annotationList));
    }

    // -----------------------------------------------------
    //                                                IN/OUT
    //                                                ------
    protected void setupInOutItem(ActionDocMeta actionDocMeta, ActionExecute execute, Method executeMethod) {
        final List<TypeDocMeta> parameterTypeDocMetaList = DfCollectionUtil.newArrayList();
        Arrays.stream(executeMethod.getParameters()).filter(parameter -> {
            return !(execute.getFormMeta().isPresent() && execute.getFormMeta().get().getSymbolFormType().equals(parameter.getType()));
        }).forEach(parameter -> { // except form parameter here
            actionDocMeta.setUrl(buildNewActionUrl(actionDocMeta, parameter));
            parameterTypeDocMetaList.add(analyzeMethodParameter(parameter));
        });
        actionDocMeta.setParameterTypeDocMetaList(parameterTypeDocMetaList);
        analyzeFormClass(execute).ifPresent(formTypeDocMeta -> {
            actionDocMeta.setFormTypeDocMeta(formTypeDocMeta);
        });
        actionDocMeta.setReturnTypeDocMeta(analyzeReturnClass(executeMethod));
    }

    protected String buildNewActionUrl(ActionDocMeta actionDocMeta, Parameter parameter) {
        final StringBuilder builder = new StringBuilder();
        builder.append("{").append(parameter.getName()).append("}");
        return actionDocMeta.getUrl().replaceFirst("\\{\\}", builder.toString());
    }

    // -----------------------------------------------------
    //                                     Analyze Parameter
    //                                     -----------------
    protected TypeDocMeta analyzeMethodParameter(Parameter parameter) {
        return createExecuteParameterAnalyzer().analyzeMethodParameter(parameter);
    }

    protected ExecuteParameterAnalyzer createExecuteParameterAnalyzer() {
        return new ExecuteParameterAnalyzer(sourceParserReflector, metaAnnotationArranger, metaTypeNameAdjuster);
    }

    // -----------------------------------------------------
    //                                          Analyze Form
    //                                          ------------
    protected OptionalThing<TypeDocMeta> analyzeFormClass(ActionExecute execute) {
        return createExecuteFormTypeAnalyzer().analyzeFormClass(execute);
    }

    protected ExecuteFormTypeAnalyzer createExecuteFormTypeAnalyzer() {
        return new ExecuteFormTypeAnalyzer(depth, sourceParserReflector, metaAnnotationArranger, metaTypeNameAdjuster,
                formFieldNameAdjuster);
    }

    // -----------------------------------------------------
    //                                        Analyze Return
    //                                        --------------
    protected TypeDocMeta analyzeReturnClass(Method method) {
        return createExecuteReturnTypeAnalyzer().analyzeReturnClass(method);
    }

    protected ExecuteReturnTypeAnalyzer createExecuteReturnTypeAnalyzer() {
        return new ExecuteReturnTypeAnalyzer(depth, sourceParserReflector, metaAnnotationArranger, metaTypeNameAdjuster,
                nativeDataTypeProvider, formFieldNameAdjuster);
    }

    // ===================================================================================
    //                                                                     Action Property
    //                                                                     ===============
    // #thinking jflute unused so comment it out, unneeded? (2021/06/26)
    //protected Map<String, String> convertPropertyNameMap(String parentName, TypeDocMeta typeDocMeta) {
    //    if (typeDocMeta == null) {
    //        return DfCollectionUtil.newLinkedHashMap();
    //    }
    //
    //    final Map<String, String> propertyNameMap = DfCollectionUtil.newLinkedHashMap();
    //
    //    final String name = calculateName(parentName, typeDocMeta.getName(), typeDocMeta.getTypeName());
    //    if (DfStringUtil.is_NotNull_and_NotEmpty(name)) {
    //        propertyNameMap.put(name, "");
    //    }
    //
    //    if (typeDocMeta.getNestTypeDocMetaList() != null) {
    //        typeDocMeta.getNestTypeDocMetaList().forEach(nestDocMeta -> {
    //            propertyNameMap.putAll(convertPropertyNameMap(name, nestDocMeta));
    //        });
    //    }
    //
    //    return propertyNameMap;
    //}
    //
    //protected String calculateName(String parentName, String name, String type) {
    //    if (DfStringUtil.is_Null_or_Empty(name)) {
    //        return null;
    //    }
    //
    //    final StringBuilder builder = new StringBuilder();
    //    if (DfStringUtil.is_NotNull_and_NotEmpty(parentName)) {
    //        builder.append(parentName + ".");
    //    }
    //    builder.append(name);
    //    if (name.endsWith("List")) {
    //        builder.append("[]");
    //    }
    //
    //    return builder.toString();
    //}
}
