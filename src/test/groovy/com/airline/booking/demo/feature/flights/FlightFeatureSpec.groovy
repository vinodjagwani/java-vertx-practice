package com.airline.booking.demo.feature.flights

import com.airline.booking.demo.common.validation.RequestValidationHandler
import com.airline.booking.demo.feature.flights.dto.FlightRequest
import com.airline.booking.demo.feature.flights.handler.FlightHandler
import io.vertx.core.Handler
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import spock.lang.Specification
import spock.lang.Subject

class FlightFeatureSpec extends Specification {

    Router router = Mock()
    Route routeWildcard = Mock()
    Route routeSearch = Mock()
    Route routeGetById = Mock()
    Route routePost = Mock()

    FlightHandler handler = Mock()
    RequestValidationHandler requestValidationHandler = Mock()

    @Subject
    FlightFeature feature = new FlightFeature(handler, requestValidationHandler)

    def "should register flight routes and handlers correctly"() {
        given: "A mocked validation handler"
        def validationHandler = Mock(Handler)

        when: "The feature is initialized"
        feature.init(router)

        then: "BodyHandler is registered on the wildcard route"
        1 * router.route("/flights*") >> routeWildcard
        1 * routeWildcard.handler(_ as BodyHandler) >> routeWildcard

        and: "GET search route is registered"
        1 * router.get("/flights/search") >> routeSearch
        1 * routeSearch.handler(_ as Handler) >> routeSearch

        and: "GET by ID route is registered"
        1 * router.get("/flights/:id") >> routeGetById
        1 * routeGetById.handler(_ as Handler) >> routeGetById

        and: "POST route is registered with validation chain"
        1 * requestValidationHandler.validate(FlightRequest.class) >> validationHandler
        1 * router.post("/flights") >> routePost

        1 * routePost.handler(validationHandler) >> routePost

        1 * routePost.handler(_ as Handler) >> routePost
    }
}
