package com.increff.pos.flow;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderFlowTest {

    @Mock private OrderApi orderApi;
    @Mock private InventoryApi inventoryApi;

    @InjectMocks private OrderFlow orderFlow;

    @Test
    void create_setsFulfillable_whenInventoryDeducts() throws Exception {
        OrderPojo order = sampleOrder();

        when(orderApi.orderReferenceIdExists(anyString())).thenReturn(false);
        when(inventoryApi.tryDeductInventoryForOrder(anyList())).thenReturn(true);
        when(orderApi.createOrder(any(OrderPojo.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPojo saved = orderFlow.create(order);

        assertNotNull(saved.getOrderReferenceId());
        assertNotNull(saved.getOrderTime());
        assertEquals(OrderStatus.FULFILLABLE.name(), saved.getStatus());
        verify(orderApi).createOrder(any(OrderPojo.class));
    }

    @Test
    void markInvoiced_throws_whenAlreadyInvoiced() throws Exception {
        OrderPojo order = sampleOrder();
        order.setStatus(OrderStatus.INVOICED.name());

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(order);

        ApiException ex = assertThrows(ApiException.class, () -> orderFlow.markInvoiced("ORD-1"));
        assertTrue(ex.getMessage().toLowerCase().contains("already invoiced"));

        verify(orderApi, never()).updateOrder(any(OrderPojo.class));
    }

    private OrderPojo sampleOrder() {
        OrderItemPojo item = new OrderItemPojo();
        item.setProductBarcode("P1");
        item.setOrderedQuantity(1);
        item.setSellingPrice(10.0);

        OrderPojo order = new OrderPojo();
        order.setOrderItems(List.of(item));
        return order;
    }
}
