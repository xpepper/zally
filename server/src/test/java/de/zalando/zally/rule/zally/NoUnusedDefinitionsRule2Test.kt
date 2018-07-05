package de.zalando.zally.rule.zally

import com.fasterxml.jackson.core.JsonPointer
import de.zalando.zally.getContextFromFixture
import de.zalando.zally.getFixture
import de.zalando.zally.rule.api.Violation
import io.swagger.models.Swagger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NoUnusedDefinitionsRule2Test {

    private val rule = NoUnusedDefinitionsRule2()

    @Test
    fun positiveCase() {
        val context = getContextFromFixture("unusedDefinitionsValid.json")!!
        assertThat(rule.validate(context))
            .isEmpty()
    }

    @Test
    fun negativeCase() {
        val results = rule.validate(getContextFromFixture("unusedDefinitionsInvalid.json")!!)
        assertThat(results).hasSameElementsAs(listOf(
            Violation("", JsonPointer.compile("/definitions/PetName")),
            Violation("", JsonPointer.compile("/parameters/FlowId"))
        ))
    }

//    @Test
//    fun emptySwaggerShouldPass() {
//        val swagger = Swagger()
//        assertThat(rule.validate(swagger)).isNull()
//    }
//
//    @Test
//    fun positiveCaseSpp() {
//        val swagger = getFixture("api_spp.json")
//        assertThat(rule.validate(swagger)).isNull()
//    }
//
//    @Test
//    fun positiveCaseTinbox() {
//        val swagger = getFixture("api_tinbox.yaml")
//        assertThat(rule.validate(swagger)).isNull()
//    }
}
