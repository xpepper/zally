package de.zalando.zally.rule.zally

import de.zalando.zally.rule.Context
import de.zalando.zally.rule.api.Check
import de.zalando.zally.rule.api.Rule
import de.zalando.zally.rule.api.Severity
import de.zalando.zally.rule.api.Violation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import org.slf4j.LoggerFactory

@Rule(
    ruleSet = ZallyRuleSet::class,
    id = "S005",
    severity = Severity.SHOULD,
    title = "Do not leave unused definitions"
)
class NoUnusedDefinitionsRule2 {

    @Check(severity = Severity.SHOULD)
    fun validate(context: Context): List<Violation> {
        this.context = context
        return findUnreferencedParameters(context) + findUnreferencedSchemas(context)
    }

    private var context: Context? = null // TODO: KILL MEEEEEEEEE (after debugging)

    private fun findUnreferencedParameters(context: Context): List<Violation> {
        val paramsInPaths = context.unrecordedApi.paths.orEmpty().flatMap { (_, path) ->
            path.readOperations().flatMap { operation ->
                val fromParameters = operation.parameters.orEmpty()
                val fromBody = listOfNotNull(operation.requestBody)
                fromParameters + fromBody
            }
        }
        return context.validateParameters { (_, parameter) ->
            if (parameter in paramsInPaths) {
                emptyList()
            } else {
                context.violations("Unused parameter definition.", parameter)
            }
        }
    }

    private fun findUnreferencedSchemas(context: Context): List<Violation> {
        val allRefs: Set<Schema<*>> = findAllSchemasInApi(context.unrecordedApi).toSet()
        return context.validateSchemas { (name, schema) ->
            logger.debug("Finding if schema `$name` is referenced.")
            if (schema in allRefs) {
                emptyList()
            } else {
                logger.debug("Schema `$name` is not referenced. Generating violation.")
                context.violations("Unused schema definition.", schema)
            }
        }
    }

    private fun findAllSchemasInApi(api: OpenAPI): List<Schema<*>> {
        val refsInPaths = api.paths.orEmpty()
            .flatMap { (name, path) -> findAllSchemasInPath(name, path) }
        val refsInSchemaDefinitions = api.components?.schemas.orEmpty()
            .flatMap { (schemaName, schema) -> findAllSchemasInSchema(schemaName, schema, includeSelf = false) }
        return refsInPaths + refsInSchemaDefinitions
    }

    private fun findAllSchemasInPath(name: String, path: PathItem): List<Schema<*>> {
        logger.debug("Find all schemas in path `$name`.")
        return path.readOperations().flatMap(this::findAllSchemasInOperation)
    }

    private fun findAllSchemasInOperation(operation: Operation): List<Schema<*>> {
        logger.debug("Find all schemas in operation `${operation.operationId}`.")
        val inParameters = operation.requestBody?.content.orEmpty()
            .flatMap { (mediaTypeName, mediaType) ->
                findAllSchemasInSchema("(schema for mediaType `$mediaTypeName`)", mediaType.schema)
            }
        val inResponse = operation.responses.orEmpty()
            .flatMap { (status, response) ->
                findAllSchemasInResponse(status, response)
            }
        return inParameters + inResponse
    }

    private fun findAllSchemasInSchema(name: String, schema: Schema<*>): List<Schema<*>> = findAllSchemasInSchema(name, schema, true)

    private fun findAllSchemasInSchema(name: String, schema: Schema<*>, includeSelf: Boolean): List<Schema<*>> {
        logger.debug("Find all schemas in schema `$name` (including itself: $includeSelf)")
        val specific = when (schema) {
            is ArraySchema -> {
                val inItemsSchema = findAllSchemasInSchema("(items in array `$name`)", schema.items)
                inItemsSchema
            }
            is ComposedSchema -> {
                val inComposedSchema = findAllSchemasInComposedSchema(name, schema)
                inComposedSchema
            }
            else -> {
                val properties = schema.properties.orEmpty()
                    .flatMap { (propertyName, property) -> findAllSchemasInSchema(propertyName, property) }
                val additionalProperties = findAllRefsInAdditionalProperties(name, schema.properties)
                properties + additionalProperties
            }
        }
        val self = if (includeSelf) listOf(schema) else emptyList()
        return self + specific
    }

    private fun findAllSchemasInComposedSchema(name: String, schema: ComposedSchema): List<Schema<*>> {
        logger.debug("Find all schemas in composed schema `$name`.")
        val allOf = schema.allOf.orEmpty().flatMap { findAllSchemasInSchema("(allOf in `$name`)", it) }
        val additionalProperties = findAllRefsInAdditionalProperties(name, schema.additionalProperties)
        return allOf + additionalProperties
    }

    private fun findAllRefsInAdditionalProperties(name: String, additionalProperties: Any?): List<Schema<*>> {
        logger.debug("Find all refs in additional property of `$name`.")
        return when (additionalProperties) {
            is Schema<*> ->
                findAllSchemasInSchema(name, additionalProperties)
            is Map<*, *> ->
                additionalProperties
                    .mapNotNull { (additionalPropertyName, additionalProperty) ->
                        when (additionalProperty) {
                            is Schema<*> -> additionalPropertyName to additionalProperty
                            else -> null
                        }
                    }
                    .flatMap { (additionalPropertyName, additionalProperty) ->
                        findAllSchemasInSchema(additionalPropertyName.toString(), additionalProperty)
                    }
            else ->
                emptyList()
        }
    }

    private fun findAllSchemasInResponse(status: String, response: ApiResponse): List<Schema<*>> {
        logger.debug("Find all schemas in response `$status`.")
        return response.content.orEmpty()
            .flatMap { (mediaTypeName, mediaType) ->
                findAllSchemasInSchema("(schema for mediaType `$mediaTypeName`)", mediaType.schema)
            }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
