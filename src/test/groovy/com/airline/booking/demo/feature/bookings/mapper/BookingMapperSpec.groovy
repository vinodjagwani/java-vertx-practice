package com.airline.booking.demo.feature.bookings.mapper

import com.airline.booking.demo.feature.bookings.dto.BookingRequest
import com.airline.booking.demo.feature.bookings.dto.BookingResponse
import com.airline.booking.demo.feature.bookings.repository.entity.Booking
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset

class BookingMapperSpec extends Specification {

    def "toEntity should map BookingRequest to Booking entity correctly"() {
        given:
        def req = new BookingRequest(10L, 20L, "A1")

        when:
        def entity = BookingMapper.toEntity(req)

        then:
        entity.passengerId == 10L
        entity.flightId == 20L
        entity.seatNumber == "A1"
        entity.id == null
        entity.bookingReference == null
        entity.status == null
        entity.totalAmount == null
        entity.bookingDate == null
        entity.createdAt == null
        entity.updatedAt == null
    }

    def "toResponse should map Booking entity to BookingResponse correctly"() {
        given:
        def now = OffsetDateTime.now(ZoneOffset.UTC)

        def entity = new Booking()
        entity.id = 99L
        entity.bookingReference = "REF123"
        entity.passengerId = 10L
        entity.flightId = 20L
        entity.seatNumber = "A1"
        entity.status = "CONFIRMED"
        entity.totalAmount = new BigDecimal("550.00")
        entity.bookingDate = now.minusDays(2)
        entity.createdAt = now.minusDays(1)
        entity.updatedAt = now

        when:
        def resp = BookingMapper.toResponse(entity)

        then:
        resp instanceof BookingResponse
        resp.id == entity.id
        resp.bookingReference == entity.bookingReference
        resp.passengerId == entity.passengerId
        resp.flightId == entity.flightId
        resp.seatNumber == entity.seatNumber
        resp.status == entity.status
        resp.totalAmount == entity.totalAmount
        resp.bookingDate == entity.bookingDate
        resp.createdAt == entity.createdAt
        resp.updatedAt == entity.updatedAt
    }

    def "toResponse should throw NPE on null entity"() {
        when:
        BookingMapper.toResponse(null)

        then:
        thrown(NullPointerException)
    }
}
