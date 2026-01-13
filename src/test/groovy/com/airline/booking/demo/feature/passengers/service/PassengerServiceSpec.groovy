package com.airline.booking.demo.feature.passengers.service

import com.airline.booking.demo.exception.BusinessServiceException
import com.airline.booking.demo.exception.dto.ErrorCodeEnum
import com.airline.booking.demo.feature.passengers.dto.PassengerRequest
import com.airline.booking.demo.feature.passengers.mapper.PassengerMapper
import com.airline.booking.demo.feature.passengers.repository.PassengerRepository
import com.airline.booking.demo.feature.passengers.repository.entity.Passenger
import io.vertx.core.Future
import spock.lang.Specification

class PassengerServiceSpec extends Specification {

    PassengerRepository repo = Mock()
    PassengerService service = new PassengerService(repo)

    def "create should call repository.save and return created passenger"() {
        given:
        def req = new PassengerRequest(
                "John", "Doe", "test@mail.com", "+999", "A12345", "12-12-1980"
        )

        def mockEntity = PassengerMapper.toEntity(req)
        mockEntity.id = 10L

        repo.save(_ as Passenger) >> { Passenger p -> Future.succeededFuture(mockEntity) }

        when:
        def result = service.create(req).result()

        then:
        result.id == 10L
        result.email == "test@mail.com"
        result.passportNumber == "A12345"
    }

    def "create should propagate repository failure"() {
        given:
        def req = new PassengerRequest(
                "John", "Doe", "fail@mail.com", "+111", "PX123", "12-12-1980"
        )

        def ex = new RuntimeException("DB error")

        repo.save(_ as Passenger) >> Future.failedFuture(ex)

        when:
        def future = service.create(req)

        then:
        future.failed()
        future.cause() == ex
    }

    def "getById should return passenger when found"() {
        given:
        def id = 42L
        def entity = new Passenger(id: id, email: "found@mail.com")

        repo.findById(id) >> Future.succeededFuture(entity)

        when:
        def result = service.getById(id).result()

        then:
        result.id == 42L
        result.email == "found@mail.com"
    }

    def "getById should fail with BusinessServiceException when not found"() {
        given:
        def id = 99L

        repo.findById(id) >> Future.succeededFuture(null)

        when:
        def future = service.getById(id)

        then:
        future.failed()
        future.cause() instanceof BusinessServiceException
        future.cause().errorEnum == ErrorCodeEnum.ENTITY_NOT_FOUND
        future.cause().message.contains("Passenger not found")
    }

    def "getById should propagate repository failure"() {
        given:
        def id = 123L
        def ex = new RuntimeException("DB down")

        repo.findById(id) >> Future.failedFuture(ex)

        when:
        def future = service.getById(id)

        then:
        future.failed()
        future.cause() == ex
    }
}
