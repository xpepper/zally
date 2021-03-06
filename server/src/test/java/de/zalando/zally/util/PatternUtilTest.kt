package de.zalando.zally.util

import de.zalando.zally.util.PatternUtil.hasTrailingSlash
import de.zalando.zally.util.PatternUtil.isCamelCase
import de.zalando.zally.util.PatternUtil.isHyphenated
import de.zalando.zally.util.PatternUtil.isHyphenatedCamelCase
import de.zalando.zally.util.PatternUtil.isHyphenatedPascalCase
import de.zalando.zally.util.PatternUtil.isKebabCase
import de.zalando.zally.util.PatternUtil.isPascalCase
import de.zalando.zally.util.PatternUtil.isPathVariable
import de.zalando.zally.util.PatternUtil.isSnakeCase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for patterns utility
 */
class PatternUtilTest {

    @Test
    fun checkHasTrailingSlash() {
        assertTrue(hasTrailingSlash("blah/"))
        assertFalse(hasTrailingSlash("blah"))
    }

    @Test
    fun checkIsPathVariable() {
        assertTrue(isPathVariable("{test}"))
        assertFalse(isPathVariable("{}"))
        assertFalse(isPathVariable(" { } "))
        assertFalse(isPathVariable("abc"))
        assertFalse(isPathVariable("{test"))
        assertFalse(isPathVariable("test}"))
    }

    @Test
    fun checkIsCamelCase() {
        assertTrue(isCamelCase("testCase"))
        assertFalse(isCamelCase("TestCase"))
    }

    @Test
    fun checkIsPascalCase() {
        assertTrue(isPascalCase("TestCase"))
        assertFalse(isPascalCase("testCase"))
    }

    @Test
    fun checkIsHyphenatedCamelCase() {
        assertTrue(isHyphenatedCamelCase("test-Case"))
        assertFalse(isHyphenatedCamelCase("Test-Case"))
        assertFalse(isHyphenatedCamelCase("testCase"))
        assertFalse(isHyphenatedCamelCase("TestCase"))
    }

    @Test
    fun checkIsHyphenatedPascalCase() {
        assertTrue(isHyphenatedPascalCase("Test-Case"))
        assertTrue(isHyphenatedPascalCase("X-Flow-Id"))
        assertTrue(isHyphenatedPascalCase("ETag"))
        assertFalse(isHyphenatedPascalCase("test-Case"))
        assertFalse(isHyphenatedPascalCase("TestCase"))
        assertFalse(isHyphenatedPascalCase("testCase"))
    }

    @Test
    fun checkIsSnakeCase() {
        assertTrue(isSnakeCase("test_case"))
        assertTrue(isSnakeCase("test"))
        assertTrue(isSnakeCase("v1_id"))
        assertTrue(isSnakeCase("0_1_2_3"))
        assertFalse(isSnakeCase("test__case"))
        assertFalse(isSnakeCase("TestCase"))
        assertFalse(isSnakeCase("Test_Case"))
        assertFalse(isSnakeCase(""))
        assertFalse(isSnakeCase("_"))
        assertFalse(isSnakeCase("customer-number"))
        assertFalse(isSnakeCase("_customer_number"))
        assertFalse(isSnakeCase("CUSTOMER_NUMBER"))
    }

    @Test
    fun checkIsKebabCase() {
        assertTrue(isKebabCase("test-case"))
        assertFalse(isKebabCase("test-Case"))
        assertFalse(isKebabCase("testCase"))
    }

    @Test
    fun checkIsHyphenated() {
        // uncontroversial positive cases
        assertTrue(isHyphenated("A"))
        assertTrue(isHyphenated("low"))
        assertTrue(isHyphenated("Aa"))
        assertTrue(isHyphenated("A-A"))
        assertTrue(isHyphenated("X-Auth-2.0"))
        assertTrue(isHyphenated("This-Is-Some-Hyphenated-String"))
        assertTrue(isHyphenated("this-is-other-hyphenated-string"))

        // uncontroversial negative cases
        assertFalse(isHyphenated("Sorry no hyphens here"))
        assertFalse(isHyphenated("a--a"))

        // issue 572
        assertTrue(isHyphenated("X-RateLimit-Reset"))

        // the following are all perfectly valid single-term 'hypenated' headers
        assertTrue(isHyphenated("aA"))
        assertTrue(isHyphenated("AA"))
        assertTrue(isHyphenated("CamelCaseIsNotAcceptableAndShouldBeIllegal"))
    }
}
