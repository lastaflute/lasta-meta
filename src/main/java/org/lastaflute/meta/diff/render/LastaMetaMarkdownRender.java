package org.lastaflute.meta.diff.render;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openapitools.openapidiff.core.model.Changed;
import org.openapitools.openapidiff.core.model.ChangedMetadata;
import org.openapitools.openapidiff.core.model.ChangedOperation;
import org.openapitools.openapidiff.core.model.ChangedParameter;
import org.openapitools.openapidiff.core.model.ChangedParameters;
import org.openapitools.openapidiff.core.model.ChangedSchema;
import org.openapitools.openapidiff.core.output.MarkdownRender;

import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;

/**
 * @author p1us2er0
 */
public class LastaMetaMarkdownRender extends MarkdownRender {
	
	public LastaMetaMarkdownRender() {
		super();
		setShowChangedMetadata(true);
	}

	@Override
    protected String listEndpoints(List<ChangedOperation> changedOperations) {
        if (null == changedOperations || changedOperations.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        changedOperations.stream().map(operation -> {
            StringBuilder details = new StringBuilder();
            if (Changed.result(operation.getParameters()).isDifferent()) {
                String a = parameters(operation.getParameters());
                if (!a.isEmpty() && !a.equals("\n")) {
                    details.append(titleH5("Parameters:")).append(a);
                }
            }
            if (operation.resultRequestBody().isDifferent()) {
                details.append(titleH5("Request:"))
                        .append(metadata("Description", operation.getRequestBody().getDescription()))
                        .append(bodyContent(operation.getRequestBody().getContent()));
                RequestBody oldRequestBody = operation.getRequestBody().getOldRequestBody();
                RequestBody newRequestBody = operation.getRequestBody().getNewRequestBody();
                if (oldRequestBody != null && newRequestBody != null && oldRequestBody.getRequired() != newRequestBody.getRequired()) {
                	ChangedMetadata changedMetadata = new ChangedMetadata();
                	changedMetadata.setLeft(Objects.toString(oldRequestBody.getRequired()));
                	changedMetadata.setRight(Objects.toString(newRequestBody.getRequired()));
                	details.append(metadata("Required", changedMetadata));
                }
            }
            if (operation.resultApiResponses().isDifferent()) {
                details.append(titleH5("Return Type:")).append(responses(operation.getApiResponses()));
            }
            if (details.length() != 0) {
                details.insert(0, itemEndpoint(operation.getHttpMethod().toString(), operation.getPathUrl(), operation.getSummary()));
            }
            return details.toString();
        }).forEach(sb::append);
        if (sb.length() != 0) {
            sb.insert(0, sectionTitle("What's Changed"));
        }
        return sb.toString();
    }

    @Override
    protected String parameters(ChangedParameters changedParameters) {
        List<ChangedParameter> changed = changedParameters.getChanged();
        StringBuilder sb = new StringBuilder("\n");
        sb.append(listParameter("Added", changedParameters.getIncreased()))
                .append(listParameter("Deleted", changedParameters.getMissing()));
        changed.stream()
                .filter(param -> param.getChangedElements().stream().anyMatch(x -> x != null && !x.isChanged().isUnchanged()))
                .map(this::itemParameter)
                .forEach(sb::append);
        return sb.toString();
    }

    protected String itemParameter(ChangedParameter param) {
        Parameter oldParam = param.getOldParameter();
        Parameter newParam = param.getNewParameter();
        if (param.isDeprecated()) {
            return itemParameter("Deprecated", newParam.getName(), newParam.getIn(), newParam.getDescription());
        }

        Map<String, String> map = new LinkedHashMap<>();
        if (param.getDescription() != null && !param.getDescription().isChanged().isUnchanged()) {
            map.put("description", oldParam.getDescription() + " -> " + newParam.getDescription());
        }
        if (param.getSchema() != null && !Objects.equals(oldParam.getSchema().getType(), newParam.getSchema().getType())) {
            map.put("type", oldParam.getSchema().getType() + " -> " + newParam.getSchema().getType());
        }
        if (param.getSchema() != null && !Objects.equals(oldParam.getSchema().getFormat(), newParam.getSchema().getFormat())) {
            map.put("format", oldParam.getSchema().getFormat() + " -> " + newParam.getSchema().getFormat());
        }
        return itemParameter("Changed", newParam.getName(), newParam.getIn(),
                map.isEmpty() ? newParam.getDescription() : map.toString());
    }

	@Override
    protected String property(int deepness, String name, ChangedSchema schema) {
        if (schema.isChanged().isUnchanged()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String type = type(schema.getNewSchema());
        if (schema.isChangedType()) {
            type = type(schema.getOldSchema()) + " -> " + type(schema.getNewSchema());
        }
        Map<String, String> map = new LinkedHashMap<>();
        if (schema.getDescription() != null && !schema.getDescription().isChanged().isUnchanged()) {
            map.put("description", schema.getOldSchema().getDescription() + " -> " + schema.getNewSchema().getDescription());
        }
        if (!Objects.equals(schema.getOldSchema().getType(), schema.getNewSchema().getType())) {
            map.put("type", schema.getOldSchema().getType() + " -> " + schema.getNewSchema().getType());
        }
        if (!Objects.equals(schema.getOldSchema().getFormat(), schema.getNewSchema().getFormat())) {
            map.put("format", schema.getOldSchema().getFormat() + " -> " + schema.getNewSchema().getFormat());
        }
        sb.append(property(deepness, "Changed property", name, type,
                map.isEmpty() ? schema.getNewSchema().getDescription() : map.toString()));
        sb.append(schema(++deepness, schema));
        return sb.toString();
    }

	@Override
    protected String items(int deepness, ChangedSchema schema) {
        StringBuilder sb = new StringBuilder();
        String type = type(schema.getNewSchema());
        if (schema.isChangedType()) {
            type = type(schema.getOldSchema()) + " -> " + type(schema.getNewSchema());
        }
        Map<String, String> map = new LinkedHashMap<>();
        if (schema.getDescription() != null && !schema.getDescription().isChanged().isUnchanged()) {
            map.put("description", schema.getOldSchema().getDescription() + " -> " + schema.getNewSchema().getDescription());
        }
        sb.append(items(deepness, "Changed items", type, map.isEmpty() ? schema.getNewSchema().getDescription() : map.toString()));
        sb.append(schema(deepness, schema));
		return sb.toString();
	}
}
