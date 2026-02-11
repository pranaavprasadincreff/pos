package com.increff.pos.flow;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.db.subdocs.OrderItemPojo;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderFlowTest {

    @Mock private OrderApi orderApi;
    @Mock private InventoryApi inventoryApi;
    @Mock private ProductApi productApi;

    @InjectMocks private OrderFlow orderFlow;

    // ---------------- CREATE ----------------

    @Test
    void create_happyPath_setsFulfillable_deducts_andPersists() throws Exception {
        OrderPojo orderRequest = orderWithItems(
                itemWithProductId("ID1", 2, 90.0),
                itemWithProductId("ID1", 3, 90.0)
        );
        orderRequest.setOrderReferenceId("ORD-1");

        ProductPojo product = product("ID1", "B1", 100.0);
        when(productApi.findByIds(eq(List.of("ID1")))).thenReturn(List.of(product));

        when(inventoryApi.isSufficientInventoryBulk(eq(Map.of("ID1", 5)))).thenReturn(true);
        doNothing().when(inventoryApi).deductInventoryBulk(eq(Map.of("ID1", 5)));

        when(orderApi.createOrder(any(OrderPojo.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPojo out = orderFlow.create(orderRequest);

        assertNotNull(out);
        assertEquals(OrderStatus.FULFILLABLE.name(), out.getStatus());
        assertEquals("ORD-1", out.getOrderReferenceId());
        assertEquals(2, out.getOrderItems().size());

        verify(productApi, atLeastOnce()).findByIds(eq(List.of("ID1")));
        verify(inventoryApi).isSufficientInventoryBulk(eq(Map.of("ID1", 5)));
        verify(inventoryApi).deductInventoryBulk(eq(Map.of("ID1", 5)));
        verify(orderApi).createOrder(any(OrderPojo.class));
    }

    @Test
    void create_insufficientInventory_setsUnfulfillable_doesNotDeduct() throws Exception {
        OrderPojo orderRequest = orderWithItems(itemWithProductId("ID1", 6, 90.0));
        orderRequest.setOrderReferenceId("ORD-1");

        ProductPojo product = product("ID1", "B1", 100.0);
        when(productApi.findByIds(eq(List.of("ID1")))).thenReturn(List.of(product));

        when(inventoryApi.isSufficientInventoryBulk(eq(Map.of("ID1", 6)))).thenReturn(false);

        when(orderApi.createOrder(any(OrderPojo.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPojo out = orderFlow.create(orderRequest);

        assertEquals(OrderStatus.UNFULFILLABLE.name(), out.getStatus());

        verify(productApi, atLeastOnce()).findByIds(eq(List.of("ID1")));
        verify(inventoryApi).isSufficientInventoryBulk(eq(Map.of("ID1", 6)));
        verify(inventoryApi, never()).deductInventoryBulk(anyMap());
        verify(orderApi).createOrder(any(OrderPojo.class));
    }

    @Test
    void create_sellingPriceExceedsMrp_throws_andDoesNotTouchInventoryOrPersist() throws Exception {
        OrderPojo orderRequest = orderWithItems(itemWithProductId("ID1", 1, 150.0));
        orderRequest.setOrderReferenceId("ORD-1");

        ProductPojo product = product("ID1", "B1", 100.0);
        when(productApi.findByIds(eq(List.of("ID1")))).thenReturn(List.of(product));

        ApiException ex = assertThrows(ApiException.class, () -> orderFlow.create(orderRequest));
        assertTrue(ex.getMessage().toLowerCase().contains("mrp"));

        verify(productApi).findByIds(eq(List.of("ID1")));
        verifyNoInteractions(inventoryApi);
        verify(orderApi, never()).createOrder(any());
    }

    // ---------------- UPDATE ----------------

    @Test
    void update_restoresOld_thenDeductsNew_whenFulfillable() throws Exception {
        OrderPojo existing = orderWithItems(itemWithProductId("ID1", 2, 10.0));
        existing.setStatus(OrderStatus.FULFILLABLE.name());
        existing.setOrderReferenceId("ORD-1");
        existing.setCreatedAt(ZonedDateTime.now().minusMinutes(10));

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(existing);

        OrderPojo updateRequest = orderWithItems(itemWithProductId("ID1", 5, 10.0));
        updateRequest.setOrderReferenceId("ORD-1");

        ProductPojo product = product("ID1", "B1", 100.0);
        when(productApi.findByIds(eq(List.of("ID1")))).thenReturn(List.of(product));

        doNothing().when(inventoryApi).incrementInventoryBulk(eq(Map.of("ID1", 2)));

        when(inventoryApi.isSufficientInventoryBulk(eq(Map.of("ID1", 5)))).thenReturn(true);
        doNothing().when(inventoryApi).deductInventoryBulk(eq(Map.of("ID1", 5)));

        when(orderApi.updateOrder(any(OrderPojo.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPojo out = orderFlow.update(updateRequest);

        assertEquals(OrderStatus.FULFILLABLE.name(), out.getStatus());

        InOrder inOrder = inOrder(inventoryApi, orderApi);
        inOrder.verify(inventoryApi).incrementInventoryBulk(eq(Map.of("ID1", 2)));
        inOrder.verify(inventoryApi).isSufficientInventoryBulk(eq(Map.of("ID1", 5)));
        inOrder.verify(inventoryApi).deductInventoryBulk(eq(Map.of("ID1", 5)));
        inOrder.verify(orderApi).updateOrder(any(OrderPojo.class));

        verify(productApi, atLeastOnce()).findByIds(eq(List.of("ID1")));
    }

    @Test
    void update_restoresOld_butDoesNotDeduct_whenNewIsUnfulfillable() throws Exception {
        OrderPojo existing = orderWithItems(itemWithProductId("ID1", 2, 10.0));
        existing.setStatus(OrderStatus.FULFILLABLE.name());
        existing.setOrderReferenceId("ORD-1");
        existing.setCreatedAt(ZonedDateTime.now().minusMinutes(10));

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(existing);

        OrderPojo updateRequest = orderWithItems(itemWithProductId("ID1", 10, 10.0));
        updateRequest.setOrderReferenceId("ORD-1");

        ProductPojo product = product("ID1", "B1", 100.0);
        when(productApi.findByIds(eq(List.of("ID1")))).thenReturn(List.of(product));

        doNothing().when(inventoryApi).incrementInventoryBulk(eq(Map.of("ID1", 2)));
        when(inventoryApi.isSufficientInventoryBulk(eq(Map.of("ID1", 10)))).thenReturn(false);

        when(orderApi.updateOrder(any(OrderPojo.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPojo out = orderFlow.update(updateRequest);

        assertEquals(OrderStatus.UNFULFILLABLE.name(), out.getStatus());

        verify(inventoryApi).incrementInventoryBulk(eq(Map.of("ID1", 2)));
        verify(inventoryApi).isSufficientInventoryBulk(eq(Map.of("ID1", 10)));
        verify(inventoryApi, never()).deductInventoryBulk(anyMap());
        verify(orderApi).updateOrder(any(OrderPojo.class));

        verify(productApi, atLeastOnce()).findByIds(eq(List.of("ID1")));
    }

    // ---------------- CANCEL ----------------

    @Test
    void cancel_whenFulfillable_restoresInventory() throws Exception {
        OrderPojo existing = orderWithItems(itemWithProductId("ID1", 3, 10.0));
        existing.setStatus(OrderStatus.FULFILLABLE.name());
        existing.setOrderReferenceId("ORD-1");
        existing.setCreatedAt(ZonedDateTime.now().minusMinutes(10));

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(existing);

        doNothing().when(inventoryApi).incrementInventoryBulk(eq(Map.of("ID1", 3)));
        when(orderApi.updateOrder(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderPojo cancelled = orderFlow.cancel("ORD-1");

        assertEquals(OrderStatus.CANCELLED.name(), cancelled.getStatus());
        verify(inventoryApi).incrementInventoryBulk(eq(Map.of("ID1", 3)));
        verify(orderApi).updateOrder(any());
    }

    @Test
    void cancel_whenUnfulfillable_doesNotRestoreInventory() throws Exception {
        OrderPojo existing = orderWithItems(itemWithProductId("ID1", 3, 10.0));
        existing.setStatus(OrderStatus.UNFULFILLABLE.name());
        existing.setOrderReferenceId("ORD-1");
        existing.setCreatedAt(ZonedDateTime.now().minusMinutes(10));

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(existing);

        when(orderApi.updateOrder(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderPojo cancelled = orderFlow.cancel("ORD-1");

        assertEquals(OrderStatus.CANCELLED.name(), cancelled.getStatus());
        verify(inventoryApi, never()).incrementInventoryBulk(anyMap());
        verify(orderApi).updateOrder(any());
    }

    // ---------------- helpers ----------------

    private static OrderItemPojo itemWithProductId(String productId, int qty, double price) {
        OrderItemPojo i = new OrderItemPojo();
        i.setProductId(productId);
        i.setOrderedQuantity(qty);
        i.setSellingPrice(price);
        return i;
    }

    private static OrderPojo orderWithItems(OrderItemPojo... items) {
        OrderPojo o = new OrderPojo();
        o.setOrderItems(List.of(items));
        return o;
    }

    private static ProductPojo product(String id, String barcode, double mrp) {
        ProductPojo p = new ProductPojo();
        p.setId(id);
        p.setBarcode(barcode);
        p.setMrp(mrp);
        return p;
    }
}
