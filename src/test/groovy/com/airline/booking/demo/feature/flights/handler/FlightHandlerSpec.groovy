package com.airline.booking.demo.feature.flights.handler

import com.airline.booking.demo.config.JsonConfig
import com.airline.booking.demo.exception.BusinessServiceException
import com.airline.booking.demo.exception.dto.ErrorCodeEnum
import com.airline.booking.demo.feature.flights.dto.FlightRequest
import com.airline.booking.demo.feature.flights.mapper.FlightMapper
import com.airline.booking.demo.feature.flights.repository.entity.Flight
import com.airline.booking.demo.feature.flights.service.FlightService
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Future
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.RoutingContext
import spock.lang.Specification

import java.time.OffsetDateTime

class FlightHandlerSpec extends Specification {

    FlightService service
    RoutingContext ctx
    HttpServerResponse response
    FlightHandler handler

    def setupSpec() {
        JsonConfig.register()
    }

    def setup() {
        service = Mock()
        ctx = Mock()
        response = Mock()
        handler = new FlightHandler(service)
    }

    def "getById should return 200 and json payload"() {
        given:
        ctx.pathParam("id") >> "10"

        def flight = new Flight(
                id: 10L,
                departureTime: OffsetDateTime.now(),
                arrivalTime: OffsetDateTime.now().plusHours(2)
        )

        service.getById(10L) >> Future.succeededFuture(flight)

        when:
        handler.getById(ctx)

        then:
        1 * ctx.json(_)
    }

    def "getById should fail on invalid id"() {
        given:
        ctx.pathParam("id") >> "abc"

        when:
        handler.getById(ctx)

        then:
        1 * ctx.fail(_ as BusinessServiceException) >> { BusinessServiceException ex ->
            assert ex.errorEnum == ErrorCodeEnum.INVALID_PARAM
        }
    }

    def "getById should call ctx.fail on service error"() {
        given:
        ctx.pathParam("id") >> "15"
        def ex = new RuntimeException("boom")

        service.getById(15L) >> Future.failedFuture(ex)

        when:
        handler.getById(ctx)

        then:
        1 * ctx.fail(ex)
    }

    def "search should return 200 with list result"() {
        given:
        ctx.queryParam("from") >> ["JNB"]
        ctx.queryParam("to") >> ["CPT"]

        def flights = [
                new Flight(id: 1L, departureTime: OffsetDateTime.now(), arrivalTime: OffsetDateTime.now()),
                new Flight(id: 2L, departureTime: OffsetDateTime.now(), arrivalTime: OffsetDateTime.now())
        ]

        service.search("JNB", "CPT") >> Future.succeededFuture(flights)

        when:
        handler.search(ctx)

        then:
        1 * ctx.json(_ as List)
    }

    def "create should return 201 and json response"() {
        given:
        def req = new FlightRequest(
                "SA123", 1L, "JNB", "CPT",
                "2026-02-10T10:00:00Z", "2026-02-10T12:00:00Z",
                100, 100, new BigDecimal("5000")
        )

        ctx.get("validated_body") >> req
        ctx.response() >> response

        def flight = FlightMapper.toEntity(req)
        flight.id = 99L

        service.create(req) >> Future.succeededFuture(flight)

        response.setStatusCode(_) >> response
        response.putHeader(_, _) >> response

        when:
        handler.create(ctx)

        then:
        noExceptionThrown()
        1 * response.setStatusCode(HttpResponseStatus.CREATED.code())
        1 * response.putHeader("content-type", _ as String)
        1 * response.end({ it.contains('"id":99') })
        0 * ctx.fail(_)
    }

    def "create should call ctx.fail on service error"() {
        given:
        def req = new FlightRequest(
                "SA123", 1L, "JNB", "CPT",
                "2026-02-10T10:00:00Z", "2026-02-10T12:00:00Z",
                100, 100, new BigDecimal("5000")
        )

        ctx.get("validated_body") >> req

        def ex = new RuntimeException("fail")
        service.create(req) >> Future.failedFuture(ex)

        when:
        handler.create(ctx)

        then:
        1 * ctx.fail(ex)
    }

    def "create should fail on unexpected exception"() {
        given:
        ctx.get("validated_body") >> { throw new IllegalStateException("broken") }

        when:
        handler.create(ctx)

        then:
        1 * ctx.fail(_ as IllegalStateException)
    }
}
