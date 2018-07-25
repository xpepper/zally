package de.zalando.zally.util

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import java.util.AbstractMap.SimpleEntry

typealias NamedProperty = Map.Entry<String, Schema<*>>
typealias NamedParameter = Map.Entry<String, Parameter>

data class HeaderElement(
    val name: String,
    val element: Any
)

fun OpenAPI.getAllHeaders(): List<HeaderElement> {

    fun Collection<Parameter>?.extractHeaders() = orEmpty()
        .filter { it.`in` == "header" }
        .map { HeaderElement(it.name, it) }

    fun Collection<ApiResponse>?.extractHeaders() = orEmpty()
        .flatMap { it.headers.orEmpty().entries }
        .map { HeaderElement(it.key, it.value) }

    val fromParams = components.parameters.orEmpty().values.extractHeaders()

    val fromPaths = paths.orEmpty().flatMap { (_, path) ->
        val fromPathParameters = path.parameters.extractHeaders()
        val fromOperations = path.readOperations().flatMap { operation ->
            val fromOpParams = operation.parameters.extractHeaders()
            val fromOpResponses = operation.responses.orEmpty().values.extractHeaders()
            fromOpParams + fromOpResponses
        }
        fromPathParameters + fromOperations
    }

    return fromParams + fromPaths
}

fun OpenAPI.getAllProperties(
    filter: (NamedProperty) -> Boolean = { true }
): List<NamedProperty> =
    components.schemas.orEmpty()
        .flatMap { (_, schema) ->
            schema.properties.filter(filter).entries
        }

fun Schema<*>.isEnum(): Boolean =
// the mere fact that the `enum` property has value means that the parser read an `enum` property
// in the schema definition and it then cannot be a `x-extensible-enum` or another extensible form.
    enum?.isNotEmpty() ?: false

fun Parameter.toNamedParameter() = SimpleEntry(name, this)

fun OpenAPI.getAllParameters(
    filter: (NamedParameter) -> Boolean = { true }
): List<NamedParameter> {
    val fromPaths = paths.orEmpty()
        .flatMap { (_, path: PathItem) ->
            val pathParameters = path.parameters.orEmpty()
                .map { SimpleEntry(it.name, it) }
                .filter(filter)
            val verbsParameters = path.readOperations()
                .flatMap { operation ->
                    operation.parameters.orEmpty()
                        .map(Parameter::toNamedParameter)
                        .filter(filter)
                }
            pathParameters + verbsParameters
        }
    val fromComponents = components.parameters.orEmpty()
        .entries
        .filter(filter)
    return fromPaths + fromComponents
}

fun Parameter.getAllSchemas(): List<Schema<*>> {
    val fromSchema = listOf(schema)
    val fromContent = content.orEmpty().values.map { it.schema }
    return fromSchema + fromContent
}

fun ApiResponse.getAllSchemas(): List<Schema<*>> {
    return content.orEmpty().values.map { it.schema }
}

fun RequestBody.getAllSchemas(): List<Schema<*>> {
    return content.orEmpty().values.map { it.schema }
}

fun Operation.getAllSchemas(): List<Schema<*>> {
    val fromParams = parameters.orEmpty().flatMap(Parameter::getAllSchemas)
    val fromResponses = responses.orEmpty().values.flatMap(ApiResponse::getAllSchemas)
    val fromReqBody = requestBody.getAllSchemas()
    return fromParams + fromResponses + fromReqBody
}

fun PathItem.getAllSchemas(): List<Schema<*>> {
    val fromParams = parameters.orEmpty().flatMap(Parameter::getAllSchemas)
    val fromOperations = readOperations().flatMap(Operation::getAllSchemas)
    return fromParams + fromOperations
}

fun ArraySchema.getAllSchemas(): List<Schema<*>> = items.getAllSchemas()

fun ComposedSchema.getAllSchemas(): List<Schema<*>> {
    val fromAllOf = allOf.orEmpty().flatMap(Schema<*>::getAllSchemas)
    val fromAddProps = getAllSchemasFromAny(additionalProperties)
    return fromAllOf + fromAddProps
}

fun Schema<*>.getAllSchemas() = getAllSchemas(includeSelf = true)

fun Schema<*>.getAllSchemas(includeSelf: Boolean): List<Schema<*>> {
    val fromSelf = if (includeSelf) listOf(this) else emptyList()
    val specifics = when (this) {
        is ArraySchema -> getAllSchemas()
        is ComposedSchema -> getAllSchemas()
        else -> {
            val fromAddProps = getAllSchemasFromAny(this.additionalProperties)
            val fromProperties = properties.orEmpty().values.flatMap(Schema<*>::getAllSchemas)
            fromAddProps + fromProperties
        }
    }
    return fromSelf + specifics
}

/**
 * Typically used for `additionalProperties`, since they are types `Object` (Any?).
 */
fun getAllSchemasFromAny(o: Any?): List<Schema<*>> = when (o) {
    null -> emptyList()
    is Schema<*> -> o.getAllSchemas()
    is Map<*, *> -> o.values.flatMap(::getAllSchemasFromAny)
    else -> emptyList()
}

fun Components.getAllSchemas(): List<Schema<*>> {
    val fromCompSchemas = schemas.orEmpty().values.flatMap(Schema<*>::getAllSchemas)
    val fromCompParams = parameters.orEmpty().values.flatMap(Parameter::getAllSchemas)
    val fromCompResponses = responses.orEmpty().values.flatMap(ApiResponse::getAllSchemas)
    val fromCompRespBodies = requestBodies.orEmpty().values.flatMap(RequestBody::getAllSchemas)
    return fromCompSchemas + fromCompParams + fromCompResponses + fromCompRespBodies
}

fun OpenAPI.getAllSchemas(): List<Schema<*>> {
    val fromComponents = components?.getAllSchemas() ?: emptyList()
    val fromPaths = paths.orEmpty().values.flatMap(PathItem::getAllSchemas)
    return fromComponents + fromPaths
}
