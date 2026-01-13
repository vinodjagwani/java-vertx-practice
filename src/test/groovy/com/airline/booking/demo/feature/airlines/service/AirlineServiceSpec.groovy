package com.airline.booking.demo.feature.airlines.service

import com.airline.booking.demo.exception.BusinessServiceException
import com.airline.booking.demo.exception.dto.ErrorCodeEnum
import com.airline.booking.demo.feature.airlines.dto.AirlineRequest
import com.airline.booking.demo.feature.airlines.repository.AirlineRepository
import com.airline.booking.demo.feature.airlines.repository.entity.Airline
import io.vertx.core.Future
import spock.lang.Specification

class AirlineServiceSpec extends Specification {

    AirlineRepository repo = Mock(AirlineRepository)
    AirlineService service = new AirlineService(repo)

    def "create() should succeed when airline does not already exist"() {
        given:
        def req = new AirlineRequest("SA", "South African", "ZA")

        and: "findByCodeOptional returns null"
        repo.findByCodeOptional("SA") >> Future.succeededFuture(null)

        and: "save stores entity and assigns id"
        repo.save(_ as Airline) >> { Airline entity ->
            entity.setId(10L)
            Future.succeededFuture(entity)
        }

        when:
        def result = service.create(req).result()

        then:
        result.id == 10L
        result.code == "SA"
        result.name == "South African"
        result.country == "ZA"
    }

    def "create() should fail with CONFLICT when airline already exists"() {
        given:
        def req = new AirlineRequest("SA", "South African", "ZA")

        and: "findByCodeOptional returns existing airline"
        repo.findByCodeOptional("SA") >> Future.succeededFuture(new Airline())

        when:
        def future = service.create(req)

        then:
        future.failed()
        future.cause() instanceof BusinessServiceException
        future.cause().errorEnum == ErrorCodeEnum.CONFLICT
    }

    def "create() should fail if findByCodeOptional() fails"() {
        given:
        def req = new AirlineRequest("SA", "South African", "ZA")

        and: "findByCodeOptional fails"
        repo.findByCodeOptional("SA") >> Future.failedFuture(new RuntimeException("DB down"))

        when:
        def future = service.create(req)

        then:
        future.failed()
        future.cause() instanceof RuntimeException
        future.cause().message == "DB down"
    }

    def "create() should fail when save() fails"() {
        given:
        def req = new AirlineRequest("SA", "South African", "ZA")

        and: "findByCodeOptional returns no airline"
        repo.findByCodeOptional("SA") >> Future.succeededFuture(null)

        and: "save fails"
        repo.save(_ as Airline) >> Future.failedFuture(new RuntimeException("Insert failed"))

        when:
        def future = service.create(req)

        then:
        future.failed()
        future.cause() instanceof RuntimeException
        future.cause().message == "Insert failed"
    }

    def "getAll() should return list of airlines"() {
        given:
        def a1 = new Airline(id: 1L, code: "SA", name: "South African", country: "ZA")
        def a2 = new Airline(id: 2L, code: "BA", name: "British Airways", country: "UK")

        repo.findAll() >> Future.succeededFuture([a1, a2])

        when:
        def result = service.getAll().result()

        then:
        result.size() == 2
        result[0].code == "SA"
        result[1].code == "BA"
    }

    def "getAll() should fail when repository fails"() {
        given:
        repo.findAll() >> Future.failedFuture(new RuntimeException("DB error"))

        when:
        def future = service.getAll()

        then:
        future.failed()
        future.cause().message == "DB error"
    }

    def "getById() should return airline"() {
        given:
        def airline = new Airline(id: 5L, code: "SA", name: "South African", country: "ZA")
        repo.findById(5L) >> Future.succeededFuture(airline)

        when:
        def result = service.getById(5L).result()

        then:
        result.id == 5L
        result.code == "SA"
    }

    def "getById() should fail when repository fails"() {
        given:
        repo.findById(99L) >> Future.failedFuture(new RuntimeException("not found"))

        when:
        def future = service.getById(99L)

        then:
        future.failed()
        future.cause().message == "not found"
    }
}
