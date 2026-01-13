package com.airline.booking.demo.feature.flights.service

import com.airline.booking.demo.exception.BusinessServiceException
import com.airline.booking.demo.exception.dto.ErrorCodeEnum
import com.airline.booking.demo.feature.airlines.repository.AirlineRepository
import com.airline.booking.demo.feature.airlines.repository.entity.Airline
import com.airline.booking.demo.feature.flights.dto.FlightRequest
import com.airline.booking.demo.feature.flights.repository.FlightRepository
import com.airline.booking.demo.feature.flights.repository.entity.Flight
import com.airline.booking.demo.feature.flights.repository.entity.FlightStatus
import io.vertx.core.Future
import spock.lang.Specification

class FlightServiceSpec extends Specification {

    FlightRepository flightRepo = Mock()
    AirlineRepository airlineRepo = Mock()

    FlightService service = new FlightService(flightRepo, airlineRepo)

    def "getById should return flight when found"() {
        given:
        def flight = new Flight(id: 10L)
        flightRepo.findById(10L) >> Future.succeededFuture(flight)

        when:
        def result = service.getById(10L).result()

        then:
        result.id == 10L
    }

    def "getById should fail when repository fails"() {
        given:
        def ex = new BusinessServiceException(ErrorCodeEnum.ENTITY_NOT_FOUND, "not found")
        flightRepo.findById(99L) >> Future.failedFuture(ex)

        when:
        def future = service.getById(99L)

        then:
        future.failed()
        future.cause() == ex
    }

    def "search should return list"() {
        given:
        def flights = [new Flight(), new Flight()]
        flightRepo.findByRoute("JNB", "CPT") >> Future.succeededFuture(flights)

        when:
        def result = service.search("JNB", "CPT").result()

        then:
        result.size() == 2
    }

    def "create should save flight when airline exists"() {
        given:
        def req = new FlightRequest(
                "SA123",
                5L,
                "JNB",
                "CPT",
                "2026-02-10T10:00:00Z",
                "2026-02-10T12:00:00Z",
                100,
                100,
                new BigDecimal("5000")
        )

        airlineRepo.findById(5L) >> Future.succeededFuture(new Airline())
        flightRepo.save(_ as Flight) >> { Flight f -> Future.succeededFuture(f) }

        when:
        def result = service.create(req).result()

        then:
        result.availableSeats == 100
        result.status == FlightStatus.SCHEDULED.name() || result.status == null
        result.flightNumber == "SA123"
        result.airlineId == 5L
    }

    def "create should fail when airline does not exist"() {
        given:
        def req = new FlightRequest(
                "SA999",
                7L,
                "JNB",
                "CPT",
                "2026-01-10T10:00:00Z",
                "2026-01-10T12:00:00Z",
                150,
                150,
                new BigDecimal("8000")
        )

        def ex = new BusinessServiceException(ErrorCodeEnum.ENTITY_NOT_FOUND, "airline missing")
        airlineRepo.findById(7L) >> Future.failedFuture(ex)

        when:
        def future = service.create(req)

        then:
        future.failed()
        future.cause() == ex
    }

    def "create should keep existing seat and status values"() {
        given:
        def req = new FlightRequest(
                "SA444",
                3L,
                "JNB",
                "CPT",
                "2026-03-10T10:00:00Z",
                "2026-03-10T12:00:00Z",
                120,
                50,
                new BigDecimal("9000")
        )

        airlineRepo.findById(3L) >> Future.succeededFuture(new Airline())
        flightRepo.save(_ as Flight) >> { Flight f ->
            f.setStatus(FlightStatus.SCHEDULED.name())
            return Future.succeededFuture(f)
        }

        when:
        def result = service.create(req).result()

        then:
        result.availableSeats == 50
        result.status == FlightStatus.SCHEDULED.name()
    }
}
