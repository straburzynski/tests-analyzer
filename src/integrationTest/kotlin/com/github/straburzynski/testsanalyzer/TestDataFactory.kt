package com.github.straburzynski.testsanalyzer

import com.github.straburzynski.testsanalyzer.model.TestResult
import com.github.straburzynski.testsanalyzer.model.TestResultType
import com.github.straburzynski.testsanalyzer.model.TestStatus

object TestDataFactory {

    /**
     * Creates a standard set of 10 test results:
     * - 6 SUCCESS (across 3 classes)
     * - 2 FAILED
     * - 2 SKIPPED
     */
    fun createStandardResults(): List<TestResult> = listOf(
        TestResult(
            "UserServiceTest",
            "should create user",
            "com.example.service",
            300,
            TestStatus.SUCCESS,
            0,
            300
        ),
        TestResult(
            "UserServiceTest",
            "should delete user",
            "com.example.service",
            150,
            TestStatus.SUCCESS,
            0,
            150
        ),
        TestResult(
            "UserServiceTest",
            "should fail on duplicate email",
            "com.example.service",
            200,
            TestStatus.FAILED,
            100,
            300,
        ),

        TestResult(
            "OrderControllerTest",
            "should place order",
            "com.example.controller",
            500,
            TestStatus.SUCCESS,
            0,
            500,
        ),
        TestResult(
            "OrderControllerTest",
            "should cancel order",
            "com.example.controller",
            250,
            TestStatus.SUCCESS,
            200,
            450,
        ),
        TestResult(
            "OrderControllerTest",
            "should return 404 for missing order",
            "com.example.controller",
            100,
            TestStatus.FAILED,
            300,
            400,
        ),
        TestResult(
            "OrderControllerTest",
            "should skip premium feature",
            "com.example.controller",
            0,
            TestStatus.SKIPPED,
            0,
            0,
        ),

        TestResult(
            "PaymentGatewayTest",
            "should process payment",
            "com.example.payment",
            800,
            TestStatus.SUCCESS,
            0,
            800,
        ),
        TestResult(
            "PaymentGatewayTest",
            "should refund payment",
            "com.example.payment",
            400,
            TestStatus.SUCCESS,
            100,
            500,
        ),
        TestResult(
            "PaymentGatewayTest",
            "should skip sandbox test",
            "com.example.payment",
            0,
            TestStatus.SKIPPED,
            0,
            0,
        ),

        TestResult(
            "TestSuite",
            "suite",
            "com.example",
            900,
            TestStatus.SUCCESS,
            0,
            900,
            TestResultType.SUITE
        ),
    )

    const val EXPECTED_TOTAL = 10
    const val EXPECTED_PASSED = 6
    const val EXPECTED_FAILED = 2
    const val EXPECTED_SKIPPED = 2

    val EXPECTED_TESTS_PER_GROUP = mapOf(
        "UserServiceTest" to 3,
        "OrderControllerTest" to 4,
        "PaymentGatewayTest" to 3,
    )

    fun createEmptyResults(): List<TestResult> = emptyList()

    fun createResultsWithSpecialChars(): List<TestResult> = listOf(
        TestResult(
            "Test<Html>",
            "should handle <script>alert('xss')</script>",
            "com.example",
            100,
            TestStatus.SUCCESS,
            0,
            100,
        ),
        TestResult(
            "Test&Ampersand",
            "test with \"quotes\" & ampersands",
            "com.example",
            200,
            TestStatus.FAILED,
            0,
            200,
        ),
    )
}
