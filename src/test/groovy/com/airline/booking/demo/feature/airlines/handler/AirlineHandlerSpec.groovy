package com.airline.booking.demo.feature.airlines.handler

import com.airline.booking.demo.feature.airlines.dto.AirlineRequest
import com.airline.booking.demo.feature.airlines.dto.AirlineResponse
import com.airline.booking.demo.feature.airlines.repository.entity.Airline
import com.airline.booking.demo.feature.airlines.service.AirlineService
import io.vertx.core.Future
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.RoutingContext
import spock.lang.Specification

class AirlineHandlerSpec extends Specification {

    def airlineService = Mock(AirlineService)
    def handler = new AirlineHandler(airlineService)

    def "getAll should map entities to response and write JSON"() {
        given:
        def ctx = Mock(RoutingContext)
        def entities = [new Airline(id: 1L, code: "BA", name: "British Airways", country: "UK")]
        airlineService.getAll() >> Future.succeededFuture(entities)

        when:
        handler.getAll(ctx)

        then:
        1 * ctx.json({ List<AirlineResponse> res -> res[0].id() == 1L })
        0 * ctx.fail(_)
    }

    def "getById should map entity and write JSON"() {
        given:
        def ctx = Mock(RoutingContext)
        ctx.pathParam("id") >> "10"

        def entity = new Airline(id: 10L, code: "BA", name: "British Airways", country: "UK")
        airlineService.getById(10L) >> Future.succeededFuture(entity)

        when:
        handler.getById(ctx)

        then:
        1 * ctx.json({ AirlineResponse res -> res.id() == 10L })
        0 * ctx.fail(_)
    }

    def "create should set status 201 and write mapped JSON"() {
        given:
        def ctx = Mock(RoutingContext)
        def response = Mock(HttpServerResponse)

        response.setStatusCode(_ as int) >> {}
        response.putHeader(_ as String, _ as String) >> {}
        response.end(_ as String) >> {}

        ctx.get("validated_body") >> new AirlineRequest("BA", "British Airways", "UK")
        ctx.response() >> response

        airlineService.create(_ as AirlineRequest) >>
                Future.succeededFuture(new Airline(id: 100L, code: "BA", name: "British Airways", country: "UK"))

        when:
        handler.create(ctx)

        then:
        1 * response.setStatusCode(201)
        1 * response.putHeader("content-type", _ as String)
        1 * response.end({ it.contains('"id":100') })
        0 * ctx.fail(_)
    }


    def "Generic failure should call ctx.fail"() {
        given:
        def ctx = Mock(RoutingContext)
        airlineService.getAll() >> Future.failedFuture(new RuntimeException("DB error"))

        when:
        handler.getAll(ctx)

        then:
        1 * ctx.fail(_ as Throwable)
    }
}
