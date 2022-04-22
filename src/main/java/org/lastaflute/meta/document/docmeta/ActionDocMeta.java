/*
 * Copyright 2015-2022 the original author or authors.
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
package org.lastaflute.meta.document.docmeta;

import java.lang.annotation.Annotation;
import java.util.List;

import org.lastaflute.core.util.Lato;
import org.lastaflute.web.ruts.config.ActionExecute;

/**
 * The document meta of action, per execute method.
 * @author p1us2er0
 * @author jflute
 * @since 0.5.0-sp9 (2015/09/18 Friday)
 */
public class ActionDocMeta {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                           Action Item
    //                                           -----------
    /** The url of the execute method. e.g. /sea/land/ (NotNull: after setup) */
    private String url;

    // -----------------------------------------------------
    //                                            Class Item
    //                                            ----------
    // #giveup jflute rename to actionType (2021/06/25) but unknown scope so keep compatible completely (2022/04/22)
    /** The type declaring the execute method. e.g. org.docksidestage.app.web.sea.SeaAction.class (NotNull: after setup) */
    private transient Class<?> type; // exclude with gson serialize.

    /** The full name of type declaring the execute method. e.g. "org.docksidestage.app.web.sea.SeaAction" (NotNull: after setup) */
    private String typeName;

    /** The simple name of type declaring the execute method. e.g. "SeaAction" (NotNull: after setup) */
    private String simpleTypeName;

    /** The whole comment about action method, e.g. class javadoc + method javadoc. (NullAllowed: depends on java parser) */
    private String description; // basically extracted by java parser

    /** The javadoc of Action class. (NullAllowed: depends on java parser) */
    private String typeComment; // basically extracted by java parser

    // -----------------------------------------------------
    //                                            Field Item
    //                                            ----------
    /** The list of field meta, in method declaring class. (NotNull: after setup) */
    private List<TypeDocMeta> fieldTypeDocMetaList;

    // -----------------------------------------------------
    //                                           Method Item
    //                                           -----------
    /** The object of the execute method. (NotNull: after setup) */
    private transient ActionExecute actionExecute; // exclude with gson serialize.

    /** The method name of action execute. e.g. org.docksidestage.app.web.sea.SeaAction@land() (NotNull: after setup) */
    private String methodName; // contains HTTP method if RESTful e.g. "get$index"

    /** The method comment of action execute. e.g. "Let's go to land" (NullAllowed: depends on java parser) */
    private String methodComment; // basically extracted by java parser

    // -----------------------------------------------------
    //                                       Annotation Item
    //                                       ---------------
    /** The list of annotation type defined at both action and execute method. e.g. (NotNull: after setup) */
    private transient List<Annotation> annotationTypeList; // exclude with gson serialize.

    /** annotation list. e.g. ["Required", "Length{max\u003d5}"] (NotNull: after setup) */
    private List<String> annotationList;

    // -----------------------------------------------------
    //                                           IN/OUT Item
    //                                           -----------
    /** The List of meta object for path parameters of execute method. (NotNull: after setup, EmptyAllowed) */
    private List<TypeDocMeta> parameterTypeDocMetaList;

    /** The meta object for action form. (NullAllowed: if no form action) */
    private TypeDocMeta formTypeDocMeta;

    /** The meta object for action response (return). (NotNull) */
    private TypeDocMeta returnTypeDocMeta;

    // -----------------------------------------------------
    //                                           Source Item
    //                                           -----------
    /** The line count of the action class file. (NullAllowed: depends on java parser) */
    private Integer fileLineCount; // basically extracted by java parser

    /** The line count of the action method. (NullAllowed: depends on java parser) */
    private Integer methodLineCount; // basically extracted by java parser

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return Lato.string(this);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    // -----------------------------------------------------
    //                                           Action Item
    //                                           -----------
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // -----------------------------------------------------
    //                                            Class Item
    //                                            ----------
    public Class<?> getType() { // means action type
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getSimpleTypeName() {
        return simpleTypeName;
    }

    public void setSimpleTypeName(String simpleTypeName) {
        this.simpleTypeName = simpleTypeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTypeComment() {
        return typeComment;
    }

    public void setTypeComment(String typeComment) {
        this.typeComment = typeComment;
    }

    // -----------------------------------------------------
    //                                            Field Item
    //                                            ----------
    public List<TypeDocMeta> getFieldTypeDocMetaList() {
        return fieldTypeDocMetaList;
    }

    public void setFieldTypeDocMetaList(List<TypeDocMeta> fieldTypeDocMetaList) {
        this.fieldTypeDocMetaList = fieldTypeDocMetaList;
    }

    // -----------------------------------------------------
    //                                           Method Item
    //                                           -----------
    public ActionExecute getActionExecute() {
        return actionExecute;
    }

    public void setActionExecute(ActionExecute actionExecute) {
        this.actionExecute = actionExecute;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodComment() {
        return methodComment;
    }

    public void setMethodComment(String methodComment) {
        this.methodComment = methodComment;
    }

    // -----------------------------------------------------
    //                                       Annotation Item
    //                                       ---------------
    public List<Annotation> getAnnotationTypeList() {
        return annotationTypeList;
    }

    public void setAnnotationTypeList(List<Annotation> annotationTypeList) {
        this.annotationTypeList = annotationTypeList;
    }

    public List<String> getAnnotationList() {
        return annotationList;
    }

    public void setAnnotationList(List<String> annotationList) {
        this.annotationList = annotationList;
    }

    // -----------------------------------------------------
    //                                           IN/OUT Item
    //                                           -----------
    public List<TypeDocMeta> getParameterTypeDocMetaList() {
        return parameterTypeDocMetaList;
    }

    public void setParameterTypeDocMetaList(List<TypeDocMeta> parameterTypeDocMetList) {
        this.parameterTypeDocMetaList = parameterTypeDocMetList;
    }

    public TypeDocMeta getFormTypeDocMeta() {
        return formTypeDocMeta;
    }

    public void setFormTypeDocMeta(TypeDocMeta formTypeDocMeta) {
        this.formTypeDocMeta = formTypeDocMeta;
    }

    public TypeDocMeta getReturnTypeDocMeta() {
        return returnTypeDocMeta;
    }

    public void setReturnTypeDocMeta(TypeDocMeta returnTypeDocMeta) {
        this.returnTypeDocMeta = returnTypeDocMeta;
    }

    // -----------------------------------------------------
    //                                           Source Item
    //                                           -----------
    public Integer getFileLineCount() {
        return fileLineCount;
    }

    public void setFileLineCount(Integer fileLineCount) {
        this.fileLineCount = fileLineCount;
    }

    public Integer getMethodLineCount() {
        return methodLineCount;
    }

    public void setMethodLineCount(Integer methodLineCount) {
        this.methodLineCount = methodLineCount;
    }
}
