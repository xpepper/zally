package de.zalando.zally.rule.zally

import com.fasterxml.jackson.core.JsonPointer
import de.zalando.zally.getContextFromFixture
import de.zalando.zally.rule.Context
import de.zalando.zally.rule.api.Violation
import io.swagger.v3.oas.models.OpenAPI
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NoUnusedDefinitionsRule2Test {

    @Test
    fun `positive case causes no violation`() {
        val context = getContextFromFixture("unusedDefinitionsValid.json")
        assertThat(rule.validate(context))
            .isEmpty()
    }

    @Test
    fun `negative case causes violations`() {
        val results = rule.validate(getContextFromFixture("unusedDefinitionsInvalid.json"))
        assertThat(results).hasSameElementsAs(listOf(
            vSchema("/definitions/PetName"),
            vParam("/parameters/FlowId")
        ))
    }

    @Test
    fun `empty specification causes no violation`() {
        val context = Context(OpenAPI())
        assertThat(rule.validate(context))
            .isEmpty()
    }

    @Test
    fun `the SPP API causes no violations`() {
        val context = getContextFromFixture("api_spp.json")
        assertThat(rule.validate(context))
            .isEmpty()
    }

    @Test
    fun `the tinbox API causes no violation`() {
        val context = getContextFromFixture("api_tinbox.yaml")
        assertThat(rule.validate(context))
            .isEmpty()
    }

    private val rule = NoUnusedDefinitionsRule2()

    private fun vParam(pointer: String): Violation = Violation(
        "Unused parameter definition.",
        JsonPointer.compile(pointer)
    )

    private fun vSchema(pointer: String): Violation = Violation(
        "Unused schema definition.",
        JsonPointer.compile(pointer)
    )

}
