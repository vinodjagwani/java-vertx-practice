package com.airline.booking.demo.feature.bookings.handler

import com.airline.booking.demo.feature.bookings.dto.BookingRequest
import com.airline.booking.demo.feature.bookings.mapper.BookingMapper
import com.airline.booking.demo.feature.bookings.repository.entity.Booking
import com.airline.booking.demo.feature.bookings.service.BookingService
import com.google.common.net.MediaType
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Future
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.ext.web.RoutingContext
import spock.lang.Specification

class BookingHandlerSpec extends Specification {

    BookingService bookingService = Mock()
    BookingHandler handler = new BookingHandler(bookingService)
    RoutingContext ctx = Mock()

    def "create should return 201 and json payload"() {
        given:
        def req = new BookingRequest(1L, 2L, "1LAX")
        ctx.get("validated_body") >> req

        def booking = new Booking(id: 10L)
        def response = Mock(HttpServerResponse)
        ctx.response() >> response

        bookingService.create(req) >> Future.succeededFuture(booking)

        when:
        handler.create(ctx)

        then:
        1 * response.setStatusCode(HttpResponseStatus.CREATED.code())
        1 * response.putHeader("content-type", MediaType.JSON_UTF_8.toString())
        1 * response.end(Json.encode(BookingMapper.toResponse(booking)))
    }

    def "getById should return 200 and json payload"() {
        given:
        ctx.pathParam("id") >> "7"

        def booking = new Booking(id: 7L)
        bookingService.getById(7L) >> Future.succeededFuture(booking)

        when:
        handler.getById(ctx)

        then:
        1 * ctx.json(BookingMapper.toResponse(booking))
    }

    def "cancel should return 204 with no content"() {
        given:
        ctx.pathParam("id") >> "5"

        def response = Mock(HttpServerResponse)
        ctx.response() >> response

        bookingService.cancel(5L) >> Future.succeededFuture()

        when:
        handler.cancel(ctx)

        then:
        1 * response.setStatusCode(HttpResponseStatus.NO_CONTENT.code()) >> response
        1 * response.end()
    }

    def "create should call ctx.fail on error"() {
        given:
        def req = new BookingRequest(1L, 2L, "1LAX")
        ctx.get("validated_body") >> req

        def ex = new RuntimeException("fail")
        bookingService.create(req) >> Future.failedFuture(ex)

        when:
        handler.create(ctx)

        then:
        1 * ctx.fail(ex)
    }
}
