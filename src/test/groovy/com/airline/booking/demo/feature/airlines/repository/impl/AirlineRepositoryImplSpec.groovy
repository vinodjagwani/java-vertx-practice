package com.airline.booking.demo.feature.airlines.repository.impl

import com.airline.booking.demo.feature.airlines.repository.entity.Airline
import io.vertx.core.Future
import io.vertx.sqlclient.*
import spock.lang.Specification
import spock.lang.Subject

import java.time.OffsetDateTime

class AirlineRepositoryImplSpec extends Specification {

    Pool pool = Mock(Pool)
    SqlConnection connection = Mock(SqlConnection)
    PreparedQuery<RowSet<Row>> preparedQuery = Mock(PreparedQuery)
    Query<RowSet<Row>> simpleQuery = Mock(Query)
    RowSet<Row> rowSet = Mock(RowSet)
    RowIterator<Row> rowIterator = Mock(RowIterator)
    Row row = Mock(Row)

    @Subject
    AirlineRepositoryImpl repository = new AirlineRepositoryImpl(pool)

    def setup() {
        pool.getConnection() >> Future.succeededFuture(connection)
        connection.close() >> Future.succeededFuture()

        pool.preparedQuery(_ as String) >> preparedQuery
        connection.preparedQuery(_ as String) >> preparedQuery
        preparedQuery.execute(_ as Tuple) >> Future.succeededFuture(rowSet)

        pool.query(_ as String) >> simpleQuery
        connection.query(_ as String) >> simpleQuery
        simpleQuery.execute() >> Future.succeededFuture(rowSet)

        row.getLong("id") >> 1L
        row.getString("code") >> "TH"
        row.getString("name") >> "Thai Airways"
        row.getString("country") >> "Thailand"
        row.get(_ as Class, _ as String) >> { OffsetDateTime.now() }
        row.getValue(_ as String) >> OffsetDateTime.now()
    }

    def "findById should return airline when record exists"() {
        given:
        rowSet.iterator() >> rowIterator
        rowIterator.hasNext() >>> [true, false]
        rowIterator.next() >> row

        when:
        def result = repository.findById(1L)

        then:
        result.succeeded()
        result.result().code == "TH"
        result.result().name == "Thai Airways"
    }

    def "findByCode should return airline when record exists"() {
        given:
        rowSet.iterator() >> rowIterator
        rowIterator.hasNext() >>> [true, false]
        rowIterator.next() >> row

        when:
        def result = repository.findByCode("TH")

        then:
        result.succeeded()
        result.result().id == 1L
    }

    def "findAll should return list of airlines"() {
        given:
        def list = [row, row]
        rowSet.spliterator() >> list.spliterator()

        when:
        def result = repository.findAll()

        then:
        result.succeeded()
        result.result().size() == 2
        result.result()[0].code == "TH"
    }

    def "save should perform insert when ID is null"() {
        given:
        Airline airline = new Airline(code: "TH", name: "Thai Airways")

        def insertIterator = Mock(RowIterator)
        def findIterator = Mock(RowIterator)

        insertIterator.hasNext() >>> [true, false]
        insertIterator.next() >> row

        findIterator.hasNext() >>> [true, false]
        findIterator.next() >> row

        row.getLong("id") >>> [5L, 5L]

        rowSet.iterator() >> { insertIterator } >> { findIterator }

        when:
        def result = repository.save(airline)

        then:
        result.succeeded()
        result.result().code == "TH"
        1 * connection.close()
    }

    def "save should perform update when ID is present"() {
        given:
        Airline airline = new Airline(id: 1L, code: "TH", name: "Thai New")

        rowSet.iterator() >> rowIterator
        rowIterator.hasNext() >>> [true, false]
        rowIterator.next() >> row

        when:
        def result = repository.save(airline)

        then:
        result.succeeded()
        result.result().code == "TH"
    }

    def "findById should fail with ENTITY_NOT_FOUND when record missing"() {
        given:
        rowSet.iterator() >> rowIterator
        rowIterator.hasNext() >> false

        when:
        def result = repository.findById(99L)

        then:
        result.failed()
        result.cause().message.contains("Airline not found with id: 99")
    }

    def "findByCodeOptional should return airline when record exists"() {
        given:
        rowSet.iterator() >> rowIterator
        rowIterator.hasNext() >>> [true, false]
        rowIterator.next() >> row

        when:
        def result = repository.findByCodeOptional("TH")

        then:
        result.succeeded()
        result.result() != null
        result.result().code == "TH"
        result.result().name == "Thai Airways"
    }

    def "findByCodeOptional should return null when no record exists"() {
        given:
        rowSet.iterator() >> rowIterator
        rowIterator.hasNext() >> false

        when:
        def result = repository.findByCodeOptional("XX")

        then:
        result.succeeded()
        result.result() == null
    }
}
