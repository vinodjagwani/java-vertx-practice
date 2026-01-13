package com.airline.booking.demo.feature.airlines.repository.entity;


import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Airline {

    private Long id;
    private String code;
    private String name;
    private String country;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
