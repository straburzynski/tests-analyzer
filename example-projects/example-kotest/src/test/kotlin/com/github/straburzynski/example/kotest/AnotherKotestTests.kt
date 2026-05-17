package com.github.straburzynski.example.kotest

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

class AnotherKotestTests : StringSpec({

    "should send welcome email to new user" {
        delay(150)
        true shouldBe true
    }

    "should calculate shipping cost" {
        delay(1700)
        true shouldBe true
    }

    "should render dashboard widgets" {
        delay(1100)
        true shouldBe true
    }

    "should enforce rate limiting" {
        delay(1400)
        "blocked" shouldBe "allowed"
    }

    "!should deliver push notification" {
        delay(500)
        true shouldBe true
    }
})
