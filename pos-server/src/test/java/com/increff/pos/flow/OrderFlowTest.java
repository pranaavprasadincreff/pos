package com.increff.pos.flow;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.OrderApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
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
    void create_happyPath_setsFulfillable_callsBulkDeduct_andPersists() throws Exception {
        OrderPojo order = orderWithItems(
                item(2, 90.0),
                item(3, 90.0)
        );
        List<String> barcodesByIndex = List.of("B1", "B1");

        when(orderApi.orderReferenceIdExists(anyString())).thenReturn(false);

        ProductPojo p1 = product("ID1", "B1", 100.0);
        when(productApi.findByBarcodes(eq(List.of("B1")))).thenReturn(List.of(p1));

        doNothing().when(inventoryApi).deductInventoryBulk(anyMap());
        when(orderApi.createOrder(any(OrderPojo.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPojo saved = orderFlow.create(order, barcodesByIndex);

        assertNotNull(saved.getOrderReferenceId());
        assertTrue(saved.getOrderReferenceId().startsWith("ORD-"));
        assertNotNull(saved.getOrderTime());
        assertEquals(OrderStatus.FULFILLABLE.name(), saved.getStatus());

        // productId should be set on both items
        assertEquals("ID1", saved.getOrderItems().get(0).getProductId());
        assertEquals("ID1", saved.getOrderItems().get(1).getProductId());

        ArgumentCaptor<Map<String, Integer>> bulkCaptor = ArgumentCaptor.forClass(Map.class);
        verify(inventoryApi, times(1)).deductInventoryBulk(bulkCaptor.capture());
        assertEquals(Map.of("ID1", 5), bulkCaptor.getValue());

        verify(productApi, times(1)).findByBarcodes(eq(List.of("B1")));
        verify(orderApi, times(1)).createOrder(any(OrderPojo.class));
    }

    @Test
    void create_insufficientInventory_bulkDeductThrows_andDoesNotPersist() throws Exception {
        OrderPojo order = orderWithItems(item(6, 90.0));
        List<String> barcodesByIndex = List.of("B1");

        when(orderApi.orderReferenceIdExists(anyString())).thenReturn(false);

        ProductPojo p1 = product("ID1", "B1", 100.0);
        when(productApi.findByBarcodes(eq(List.of("B1")))).thenReturn(List.of(p1));

        doThrow(new ApiException("Insufficient inventory"))
                .when(inventoryApi).deductInventoryBulk(eq(Map.of("ID1", 6)));

        ApiException ex = assertThrows(ApiException.class, () -> orderFlow.create(order, barcodesByIndex));
        assertTrue(ex.getMessage().toLowerCase().contains("insufficient"));

        verify(orderApi, never()).createOrder(any());
    }

    @Test
    void create_sellingPriceExceedsMrp_throws_andDoesNotTouchInventoryOrPersist() throws Exception {
        OrderPojo order = orderWithItems(item(1, 150.0));
        List<String> barcodesByIndex = List.of("B1");

        when(orderApi.orderReferenceIdExists(anyString())).thenReturn(false);

        ProductPojo p1 = product("ID1", "B1", 100.0);
        when(productApi.findByBarcodes(eq(List.of("B1")))).thenReturn(List.of(p1));

        ApiException ex = assertThrows(ApiException.class, () -> orderFlow.create(order, barcodesByIndex));
        assertTrue(ex.getMessage().contains("Selling price cannot exceed MRP"));
        assertTrue(ex.getMessage().contains("B1"));

        verify(inventoryApi, never()).deductInventoryBulk(anyMap());
        verify(orderApi, never()).createOrder(any());
    }

    @Test
    void create_missingProduct_throws_withBarcodeList() throws Exception {
        OrderPojo order = orderWithItems(item(1, 10.0));
        List<String> barcodesByIndex = List.of("B_MISSING");

        when(orderApi.orderReferenceIdExists(anyString())).thenReturn(false);
        when(productApi.findByBarcodes(eq(List.of("B_MISSING")))).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () -> orderFlow.create(order, barcodesByIndex));
        assertTrue(ex.getMessage().contains("Products not found for barcodes"));
        assertTrue(ex.getMessage().contains("B_MISSING"));

        verify(inventoryApi, never()).deductInventoryBulk(anyMap());
        verify(orderApi, never()).createOrder(any());
    }

    @Test
    void create_itemsAndBarcodeListSizeMismatch_throws() throws Exception {
        OrderPojo order = orderWithItems(item(1, 10.0), item(1, 10.0));
        List<String> barcodesByIndex = List.of("B1"); // mismatch

        when(orderApi.orderReferenceIdExists(anyString())).thenReturn(false);

        ProductPojo p1 = product("ID1", "B1", 100.0);
        when(productApi.findByBarcodes(eq(List.of("B1")))).thenReturn(List.of(p1));

        ApiException ex = assertThrows(ApiException.class, () -> orderFlow.create(order, barcodesByIndex));
        assertTrue(ex.getMessage().toLowerCase().contains("size mismatch"));

        verify(productApi, times(1)).findByBarcodes(eq(List.of("B1")));
        verifyNoInteractions(inventoryApi);
        verify(orderApi, never()).createOrder(any());
    }

    // ---------------- CANCEL ----------------

    @Test
    void cancel_whenFulfillable_restoresBulk_andSetsCancelled() throws Exception {
        OrderPojo existing = orderWithItems(itemWithProductId("ID1", 2, 10.0), itemWithProductId("ID1", 1, 10.0));
        existing.setStatus(OrderStatus.FULFILLABLE.name());
        existing.setOrderReferenceId("ORD-1");

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(existing);
        doNothing().when(inventoryApi).incrementInventoryBulk(eq(Map.of("ID1", 3)));
        when(orderApi.updateOrder(any(OrderPojo.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPojo cancelled = orderFlow.cancel("ORD-1");

        assertEquals(OrderStatus.CANCELLED.name(), cancelled.getStatus());
        verify(inventoryApi, times(1)).incrementInventoryBulk(eq(Map.of("ID1", 3)));
        verify(orderApi, times(1)).updateOrder(any(OrderPojo.class));
        verifyNoInteractions(productApi);
    }

    @Test
    void cancel_whenUnfulfillable_doesNotRestoreInventory() throws Exception {
        OrderPojo existing = orderWithItems(itemWithProductId("ID1", 2, 10.0));
        existing.setStatus(OrderStatus.UNFULFILLABLE.name());
        existing.setOrderReferenceId("ORD-1");

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(existing);
        when(orderApi.updateOrder(any(OrderPojo.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPojo cancelled = orderFlow.cancel("ORD-1");

        assertEquals(OrderStatus.CANCELLED.name(), cancelled.getStatus());
        verify(inventoryApi, never()).incrementInventoryBulk(anyMap());
        verify(orderApi).updateOrder(any(OrderPojo.class));
        verifyNoInteractions(productApi);
    }

    // ---------------- UPDATE ----------------

    @Test
    void update_whenNotEditable_throws() throws Exception {
        OrderPojo existing = orderWithItems(itemWithProductId("ID1", 1, 10.0));
        existing.setStatus(OrderStatus.CANCELLED.name());
        existing.setOrderReferenceId("ORD-1");

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(existing);

        OrderPojo updateRequest = orderWithItems(item(1, 10.0));
        List<String> barcodesByIndex = List.of("B1");

        ApiException ex = assertThrows(ApiException.class, () -> orderFlow.update("ORD-1", updateRequest, barcodesByIndex));
        assertTrue(ex.getMessage().toLowerCase().contains("cannot be modified"));

        verifyNoInteractions(productApi);
        verify(inventoryApi, never()).incrementInventoryBulk(anyMap());
        verify(inventoryApi, never()).deductInventoryBulk(anyMap());
        verify(orderApi, never()).updateOrder(any());
    }

    @Test
    void update_restoresOldIfFulfillable_thenBulkDeductsNew_andPersists() throws Exception {
        OrderPojo existing = orderWithItems(itemWithProductId("ID1", 2, 10.0));
        existing.setStatus(OrderStatus.FULFILLABLE.name());
        existing.setOrderReferenceId("ORD-1");

        OrderPojo updateRequest = orderWithItems(item(5, 10.0));
        List<String> barcodesByIndex = List.of("B1");

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(existing);

        ProductPojo p1 = product("ID1", "B1", 100.0);
        when(productApi.findByBarcodes(eq(List.of("B1")))).thenReturn(List.of(p1));

        doNothing().when(inventoryApi).incrementInventoryBulk(eq(Map.of("ID1", 2)));
        doNothing().when(inventoryApi).deductInventoryBulk(eq(Map.of("ID1", 5)));

        when(orderApi.updateOrder(any(OrderPojo.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderPojo out = orderFlow.update("ORD-1", updateRequest, barcodesByIndex);

        assertEquals(OrderStatus.FULFILLABLE.name(), out.getStatus());
        assertEquals(1, out.getOrderItems().size());
        assertEquals(5, out.getOrderItems().get(0).getOrderedQuantity());
        assertEquals("ID1", out.getOrderItems().get(0).getProductId());

        InOrder inOrder = inOrder(inventoryApi, orderApi);
        inOrder.verify(inventoryApi).incrementInventoryBulk(eq(Map.of("ID1", 2)));
        inOrder.verify(inventoryApi).deductInventoryBulk(eq(Map.of("ID1", 5)));
        inOrder.verify(orderApi).updateOrder(any(OrderPojo.class));
    }

    // ---------------- MARK INVOICED ----------------

    @Test
    void markInvoiced_throws_whenAlreadyInvoiced() throws Exception {
        OrderPojo existing = orderWithItems(itemWithProductId("ID1", 1, 10.0));
        existing.setStatus(OrderStatus.INVOICED.name());

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(existing);

        ApiException ex = assertThrows(ApiException.class, () -> orderFlow.markInvoiced("ORD-1"));
        assertTrue(ex.getMessage().toLowerCase().contains("already invoiced"));

        verify(orderApi, never()).updateOrder(any());
    }

    @Test
    void markInvoiced_throws_whenUnfulfillable() throws Exception {
        OrderPojo existing = orderWithItems(itemWithProductId("ID1", 1, 10.0));
        existing.setStatus(OrderStatus.UNFULFILLABLE.name());

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(existing);

        ApiException ex = assertThrows(ApiException.class, () -> orderFlow.markInvoiced("ORD-1"));
        assertTrue(ex.getMessage().toLowerCase().contains("only fulfillable"));

        verify(orderApi, never()).updateOrder(any());
    }

    @Test
    void markInvoiced_happyPath_setsStatusAndUpdates() throws Exception {
        OrderPojo existing = orderWithItems(itemWithProductId("ID1", 1, 10.0));
        existing.setStatus(OrderStatus.FULFILLABLE.name());

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(existing);
        when(orderApi.updateOrder(any(OrderPojo.class))).thenAnswer(inv -> inv.getArgument(0));

        orderFlow.markInvoiced("ORD-1");

        assertEquals(OrderStatus.INVOICED.name(), existing.getStatus());
        verify(orderApi).updateOrder(existing);
    }

    // ---------------- helpers ----------------

    private static OrderItemPojo item(int qty, double sellingPrice) {
        OrderItemPojo i = new OrderItemPojo();
        i.setOrderedQuantity(qty);
        i.setSellingPrice(sellingPrice);
        return i;
    }

    private static OrderItemPojo itemWithProductId(String productId, int qty, double sellingPrice) {
        OrderItemPojo i = new OrderItemPojo();
        i.setProductId(productId);
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
}
