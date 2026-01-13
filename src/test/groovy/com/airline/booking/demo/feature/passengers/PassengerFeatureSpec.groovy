package com.airline.booking.demo.feature.passengers

import com.airline.booking.demo.common.validation.RequestValidationHandler
import com.airline.booking.demo.feature.passengers.dto.PassengerRequest
import com.airline.booking.demo.feature.passengers.handler.PassengerHandler
import io.vertx.core.Handler
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import spock.lang.Specification
import spock.lang.Subject

class PassengerFeatureSpec extends Specification {

    Router router = Mock()
    Route routeWildcard = Mock()
    Route routePost = Mock()
    Route routeGetBookings = Mock()

    PassengerHandler handler = Mock()
    RequestValidationHandler requestValidationHandler = Mock()

    @Subject
    PassengerFeature feature = new PassengerFeature(handler, requestValidationHandler)

    def "should register passenger routes and handlers correctly"() {
        given: "A mocked validation handler"
        def validationHandler = Mock(Handler)

        when: "The feature is initialized"
        feature.init(router)

        then: "BodyHandler is registered on the wildcard route"
        1 * router.route("/passengers*") >> routeWildcard
        1 * routeWildcard.handler(_ as BodyHandler) >> routeWildcard

        and: "POST route is registered with validation and create handler"
        1 * requestValidationHandler.validate(PassengerRequest.class) >> validationHandler
        1 * router.post("/passengers") >> routePost

        1 * routePost.handler(validationHandler) >> routePost
        1 * routePost.handler(_ as Handler) >> routePost

        and: "GET route is registered for getBookings"
        1 * router.get("/passengers/:id/bookings") >> routeGetBookings
        1 * routeGetBookings.handler(_ as Handler) >> routeGetBookings
    }
}
