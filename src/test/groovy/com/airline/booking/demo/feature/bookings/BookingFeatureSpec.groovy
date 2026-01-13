package com.airline.booking.demo.feature.bookings

import com.airline.booking.demo.common.validation.RequestValidationHandler
import com.airline.booking.demo.feature.bookings.dto.BookingRequest
import com.airline.booking.demo.feature.bookings.handler.BookingHandler
import io.vertx.core.Handler
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import spock.lang.Specification
import spock.lang.Subject

class BookingFeatureSpec extends Specification {

    Router router = Mock()
    Route routeWildcard = Mock()
    Route routePost = Mock()
    Route routeGetById = Mock()
    Route routeDelete = Mock()

    BookingHandler handler = Mock()
    RequestValidationHandler requestValidationHandler = Mock()

    @Subject
    BookingFeature feature = new BookingFeature(handler, requestValidationHandler)

    def "should register booking routes and handlers correctly"() {
        given: "A dummy validation handler"
        def validationHandler = Mock(Handler)

        when: "The feature is initialized"
        feature.init(router)

        then: "BodyHandler is registered on the wildcard route"
        1 * router.route("/bookings*") >> routeWildcard
        1 * routeWildcard.handler(_ as BodyHandler) >> routeWildcard

        and: "POST route is registered with validation and create handler"
        1 * requestValidationHandler.validate(BookingRequest.class) >> validationHandler
        1 * router.post("/bookings") >> routePost

        1 * routePost.handler(validationHandler) >> routePost
        1 * routePost.handler(_ as Handler) >> routePost

        and: "GET route is registered for getById"
        1 * router.get("/bookings/:id") >> routeGetById
        1 * routeGetById.handler(_ as Handler) >> routeGetById

        and: "DELETE route is registered for cancel"
        1 * router.delete("/bookings/:id") >> routeDelete
        1 * routeDelete.handler(_ as Handler) >> routeDelete
    }
}
