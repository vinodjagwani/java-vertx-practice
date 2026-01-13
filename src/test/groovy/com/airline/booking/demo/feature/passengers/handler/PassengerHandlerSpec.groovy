package com.airline.booking.demo.feature.passengers.handler

import com.airline.booking.demo.exception.BusinessServiceException
import com.airline.booking.demo.feature.bookings.service.BookingService
import com.airline.booking.demo.feature.passengers.dto.PassengerRequest
import com.airline.booking.demo.feature.passengers.mapper.PassengerMapper
import com.airline.booking.demo.feature.passengers.repository.entity.Passenger
import com.airline.booking.demo.feature.passengers.service.PassengerService
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Future
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.RoutingContext
import spock.lang.Specification

class PassengerHandlerSpec extends Specification {

    BookingService bookingService = Mock()
    PassengerService passengerService = Mock()
    PassengerHandler handler = new PassengerHandler(bookingService, passengerService)

    RoutingContext ctx = Mock()
    def response = Mock(HttpServerResponse)

    def "getBookings should return JSON list when passenger exists"() {
        given:
        ctx.pathParam("id") >> "10"
        passengerService.getById(10L) >> Future.succeededFuture(new Passenger(id: 10L))
        bookingService.findByPassenger(10L) >> Future.succeededFuture([[:], [:]])

        when:
        handler.getBookings(ctx)

        then:
        1 * ctx.json(_ as List)
    }

    def "getBookings should fail on invalid id"() {
        given:
        ctx.pathParam("id") >> "abc"

        when:
        handler.getBookings(ctx)

        then:
        1 * ctx.fail(_ as BusinessServiceException)
    }

    def "getBookings should call ctx.fail when passenger lookup fails"() {
        given:
        ctx.pathParam("id") >> "10"
        def ex = new RuntimeException("notfound")
        passengerService.getById(10L) >> Future.failedFuture(ex)

        when:
        handler.getBookings(ctx)

        then:
        1 * ctx.fail(ex)
    }

    def "create should return 201 and JSON response"() {
        given:
        def req = new PassengerRequest("John", "Doe", "john@doe.com", "+123", "SA12345", "22-09-2029")
        ctx.get("validated_body") >> req
        ctx.response() >> response

        def passenger = PassengerMapper.toEntity(req)
        passenger.id = 99L

        passengerService.create(req) >> Future.succeededFuture(passenger)

        response.setStatusCode(_) >> response
        response.putHeader(_, _) >> response
        response.end(_ as String) >> {}

        when:
        handler.create(ctx)

        then:
        1 * response.setStatusCode(HttpResponseStatus.CREATED.code())
        1 * response.putHeader("content-type", { it.contains("application/json") })
        1 * response.end(_ as String)
    }

    def "create should call ctx.fail on service error"() {
        given:
        def req = new PassengerRequest("John", "Doe", "john@doe.com", "+123", "SA", "01-01-1999")
        ctx.get("validated_body") >> req

        def ex = new RuntimeException("fail")
        passengerService.create(req) >> Future.failedFuture(ex)

        when:
        handler.create(ctx)

        then:
        1 * ctx.fail(ex)
    }

    def "create should fail on unexpected exception"() {
        given:
        ctx.get("validated_body") >> { throw new IllegalStateException("boom") }

        when:
        handler.create(ctx)

        then:
        1 * ctx.fail(_ as IllegalStateException)
    }
}
