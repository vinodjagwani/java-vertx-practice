package com.airline.booking.demo.feature.airlines

import com.airline.booking.demo.common.validation.RequestValidationHandler
import com.airline.booking.demo.feature.airlines.dto.AirlineRequest
import com.airline.booking.demo.feature.airlines.handler.AirlineHandler
import io.vertx.core.Handler
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import spock.lang.Specification
import spock.lang.Subject

class AirlineFeatureSpec extends Specification {

    Router router = Mock()
    Route routeWildcard = Mock()
    Route routeGetAll = Mock()
    Route routeGetById = Mock()
    Route routePost = Mock()

    AirlineHandler handler = Mock()
    RequestValidationHandler requestValidationHandler = Mock()

    @Subject
    AirlineFeature feature = new AirlineFeature(handler, requestValidationHandler)

    def "should register routes and handlers correctly"() {
        given:
        def validationHandler = Mock(Handler)

        when:
        feature.init(router)

        then: "BodyHandler is registered on the wildcard route"
        1 * router.route("/airlines*") >> routeWildcard
        1 * routeWildcard.handler(_ as BodyHandler) >> routeWildcard

        and: "GET routes are registered"
        1 * router.get("/airlines") >> routeGetAll
        1 * routeGetAll.handler(_ as Handler) >> routeGetAll

        1 * router.get("/airlines/:id") >> routeGetById
        1 * routeGetById.handler(_ as Handler) >> routeGetById

        and: "POST route is registered with validation and create handler"
        1 * requestValidationHandler.validate(AirlineRequest.class) >> validationHandler
        1 * router.post("/airlines") >> routePost

        1 * routePost.handler(validationHandler) >> routePost

        1 * routePost.handler(_ as Handler) >> routePost
    }
}
