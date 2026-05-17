package com.github.straburzynski.example.groovy

import spock.lang.Ignore
import spock.lang.Specification

class AnotherSpockSpec extends Specification {

    def "should send welcome email to new user"() {
        when:
        Thread.sleep(2200)

        then:
        true
    }

    def "should calculate shipping cost"() {
        when:
        Thread.sleep(2600)

        then:
        true
    }

    def "should render dashboard widgets"() {
        when:
        Thread.sleep(1000)

        then:
        true
    }

    def "should enforce rate limiting"() {
        when:
        Thread.sleep(450)

        then:
        "blocked" == "allowed"
    }

    @Ignore("Notification service not configured in test environment")
    def "should deliver push notification"() {
        when:
        Thread.sleep(500)

        then:
        true
    }
}
