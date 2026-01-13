package com.airline.booking.demo.feature.flights.mapper

import com.airline.booking.demo.feature.flights.dto.FlightRequest
import com.airline.booking.demo.feature.flights.dto.FlightResponse
import com.airline.booking.demo.feature.flights.repository.entity.Flight
import com.airline.booking.demo.feature.flights.repository.entity.FlightStatus
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset

class FlightMapperSpec extends Specification {

    def "toEntity should map FlightRequest to Flight entity correctly"() {
        given:
        def req = new FlightRequest(
                "SA100",
                5L,
                "JNB",
                "CPT",
                "2026-01-12T10:00:00Z",
                "2026-01-12T12:00:00Z",
                180,
                150,
                new BigDecimal("4500.00")
        )

        when:
        def entity = FlightMapper.toEntity(req)

        then:
        entity.flightNumber == "SA100"
        entity.airlineId == 5L
        entity.departureAirport == "JNB"
        entity.arrivalAirport == "CPT"
        entity.departureTime == OffsetDateTime.parse("2026-01-12T10:00:00Z")
        entity.arrivalTime == OffsetDateTime.parse("2026-01-12T12:00:00Z")
        entity.totalSeats == 180
        entity.availableSeats == 150
        entity.price == new BigDecimal("4500.00")
        entity.status == FlightStatus.SCHEDULED.name()
    }

    def "toEntity should default availableSeats to totalSeats when null"() {
        given:
        def req = new FlightRequest(
                "SA200",
                5L,
                "JNB",
                "DUR",
                "2026-01-13T08:00:00Z",
                "2026-01-13T10:00:00Z",
                100,
                null,
                new BigDecimal("2200.00")
        )

        when:
        def entity = FlightMapper.toEntity(req)

        then:
        entity.totalSeats == 100
        entity.availableSeats == 100
    }

    def "toResponse should map Flight entity to FlightResponse correctly"() {
        given:
        def now = OffsetDateTime.now(ZoneOffset.UTC)
        def entity = new Flight()
        entity.id = 99L
        entity.flightNumber = "SA300"
        entity.airlineId = 10L
        entity.departureAirport = "JNB"
        entity.arrivalAirport = "PLZ"
        entity.departureTime = now.minusHours(2)
        entity.arrivalTime = now.minusHours(1)
        entity.totalSeats = 200
        entity.availableSeats = 180
        entity.price = new BigDecimal("5500.00")
        entity.status = FlightStatus.SCHEDULED.name()
        entity.createdAt = now.minusDays(1)
        entity.updatedAt = now

        when:
        def resp = FlightMapper.toResponse(entity)

        then:
        resp instanceof FlightResponse
        resp.id == 99L
        resp.flightNumber == "SA300"
        resp.airlineId == 10L
        resp.departureAirport == "JNB"
        resp.arrivalAirport == "PLZ"
        resp.departureTime == entity.departureTime.toString()
        resp.arrivalTime == entity.arrivalTime.toString()
        resp.totalSeats == 200
        resp.availableSeats == 180
        resp.price == new BigDecimal("5500.00")
        resp.status == FlightStatus.SCHEDULED.name()
        resp.createdAt == entity.createdAt.toString()
        resp.updatedAt == entity.updatedAt.toString()
    }

    def "toResponse should throw NPE when entity is null"() {
        when:
        FlightMapper.toResponse(null)

        then:
        thrown(NullPointerException)
    }
}
