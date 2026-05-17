package com.github.straburzynski.example.groovy

import spock.lang.Ignore
import spock.lang.Specification

class SampleSpockSpec extends Specification {

    def "should process payment in under 1s"() {
        when:
        Thread.sleep(150)

        then:
        true
    }

    def "should validate user input"() {
        when:
        Thread.sleep(1350)

        then:
        true
    }

    def "should fetch remote configuration"() {
        when:
        Thread.sleep(550)

        then:
        true
    }

    def "should synchronize database records"() {
        when:
        Thread.sleep(750)

        then:
        true
    }

    def "should generate analytics report"() {
        when:
        Thread.sleep(950)

        then:
        true
    }

    def "should complete full data migration"() {
        when:
        Thread.sleep(1300)

        then:
        true
    }

    def "should handle concurrent write conflicts"() {
        when:
        Thread.sleep(400)

        then:
        "actual" == "expected"
    }

    def "should retry failed API call"() {
        when:
        Thread.sleep(700)

        then:
        false
    }

    @Ignore("Payment gateway sandbox is unavailable in CI")
    def "should charge credit card via gateway"() {
        when:
        Thread.sleep(500)

        then:
        true
    }

    @Ignore("Requires external S3 bucket access")
    def "should upload file to cloud storage"() {
        when:
        Thread.sleep(600)

        then:
        true
    }
}
