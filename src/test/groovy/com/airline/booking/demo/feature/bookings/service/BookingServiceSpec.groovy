package com.airline.booking.demo.feature.bookings.service

import com.airline.booking.demo.common.db.ReactiveTx
import com.airline.booking.demo.exception.BusinessServiceException
import com.airline.booking.demo.exception.dto.ErrorCodeEnum
import com.airline.booking.demo.feature.bookings.dto.BookingRequest
import com.airline.booking.demo.feature.bookings.repository.BookingRepository
import com.airline.booking.demo.feature.bookings.repository.entity.Booking
import com.airline.booking.demo.feature.bookings.repository.entity.BookingStatus
import com.airline.booking.demo.feature.flights.repository.FlightRepository
import com.airline.booking.demo.feature.flights.repository.entity.Flight
import com.airline.booking.demo.feature.passengers.repository.PassengerRepository
import com.airline.booking.demo.feature.passengers.repository.entity.Passenger
import io.vertx.core.Future
import spock.lang.Specification

import java.util.function.Function

class BookingServiceSpec extends Specification {

    ReactiveTx tx = Mock()
    FlightRepository flightRepo = Mock()
    BookingRepository bookingRepo = Mock()
    PassengerRepository passengerRepo = Mock()

    BookingService service = new BookingService(tx, flightRepo, bookingRepo, passengerRepo)

    def setup() {
        tx.withTx(_ as Function) >> { Function fn ->
            return fn.apply(null)
        }
    }

    def "getById succeeds"() {
        given:
        def booking = new Booking(id: 1L)
        bookingRepo.findById(1L) >> Future.succeededFuture(booking)

        when:
        def result = service.getById(1L)

        then:
        result.succeeded()
        result.result().id == 1L
    }

    def "getById fails"() {
        given:
        def ex = new BusinessServiceException(ErrorCodeEnum.ENTITY_NOT_FOUND, "no booking")
        bookingRepo.findById(2L) >> Future.failedFuture(ex)

        when:
        def result = service.getById(2L)

        then:
        result.failed()
        result.cause() == ex
    }


    def "create succeeds when seats available"() {
        given:
        def req = new BookingRequest(10L, 20L, "A1")

        passengerRepo.findById(10L) >> Future.succeededFuture(new Passenger(id: 10L))

        def flight = new Flight(id: 20L, availableSeats: 2, price: new BigDecimal("100.00"), flightNumber: "SA123")
        flightRepo.findByIdForUpdate(_, 20L) >> Future.succeededFuture(flight)

        flightRepo.save(_, flight) >> Future.succeededFuture(flight)

        bookingRepo.save(_, _ as Booking) >> { args ->
            Booking b = args[1]
            b.setId(99L)
            Future.succeededFuture(b)
        }

        when:
        def future = service.create(req)

        then:
        future.succeeded()
        with(future.result()) {
            id == 99L
            passengerId == 10L
            flightId == 20L
            status == BookingStatus.CONFIRMED.name()
            totalAmount == new BigDecimal("100.00")
            bookingReference.startsWith("SA123")
        }
    }

    def "create fails when passenger not found"() {
        given:
        def req = new BookingRequest(10L, 20L, "A1")
        passengerRepo.findById(10L) >> Future.failedFuture(
                new BusinessServiceException(ErrorCodeEnum.ENTITY_NOT_FOUND, "no passenger")
        )

        when:
        def future = service.create(req)

        then:
        future.failed()
        future.cause() instanceof BusinessServiceException
        future.cause().errorEnum == ErrorCodeEnum.ENTITY_NOT_FOUND
    }

    def "create fails when no seats available"() {
        given:
        def req = new BookingRequest(10L, 20L, "A1")

        passengerRepo.findById(10L) >> Future.succeededFuture(new Passenger(id: 10L))
        def flight = new Flight(id: 20L, availableSeats: 0, price: BigDecimal.TEN, flightNumber: "SA123")
        flightRepo.findByIdForUpdate(_, 20L) >> Future.succeededFuture(flight)

        when:
        def future = service.create(req)

        then:
        future.failed()
        future.cause() instanceof BusinessServiceException
        future.cause().errorEnum == ErrorCodeEnum.INVALID_PARAM
    }

    def "create fails when flightRepo.save fails"() {
        given:
        def req = new BookingRequest(10L, 20L, "A1")

        passengerRepo.findById(10L) >> Future.succeededFuture(new Passenger(id: 10L))

        def flight = new Flight(id: 20L, availableSeats: 1, price: BigDecimal.TEN, flightNumber: "SA123")
        flightRepo.findByIdForUpdate(_, 20L) >> Future.succeededFuture(flight)

        flightRepo.save(_, flight) >> Future.failedFuture(new RuntimeException("flight update failed"))

        when:
        def future = service.create(req)

        then:
        future.failed()
        future.cause().message == "flight update failed"
    }

    def "create fails when bookingRepo.save fails"() {
        given:
        def req = new BookingRequest(10L, 20L, "A1")

        passengerRepo.findById(10L) >> Future.succeededFuture(new Passenger(id: 10L))

        def flight = new Flight(id: 20L, availableSeats: 1, price: BigDecimal.TEN, flightNumber: "SA123")
        flightRepo.findByIdForUpdate(_, 20L) >> Future.succeededFuture(flight)

        flightRepo.save(_, flight) >> Future.succeededFuture(flight)
        bookingRepo.save(_, _ as Booking) >> Future.failedFuture(new RuntimeException("booking save failed"))

        when:
        def future = service.create(req)

        then:
        future.failed()
        future.cause().message == "booking save failed"
    }


    def "cancel succeeds and sets status to CANCELLED"() {
        given:
        def booking = new Booking(id: 10L, status: BookingStatus.CONFIRMED.name())
        bookingRepo.findById(_, 10L) >> Future.succeededFuture(booking)
        bookingRepo.save(_, _ as Booking) >> { args -> Future.succeededFuture(args[1]) }

        when:
        def future = service.cancel(10L)

        then:
        future.succeeded()
        booking.status == BookingStatus.CANCELLED.name()
    }

    def "cancel fails when booking not found"() {
        given:
        bookingRepo.findById(_, 10L) >> Future.failedFuture(
                new BusinessServiceException(ErrorCodeEnum.ENTITY_NOT_FOUND, "no booking")
        )

        when:
        def future = service.cancel(10L)

        then:
        future.failed()
        future.cause() instanceof BusinessServiceException
    }


    def "findByPassenger succeeds"() {
        given:
        bookingRepo.findByPassengerId(_, 99L) >> Future.succeededFuture([new Booking(id: 1L), new Booking(id: 2L)])

        when:
        def future = service.findByPassenger(99L)

        then:
        future.succeeded()
        future.result().size() == 2
    }

    def "findByPassenger fails"() {
        given:
        bookingRepo.findByPassengerId(_, 99L) >> Future.failedFuture(new RuntimeException("db error"))

        when:
        def future = service.findByPassenger(99L)

        then:
        future.failed()
        future.cause().message == "db error"
    }

    def "create should set totalAmount to ZERO when flight price is null"() {
        given:
        def req = new BookingRequest(10L, 20L, "A1")

        passengerRepo.findById(10L) >> Future.succeededFuture(new Passenger(id: 10L))

        def flight = new Flight(id: 20L, availableSeats: 1, price: null, flightNumber: "SA123")
        flightRepo.findByIdForUpdate(_, 20L) >> Future.succeededFuture(flight)

        flightRepo.save(_, flight) >> Future.succeededFuture(flight)

        bookingRepo.save(_, _ as Booking) >> { args ->
            Booking b = args[1]
            b.setId(200L)
            Future.succeededFuture(b)
        }

        when:
        def future = service.create(req)

        then:
        future.succeeded()
        def booking = future.result()
        booking.id == 200L
        booking.totalAmount == BigDecimal.ZERO
        booking.status == BookingStatus.CONFIRMED.name()
        booking.bookingReference.startsWith("SA123")
    }

    def "cancel should fail when save() fails and log error branch is hit"() {
        given:
        def booking = new Booking(id: 10L, status: BookingStatus.CONFIRMED.name())

        bookingRepo.findById(_, 10L) >> Future.succeededFuture(booking)
        bookingRepo.save(_, _ as Booking) >> {
            return Future.failedFuture(new RuntimeException("save failed"))
        }

        when:
        def future = service.cancel(10L)

        then:
        future.failed()
        future.cause().message == "save failed"
        booking.status == BookingStatus.CANCELLED.name()
    }


}
