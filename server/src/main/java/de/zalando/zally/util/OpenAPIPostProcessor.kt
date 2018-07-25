package de.zalando.zally.util

import io.swagger.v3.oas.models.OpenAPI

class OpenAPIPostProcessor(val api: OpenAPI) {

    fun setComponentsName() {
        api.components.parameters.orEmpty()
            .filterValues { param -> param.name.isNullOrBlank() }
            .forEach { (name, param) -> param.name = name }
        api.components.schemas.orEmpty()
            .filterValues { schema -> schema.name.isNullOrBlank() }
            .forEach { (name, schema) -> schema.name = name }
    }

    fun postResolveRefs() {
        TODO()
    }
}
