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
package org.lastaflute.meta.swagger.diff.node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.dbflute.util.Srl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author jflute
 * @since 0.5.1 (2021/07/08 Thursday)
 */
public class SwaggerDiffNodePathFilter {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(SwaggerDiffNodePathFilter.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected boolean pathTrailingSlashDeleted;
    protected final List<String> exceptedPathPrefixList = new ArrayList<>();
    protected final List<String> exceptedPathResponseContentTypeList = new ArrayList<>();

    // ===================================================================================
    //                                                                              Option
    //                                                                              ======
    public SwaggerDiffNodePathFilter deletePathTrailingSlash() {
        pathTrailingSlashDeleted = true;
        return this;
    }

    public SwaggerDiffNodePathFilter exceptPathByPrefix(String pathPrefix) {
        exceptedPathPrefixList.add(pathPrefix);
        return this;
    }

    public SwaggerDiffNodePathFilter exceptPathByResponseContentType(String contentType) {
        exceptedPathResponseContentTypeList.add(contentType);
        return this;
    }

    // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
    // *add condition to following determination if you add new option
    // _/_/_/_/_/_/_/_/_/_/

    protected boolean needsFiltering() {
        return pathTrailingSlashDeleted || !exceptedPathPrefixList.isEmpty() || !exceptedPathResponseContentTypeList.isEmpty();
    }

    // ===================================================================================
    //                                                                         Filter Path
    //                                                                         ===========
    public void filterPathIfNeeds(JsonNode rootNode) {
        if (!needsFiltering()) {
            return;
        }
        final JsonNode foundNode = rootNode.findValue("paths");
        if (foundNode == null || !foundNode.isObject()) {
            logger.debug("Not found the 'paths' node so cannot remove HTML/Stream path: foundNode=" + foundNode);
            return;
        }
        final ObjectNode pathsNode = (ObjectNode) foundNode;
        if (pathsNode != null) {
            if (pathTrailingSlashDeleted) {
                removeTrailingSlash(pathsNode);
            }
            final List<String> apiPathList = extractApiPathList(pathsNode);
            for (String apiPath : apiPathList) {
                final JsonNode currentJsonNode = pathsNode.get(apiPath);
                if (isExceptPath(apiPath)) {
                    pathsNode.remove(apiPath);
                } else {
                    doFilterPath(pathsNode, apiPath, currentJsonNode);
                }
            }
        }
    }

    protected void doFilterPath(ObjectNode pathsNode, String apiPath, JsonNode currentJsonNode) {
        if (!(currentJsonNode instanceof ObjectNode)) {
            return;
        }
        final ObjectNode currentObjNode = (ObjectNode) currentJsonNode;
        if (determineExceptedNode(currentObjNode)) {
            pathsNode.remove(apiPath);
        } else { // go to nest
            final Iterator<Entry<String, JsonNode>> fields = currentObjNode.fields();
            while (fields.hasNext()) {
                final JsonNode nextNode = fields.next().getValue();
                doFilterPath(pathsNode, apiPath, nextNode); // recursive
            }
        }
    }

    // ===================================================================================
    //                                                                      Trailing Slash
    //                                                                      ==============
    protected void removeTrailingSlash(ObjectNode pathsNode) {
        final List<String> apiPathList = extractApiPathList(pathsNode);
        for (String apiPath : apiPathList) {
            JsonNode currentNode = pathsNode.get(apiPath);
            final String newPath = apiPath.endsWith("/") ? Srl.rtrim(apiPath, "/") : apiPath;
            pathsNode.remove(apiPath); // by old path
            pathsNode.set(newPath, currentNode); // by new path
        }
    }

    // ===================================================================================
    //                                                                         Except Path
    //                                                                         ===========
    protected boolean isExceptPath(String apiPath) {
        for (String pathPrefix : exceptedPathPrefixList) {
            if (apiPath.startsWith(pathPrefix)) {
                return true;
            }
        }
        return false;
    }

    // ===================================================================================
    //                                                                      Except by Node
    //                                                                      ==============
    protected boolean determineExceptedNode(ObjectNode currentObjNode) {
        return hasExceptedResponsesContentType(currentObjNode) // e.g. OpenAPI 3.0
                || hasExceptedProduces(currentObjNode); // e.g. swagger-2.0 (Lasta)
    }

    protected boolean hasExceptedResponsesContentType(ObjectNode currentObjNode) { // null allowed
        final JsonNode responsesJsonNode = currentObjNode.findValue("responses");
        if (responsesJsonNode == null || !responsesJsonNode.isObject()) {
            return false;
        }
        final ObjectNode responseObjNode = (ObjectNode) responsesJsonNode;
        for (String contentType : exceptedPathResponseContentTypeList) {
            final JsonNode exceptedNode = responseObjNode.findValue(contentType);
            if (exceptedNode != null) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasExceptedProduces(ObjectNode currentObjNode) {
        // supposing lasta-presents swagger.json
        final JsonNode produces = currentObjNode.findValue("produces");
        if (produces != null && produces.isArray()) { // should be array
            final ArrayNode arrayNode = (ArrayNode) produces;
            final Iterator<JsonNode> elements = arrayNode.elements();
            while (elements.hasNext()) {
                final JsonNode jsonNode = elements.next();
                for (String contentType : exceptedPathResponseContentTypeList) {
                    if (contentType.equals(jsonNode.textValue())) { // found
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected List<String> extractApiPathList(ObjectNode pathsNode) {
        final List<String> apiPathList = new ArrayList<>(); // to avoid concurrent modification
        pathsNode.fieldNames().forEachRemaining(name -> apiPathList.add(name));
        return apiPathList;
    }
}
