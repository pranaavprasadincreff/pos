package com.increff.pos.flow;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.InventoryPojo;
import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderFlowTest {

    @Mock private OrderApi orderApi;
    @Mock private InventoryApi inventoryApi;
    @Mock private ProductApi productApi;

    @InjectMocks private OrderFlow orderFlow;

    @Test
    void create_happyPath_setsFulfillable_deductsAggregatedQty_andPersists() throws Exception {
        // two lines same barcode -> should aggregate to one deduct call
        OrderPojo order = orderWithItems(
                item("B1", 2, 90.0),
                item("B1", 3, 90.0)
        );

        when(orderApi.orderReferenceIdExists(anyString())).thenReturn(false);

        // product lookup
        ProductPojo p1 = product("ID1", "B1", 100.0);
        when(productApi.findByBarcodes(eq(List.of("B1")))).thenReturn(List.of(p1));

        // inventory lookup
        InventoryPojo inv1 = inventory("ID1", 10);
        when(inventoryApi.getByProductIds(eq(List.of("ID1")))).thenReturn(List.of(inv1));

        // deduct succeeds
        doNothing().when(inventoryApi).deductInventory("ID1", 5);

        when(orderApi.createOrder(any(OrderPojo.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPojo saved = orderFlow.create(order);

        assertNotNull(saved.getOrderReferenceId(), "orderReferenceId should be generated");
        assertNotNull(saved.getOrderTime(), "orderTime should be set");
        assertEquals(OrderStatus.FULFILLABLE.name(), saved.getStatus());

        // bulk calls once
        verify(productApi, times(1)).findByBarcodes(eq(List.of("B1")));
        verify(inventoryApi, times(1)).getByProductIds(eq(List.of("ID1")));

        // aggregated deduct (2+3)
        verify(inventoryApi, times(1)).deductInventory("ID1", 5);

        verify(orderApi, times(1)).createOrder(any(OrderPojo.class));
        verifyNoMoreInteractions(orderApi);
    }

    @Test
    void create_edge_insufficientInventory_setsUnfulfillable_andDoesNotDeduct() throws Exception {
        OrderPojo order = orderWithItems(item("B1", 6, 90.0));

        when(orderApi.orderReferenceIdExists(anyString())).thenReturn(false);

        ProductPojo p1 = product("ID1", "B1", 100.0);
        when(productApi.findByBarcodes(eq(List.of("B1")))).thenReturn(List.of(p1));

        InventoryPojo inv1 = inventory("ID1", 5);
        when(inventoryApi.getByProductIds(eq(List.of("ID1")))).thenReturn(List.of(inv1));

        when(orderApi.createOrder(any(OrderPojo.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPojo saved = orderFlow.create(order);

        assertEquals(OrderStatus.UNFULFILLABLE.name(), saved.getStatus());

        verify(inventoryApi, never()).deductInventory(anyString(), anyInt());
        verify(orderApi, times(1)).createOrder(any(OrderPojo.class));
    }

    @Test
    void create_edge_missingInventoryTreatsAsZero_setsUnfulfillable() throws Exception {
        OrderPojo order = orderWithItems(item("B1", 1, 90.0));

        when(orderApi.orderReferenceIdExists(anyString())).thenReturn(false);

        ProductPojo p1 = product("ID1", "B1", 100.0);
        when(productApi.findByBarcodes(eq(List.of("B1")))).thenReturn(List.of(p1));

        // inventory list does not include ID1 => treated as 0 available
        when(inventoryApi.getByProductIds(eq(List.of("ID1")))).thenReturn(List.of());

        when(orderApi.createOrder(any(OrderPojo.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPojo saved = orderFlow.create(order);

        assertEquals(OrderStatus.UNFULFILLABLE.name(), saved.getStatus());
        verify(inventoryApi, never()).deductInventory(anyString(), anyInt());
        verify(orderApi).createOrder(any(OrderPojo.class));
    }

    @Test
    void create_edge_sellingPriceExceedsMrp_throws_andDoesNotTouchInventoryOrPersist() throws Exception {
        OrderPojo order = orderWithItems(item("B1", 1, 150.0)); // selling > mrp

        when(orderApi.orderReferenceIdExists(anyString())).thenReturn(false);

        ProductPojo p1 = product("ID1", "B1", 100.0);
        when(productApi.findByBarcodes(eq(List.of("B1")))).thenReturn(List.of(p1));

        ApiException ex = assertThrows(ApiException.class, () -> orderFlow.create(order));
        assertTrue(ex.getMessage().contains("Selling price cannot exceed MRP"), ex.getMessage());
        assertTrue(ex.getMessage().contains("B1"), "message should contain barcode");

        verify(inventoryApi, never()).getByProductIds(anyList());
        verify(inventoryApi, never()).deductInventory(anyString(), anyInt());
        verify(orderApi, never()).createOrder(any());
    }

    @Test
    void create_edge_missingProduct_throws_withBarcodeList() throws Exception {
        OrderPojo order = orderWithItems(item("B_MISSING", 1, 10.0));

        when(orderApi.orderReferenceIdExists(anyString())).thenReturn(false);

        // returns empty => missing
        when(productApi.findByBarcodes(eq(List.of("B_MISSING")))).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () -> orderFlow.create(order));
        assertTrue(ex.getMessage().contains("Products not found for barcodes"), ex.getMessage());
        assertTrue(ex.getMessage().contains("B_MISSING"), ex.getMessage());

        verify(inventoryApi, never()).getByProductIds(anyList());
        verify(orderApi, never()).createOrder(any());
    }

    @Test
    void cancel_whenFulfillable_restoresAggregatedQty_andSetsCancelled() throws Exception {
        OrderPojo existing = orderWithItems(item("B1", 2, 10.0), item("B1", 1, 10.0));
        existing.setStatus(OrderStatus.FULFILLABLE.name());
        existing.setOrderReferenceId("ORD-1");

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(existing);

        ProductPojo p1 = product("ID1", "B1", 100.0);
        when(productApi.findByBarcodes(eq(List.of("B1")))).thenReturn(List.of(p1));

        InventoryPojo inv1 = inventory("ID1", 10);
        when(inventoryApi.getByProductIds(eq(List.of("ID1")))).thenReturn(List.of(inv1));

        doNothing().when(inventoryApi).incrementInventory("ID1", 3);
        when(orderApi.updateOrder(any(OrderPojo.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPojo cancelled = orderFlow.cancel("ORD-1");

        assertEquals(OrderStatus.CANCELLED.name(), cancelled.getStatus());
        verify(inventoryApi, times(1)).incrementInventory("ID1", 3);
        verify(orderApi, times(1)).updateOrder(any(OrderPojo.class));
    }

    @Test
    void cancel_whenUnfulfillable_doesNotRestoreInventory() throws Exception {
        OrderPojo existing = orderWithItems(item("B1", 2, 10.0));
        existing.setStatus(OrderStatus.UNFULFILLABLE.name());
        existing.setOrderReferenceId("ORD-1");

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(existing);
        when(orderApi.updateOrder(any(OrderPojo.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPojo cancelled = orderFlow.cancel("ORD-1");

        assertEquals(OrderStatus.CANCELLED.name(), cancelled.getStatus());
        verifyNoInteractions(productApi);
        verify(inventoryApi, never()).incrementInventory(anyString(), anyInt());
    }

    @Test
    void markInvoiced_throws_whenOrderAlreadyInvoiced() throws Exception {
        OrderPojo existing = orderWithItems(item("B1", 1, 10.0));
        existing.setStatus(OrderStatus.INVOICED.name());

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(existing);

        ApiException ex = assertThrows(ApiException.class, () -> orderFlow.markInvoiced("ORD-1"));
        assertTrue(ex.getMessage().toLowerCase().contains("already invoiced"));

        verify(orderApi, never()).updateOrder(any());
    }

    @Test
    void update_whenOrderNotEditable_throws_andDoesNotCallInventory() throws Exception {
        OrderPojo existing = orderWithItems(item("B1", 1, 10.0));
        existing.setStatus(OrderStatus.CANCELLED.name());

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(existing);

        OrderPojo updated = orderWithItems(item("B1", 1, 10.0));

        ApiException ex = assertThrows(ApiException.class, () -> orderFlow.update("ORD-1", updated));
        assertTrue(ex.getMessage().toLowerCase().contains("cannot be modified"));

        verifyNoInteractions(productApi);
        verify(inventoryApi, never()).incrementInventory(anyString(), anyInt());
        verify(inventoryApi, never()).deductInventory(anyString(), anyInt());
        verify(orderApi, never()).updateOrder(any());
    }

    // ------------------------ helpers ------------------------

    private static OrderItemPojo item(String barcode, int qty, double sellingPrice) {
        OrderItemPojo i = new OrderItemPojo();
        i.setProductBarcode(barcode);
        i.setOrderedQuantity(qty);
        i.setSellingPrice(sellingPrice);
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

    private static InventoryPojo inventory(String productId, Integer qty) {
        InventoryPojo inv = new InventoryPojo();
        inv.setProductId(productId);
        inv.setQuantity(qty);
        return inv;
    }
}
