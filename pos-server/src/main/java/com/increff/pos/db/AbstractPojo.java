package com.increff.pos.db;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Data
public abstract class AbstractPojo {

    @Id
    private String id;

    @CreatedDate
    private ZonedDateTime createdAt;

    @LastModifiedDate
    private ZonedDateTime updatedAt;
}
