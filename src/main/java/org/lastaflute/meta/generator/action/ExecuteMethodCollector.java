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
package org.lastaflute.meta.generator.action;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfCollectionUtil.AccordingToOrderResource;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfStringUtil;
import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.di.core.LaContainer;
import org.lastaflute.di.core.factory.SingletonLaContainerFactory;
import org.lastaflute.meta.reflector.SourceParserReflector;
import org.lastaflute.web.Execute;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ActionMapping;
import org.lastaflute.web.ruts.config.ModuleConfig;
import org.lastaflute.web.util.LaModuleConfigUtil;

/**
 * @author jflute
 * @since 0.5.1 split from ActionDocumentGenerator (2021/05/23 Sunday)
 */
public class ExecuteMethodCollector {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final List<String> srcDirList; // not null
    protected final OptionalThing<SourceParserReflector> sourceParserReflector; // not null
    protected final Predicate<ActionExecute> exceptingPredicate; // not null, application selection option

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ExecuteMethodCollector(List<String> srcDirList, OptionalThing<SourceParserReflector> sourceParserReflector,
            Predicate<ActionExecute> exceptingPredicate) {
        this.srcDirList = srcDirList;
        this.sourceParserReflector = sourceParserReflector;
        this.exceptingPredicate = exceptingPredicate;
    }

    // ===================================================================================
    //                                                                        Execute List
    //                                                                        ============
    public List<ActionExecute> collectActionExecuteList() { // the list is per execute method
        final List<ActionExecute> actionExecuteList = new ArrayList<>();
        final ModuleConfig moduleConfig = LaModuleConfigUtil.getModuleConfig(); // to find action mapping
        findActionComponentNameList().forEach(componentName -> { // per action class
            moduleConfig.findActionMapping(componentName).alwaysPresent(actionMapping -> {
                final List<ActionExecute> candidateExecuteList = new ArrayList<>(actionMapping.getExecuteList());
                orderBySourceIfPossible(candidateExecuteList, actionMapping);
                candidateExecuteList.stream().filter(ex -> !exceptingPredicate.test(ex)).forEach(execute -> {
                    actionExecuteList.add(execute);
                });
            });
        });
        return actionExecuteList;
    }

    // ===================================================================================
    //                                                                      Component Name
    //                                                                      ==============
    protected List<String> findActionComponentNameList() {
        final List<String> componentNameList = DfCollectionUtil.newArrayList();
        final LaContainer container = getRootContainer();
        srcDirList.stream().filter(srcDir -> Paths.get(srcDir).toFile().exists()).forEach(srcDir -> {
            try (Stream<Path> stream = Files.find(Paths.get(srcDir), Integer.MAX_VALUE, (path, attr) -> {
                return path.toString().endsWith("Action.java");
            })) {
                stream.sorted().map(path -> {
                    final String className = extractActionClassName(path, srcDir);
                    return DfReflectionUtil.forName(className);
                }).filter(clazz -> !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())).forEach(clazz -> {
                    final String componentName = container.getComponentDef(clazz).getComponentName();
                    if (componentName != null && !componentNameList.contains(componentName)) {
                        componentNameList.add(componentName);
                    }
                });
            } catch (IOException e) {
                throw new IllegalStateException("Failed to find the components: " + srcDir, e);
            }
        });
        IntStream.range(0, container.getComponentDefSize()).forEach(index -> {
            final ComponentDef componentDef = container.getComponentDef(index);
            final String componentName = componentDef.getComponentName();
            if (componentName.endsWith("Action") && !componentNameList.contains(componentName)) {
                componentNameList.add(componentDef.getComponentName());
            }
        });
        return componentNameList;
    }

    protected LaContainer getRootContainer() {
        return SingletonLaContainerFactory.getContainer().getRoot();
    }

    protected String extractActionClassName(Path path, String srcDir) { // for forName()
        String className = DfStringUtil.substringFirstRear(path.toFile().getAbsolutePath(), new File(srcDir).getAbsolutePath());
        if (className.startsWith(File.separator)) {
            className = className.substring(1);
        }
        className = DfStringUtil.substringLastFront(className, ".java").replace(File.separatorChar, '.');
        return className;
    }

    // ===================================================================================
    //                                                                        Source Order
    //                                                                        ============
    protected void orderBySourceIfPossible(List<ActionExecute> candidateExecuteList, ActionMapping actionMapping) {
        final List<Method> sourceOrderList = extractSourceOrderedExecuteMethodList(actionMapping);
        if (!sourceOrderList.isEmpty()) {
            // reflecting source order (list from mapping is reflection random)
            // source order on LastaDoc, however SwaggerUI has own order (by URL)
            final AccordingToOrderResource<ActionExecute, Method> resource = new AccordingToOrderResource<>();
            resource.setupResource(sourceOrderList, exec -> exec.getExecuteMethod());
            DfCollectionUtil.orderAccordingTo(candidateExecuteList, resource); // as source order
        }
    }

    protected List<Method> extractSourceOrderedExecuteMethodList(ActionMapping actionMapping) {
        final List<Method> sourceOrderList = DfCollectionUtil.newArrayList();
        sourceParserReflector.ifPresent(sourceParserReflector -> {
            final Class<?> actionClass = actionMapping.getActionDef().getComponentClass();
            final List<Method> allMethodList = sourceParserReflector.getMethodListOrderByDefinition(actionClass);
            allMethodList.stream().filter(mt -> mt.getAnnotation(Execute.class) != null).forEach(mt -> {
                sourceOrderList.add(mt);
            });
        });
        return sourceOrderList;
    }
}
