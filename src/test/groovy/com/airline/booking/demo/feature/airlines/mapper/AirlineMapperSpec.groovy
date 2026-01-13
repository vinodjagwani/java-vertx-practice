package com.airline.booking.demo.feature.airlines.mapper

import com.airline.booking.demo.feature.airlines.dto.AirlineRequest
import com.airline.booking.demo.feature.airlines.dto.AirlineResponse
import com.airline.booking.demo.feature.airlines.repository.entity.Airline
import spock.lang.Specification

class AirlineMapperSpec extends Specification {

    def "toEntity should map AirlineRequest to Airline entity"() {
        given:
        def req = new AirlineRequest("SA", "South African", "ZA")

        when:
        def entity = AirlineMapper.toEntity(req)

        then:
        entity != null
        entity.code == "SA"
        entity.name == "South African"
        entity.country == "ZA"
        entity.id == null
        entity.createdAt == null
        entity.updatedAt == null
    }

    def "toResponse should map Airline entity to AirlineResponse"() {
        given:
        def entity = new Airline()
        entity.setId(10L)
        entity.setCode("BA")
        entity.setName("British Airways")
        entity.setCountry("UK")

        when:
        def res = AirlineMapper.toResponse(entity)

        then:
        res != null
        res instanceof AirlineResponse
        res.id() == 10L
        res.code() == "BA"
        res.name() == "British Airways"
        res.country() == "UK"
    }

    def "toEntity should throw NPE when request is null"() {
        when:
        AirlineMapper.toEntity(null)

        then:
        thrown(NullPointerException)
    }

    def "toResponse should throw NPE when entity is null"() {
        when:
        AirlineMapper.toResponse(null)

        then:
        thrown(NullPointerException)
    }
}
