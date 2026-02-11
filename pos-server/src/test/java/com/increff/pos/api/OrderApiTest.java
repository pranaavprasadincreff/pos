package com.increff.pos.api;

import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.subdocs.OrderItemPojo;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.test.AbstractUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OrderApiTest extends AbstractUnitTest {

    @Autowired
    private OrderApi orderApi;

    @Autowired
    private MongoOperations mongoOperations;

    // -------------------- helpers --------------------

    private static OrderPojo order(String ref, String status) {
        OrderItemPojo item = new OrderItemPojo();
        item.setProductId("PID-1");
        item.setOrderedQuantity(1);
        item.setSellingPrice(10.0);

        OrderPojo o = new OrderPojo();
        o.setOrderReferenceId(ref);
        o.setStatus(status);
        o.setOrderItems(List.of(item));
        return o;
    }

    private void forceCreatedAt(String orderReferenceId, ZonedDateTime createdAt) {
        Query q = Query.query(Criteria.where("orderReferenceId").is(orderReferenceId));
        Update u = new Update().set("createdAt", createdAt);
        mongoOperations.updateFirst(q, u, OrderPojo.class);
    }

    // -------------------- tests --------------------

    @Test
    public void testGetByOrderReferenceIdNotFound() {
        ApiException ex = assertThrows(ApiException.class,
                () -> orderApi.getByOrderReferenceId("ORD-NOT-EXIST"));
        assertTrue(ex.getMessage().toLowerCase().contains("not found"));
    }

    @Test
    public void testCreateAndGetByOrderReferenceIdSuccess() throws ApiException {
        OrderPojo created = orderApi.createOrder(order(
                "ORD-1",
                OrderStatus.FULFILLABLE.name()
        ));

        assertNotNull(created);
        assertEquals("ORD-1", created.getOrderReferenceId());

        OrderPojo fetched = orderApi.getByOrderReferenceId("ORD-1");
        assertNotNull(fetched);
        assertEquals("ORD-1", fetched.getOrderReferenceId());
        assertEquals(OrderStatus.FULFILLABLE.name(), fetched.getStatus());
        assertNotNull(fetched.getOrderItems());
        assertFalse(fetched.getOrderItems().isEmpty());

        assertEquals("PID-1", fetched.getOrderItems().getFirst().getProductId());
    }

    @Test
    public void testOrderReferenceIdExists() throws ApiException {
        assertFalse(orderApi.orderReferenceIdExists("ORD-EXISTS"));

        orderApi.createOrder(order(
                "ORD-EXISTS",
                OrderStatus.FULFILLABLE.name()
        ));

        assertTrue(orderApi.orderReferenceIdExists("ORD-EXISTS"));
    }

    @Test
    public void testSearchByRefContains_caseInsensitive() throws ApiException {
        orderApi.createOrder(order("ORD-ABCD-0001", OrderStatus.FULFILLABLE.name()));
        orderApi.createOrder(order("ORD-WXYZ-0002", OrderStatus.FULFILLABLE.name()));

        Page<OrderPojo> page = orderApi.search(
                "abcd",
                null,
                null,
                null,
                0,
                10
        );

        assertEquals(1, page.getTotalElements());
        assertEquals("ORD-ABCD-0001", page.getContent().getFirst().getOrderReferenceId());
    }

    @Test
    public void testSearchByStatus_exactMatch() throws ApiException {
        orderApi.createOrder(order("ORD-S1", OrderStatus.FULFILLABLE.name()));
        orderApi.createOrder(order("ORD-S2", OrderStatus.CANCELLED.name()));

        Page<OrderPojo> page = orderApi.search(
                null,
                OrderStatus.CANCELLED.name(),
                null,
                null,
                0,
                10
        );

        assertEquals(1, page.getTotalElements());
        assertEquals("ORD-S2", page.getContent().getFirst().getOrderReferenceId());
        assertEquals(OrderStatus.CANCELLED.name(), page.getContent().getFirst().getStatus());
    }

    @Test
    public void testSearchByTimeRange_filtersCorrectly() throws ApiException {
        ZonedDateTime now = ZonedDateTime.now();

        orderApi.createOrder(order("ORD-T1", OrderStatus.FULFILLABLE.name()));
        orderApi.createOrder(order("ORD-T2", OrderStatus.FULFILLABLE.name()));
        orderApi.createOrder(order("ORD-T3", OrderStatus.FULFILLABLE.name()));

        // Force createdAt AFTER save so auditing can't override it
        forceCreatedAt("ORD-T1", now.minusDays(10));
        forceCreatedAt("ORD-T2", now.minusDays(2));
        forceCreatedAt("ORD-T3", now.minusHours(1));

        ZonedDateTime from = now.minusDays(3);
        ZonedDateTime to = now.plusSeconds(5);

        Page<OrderPojo> page = orderApi.search(
                null,
                null,
                from,
                to,
                0,
                10
        );

        assertEquals(2, page.getTotalElements());

        // DAO should sort by createdAt desc
        assertEquals("ORD-T3", page.getContent().get(0).getOrderReferenceId());
        assertEquals("ORD-T2", page.getContent().get(1).getOrderReferenceId());
    }

    @Test
    public void testSearch_combinedFilters_andPagination_sorting() throws ApiException {
        ZonedDateTime now = ZonedDateTime.now();

        orderApi.createOrder(order("ORD-X-1", OrderStatus.FULFILLABLE.name()));
        orderApi.createOrder(order("ORD-X-2", OrderStatus.FULFILLABLE.name()));
        orderApi.createOrder(order("ORD-X-3", OrderStatus.FULFILLABLE.name()));

        // Force ordering deterministically
        forceCreatedAt("ORD-X-1", now.minusMinutes(30));
        forceCreatedAt("ORD-X-2", now.minusMinutes(20));
        forceCreatedAt("ORD-X-3", now.minusMinutes(10));

        Page<OrderPojo> page0 = orderApi.search(
                "x",
                OrderStatus.FULFILLABLE.name(),
                null,
                null,
                0,
                2
        );

        assertEquals(3, page0.getTotalElements());
        assertEquals(2, page0.getContent().size());
        assertEquals("ORD-X-3", page0.getContent().get(0).getOrderReferenceId());
        assertEquals("ORD-X-2", page0.getContent().get(1).getOrderReferenceId());

        Page<OrderPojo> page1 = orderApi.search(
                "x",
                OrderStatus.FULFILLABLE.name(),
                null,
                null,
                1,
                2
        );

        assertEquals(3, page1.getTotalElements());
        assertEquals(1, page1.getContent().size());
        assertEquals("ORD-X-1", page1.getContent().getFirst().getOrderReferenceId());
    }
}
