package com.airline.booking.demo.feature.passengers.mapper

import com.airline.booking.demo.feature.passengers.dto.PassengerRequest
import com.airline.booking.demo.feature.passengers.dto.PassengerResponse
import com.airline.booking.demo.feature.passengers.repository.entity.Passenger
import spock.lang.Specification

import java.time.LocalDate
import java.time.OffsetDateTime

class PassengerMapperSpec extends Specification {

    def "toEntity should map fields correctly when request is valid"() {
        given:
        def req = new PassengerRequest("John", "Doe", "john@doe.com", "+123", "ABC123", "12-12-2002")

        when:
        def entity = PassengerMapper.toEntity(req)

        then:
        entity != null
        entity.firstName == "John"
        entity.lastName == "Doe"
        entity.email == "john@doe.com"
        entity.phone == "+123"
        entity.passportNumber == "ABC123"
        entity.dateOfBirth == LocalDate.parse("2002-12-12")
    }

    def "toEntity should return null when request is null"() {
        expect:
        PassengerMapper.toEntity(null) == null
    }

    def "toResponse should map fields correctly when entity is valid"() {
        given:
        def now = OffsetDateTime.now()
        def dob = LocalDate.of(1990, 5, 20)

        def entity = new Passenger()
        entity.id = 10L
        entity.firstName = "Alice"
        entity.lastName = "Smith"
        entity.email = "alice@smith.com"
        entity.phone = "+321"
        entity.passportNumber = "XYZ987"
        entity.dateOfBirth = dob
        entity.createdAt = now.minusDays(1)
        entity.updatedAt = now

        when:
        PassengerResponse resp = PassengerMapper.toResponse(entity)

        then:
        resp != null
        resp.id == 10L
        resp.firstName == "Alice"
        resp.lastName == "Smith"
        resp.email == "alice@smith.com"
        resp.phone == "+321"
        resp.passportNumber == "XYZ987"
        resp.dateOfBirth == dob.toString()
        resp.createdAt == entity.createdAt
        resp.updatedAt == entity.updatedAt
    }

    def "toResponse should return null when entity is null"() {
        expect:
        PassengerMapper.toResponse(null) == null
    }
}
