package de.zalando.zally.util

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse

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

//fun OpenAPI.getAllProperties(
//    filter: (NamedProperty) -> Boolean = { true }
//): List<NamedProperty> =
//    components.schemas.orEmpty()
//        .flatMap { (_, schema) ->
//            schema.properties.filter(filter).entries
//        }
//

/**
 * the mere fact that the `enum` property has value means that the parser read an `enum` property
 * in the schema definition and it then cannot be a `x-extensible-enum` or another extensible form.
 */
fun Schema<*>.isEnum(): Boolean =
    enum?.isNotEmpty() ?: false

//fun Parameter.toNamedParameter() = SimpleEntry(name, this)
//
//fun OpenAPI.getAllParameters(
//    filter: (NamedParameter) -> Boolean = { true }
//): List<NamedParameter> {
//    val fromPaths = paths.orEmpty()
//        .flatMap { (_, path: PathItem) ->
//            val pathParameters = path.parameters.orEmpty()
//                .map { SimpleEntry(it.name, it) }
//                .filter(filter)
//            val verbsParameters = path.readOperations()
//                .flatMap { operation ->
//                    operation.parameters.orEmpty()
//                        .map(Parameter::toNamedParameter)
//                        .filter(filter)
//                }
//            pathParameters + verbsParameters
//        }
//    val fromComponents = components.parameters.orEmpty()
//        .entries
//        .filter(filter)
//    return fromPaths + fromComponents
//}

data class SchemaInfo(
    val schema: Schema<*>,
    val parent: Any?,
    val name: String?
)

fun MediaType.getAllSchemas(): List<SchemaInfo> =
    listOf(SchemaInfo(this.schema, this, null))

fun Parameter.getAllSchemas(parent: Any?, name: String?): List<SchemaInfo> {
    val fromSchema = listOf(SchemaInfo(schema, parent, name))
    val fromContent = content.orEmpty().values
        .flatMap { mediaType -> mediaType.getAllSchemas() }
    return fromSchema + fromContent
}

fun ApiResponse.getAllSchemas(): List<SchemaInfo> =
    content.orEmpty().flatMap { (_, mediaType) -> mediaType.getAllSchemas() }

fun RequestBody.getAllSchemas(): List<SchemaInfo> =
    content.orEmpty().flatMap { (_, mediaType) -> mediaType.getAllSchemas() }

fun Operation.getAllSchemas(): List<SchemaInfo> {
    val fromParams = parameters.orEmpty().flatMap { it.getAllSchemas(parameters, null) }
    val fromResponses = responses.orEmpty().values.flatMap(ApiResponse::getAllSchemas)
    val fromReqBody = requestBody?.getAllSchemas() ?: emptyList()
    return fromParams + fromResponses + fromReqBody
}

fun PathItem.getAllSchemas(): List<SchemaInfo> {
    val fromParams = parameters.orEmpty().flatMap { it.getAllSchemas(parameters, null) }
    val fromOperations = readOperations().flatMap(Operation::getAllSchemas)
    return fromParams + fromOperations
}

fun ArraySchema.getAllSchemas(): List<SchemaInfo> = items.getAllSchemas(items, null, true)

fun ComposedSchema.getAllSchemas(): List<SchemaInfo> {
    val fromAllOf = allOf.orEmpty().flatMap { it.getAllSchemas(allOf, null, true) }
    val fromAddProps = getAllSchemasFromAny(additionalProperties, this, null)
    return fromAllOf + fromAddProps
}

fun Schema<*>.getAllSchemas(parent: Any?, name: String?, includeSelf: Boolean): List<SchemaInfo> {
    val fromSelf = if (includeSelf) listOf(SchemaInfo(this, parent, name)) else emptyList()
    val specifics = when (this) {
        is ArraySchema -> getAllSchemas()
        is ComposedSchema -> getAllSchemas()
        else -> {
            val fromAddProps = getAllSchemasFromAny(additionalProperties, this, null)
            val fromProperties = properties.orEmpty().flatMap { (propName, prop) -> prop.getAllSchemas(properties, propName, true) }
            fromAddProps + fromProperties
        }
    }
    return fromSelf + specifics
}

/**
 * Typically used for `additionalProperties`, since they are types `Object` (Any?).
 */
fun getAllSchemasFromAny(o: Any?, parent: Any?, name: String?): List<SchemaInfo> = when (o) {
    null -> emptyList()
    is Schema<*> -> o.getAllSchemas(parent, name, true)
    is Map<*, *> -> o.flatMap { (propName, prop) -> getAllSchemasFromAny(prop, o, propName.toString()) }
    else -> emptyList()
}

fun Components.getAllSchemas(): List<SchemaInfo> {
    val fromCompSchemas = schemas.orEmpty().flatMap { (name, schema) -> schema.getAllSchemas(schemas, name, true) }
    val fromCompParams = parameters.orEmpty()
        .flatMap { (name, param) -> param.getAllSchemas(parameters, name) }
    val fromCompResponses = responses.orEmpty().values.flatMap(ApiResponse::getAllSchemas)
    val fromCompRespBodies = requestBodies.orEmpty().values.flatMap(RequestBody::getAllSchemas)
    return fromCompSchemas + fromCompParams + fromCompResponses + fromCompRespBodies
}

fun OpenAPI.getAllSchemas(): List<SchemaInfo> {
    val fromComponents = components?.getAllSchemas() ?: emptyList()
    val fromPaths = paths.orEmpty().values.flatMap(PathItem::getAllSchemas)
    return fromComponents + fromPaths
}
