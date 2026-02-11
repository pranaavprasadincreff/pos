package com.increff.pos.flow;

import com.increff.pos.api.OrderApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.db.subdocument.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InvoiceGenerateForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceFlowTest {

    @Mock
    private OrderApi orderApi;

    @Mock
    private ProductApi productApi;

    @InjectMocks
    private InvoiceFlow invoiceFlow;

    @Test
    void invoiceAlreadyExists_whenOrderIsInvoiced_returnsTrue() throws Exception {
        OrderPojo order = sampleOrder();
        order.setOrderReferenceId("ORD-1");
        order.setStatus(OrderStatus.INVOICED.name());

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(order);

        boolean exists = invoiceFlow.invoiceAlreadyExists("ORD-1");

        assertTrue(exists);
        verify(orderApi).getByOrderReferenceId("ORD-1");
        verifyNoInteractions(productApi);
    }

    @Test
    void invoiceAlreadyExists_whenOrderNotInvoiced_returnsFalse() throws Exception {
        OrderPojo order = sampleOrder();
        order.setOrderReferenceId("ORD-1");
        order.setStatus(OrderStatus.FULFILLABLE.name());

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(order);

        boolean exists = invoiceFlow.invoiceAlreadyExists("ORD-1");

        assertFalse(exists);
        verify(orderApi).getByOrderReferenceId("ORD-1");
        verifyNoInteractions(productApi);
    }

    @Test
    void prepareInvoiceRequest_whenAlreadyInvoiced_throws() throws Exception {
        OrderPojo order = sampleOrder();
        order.setOrderReferenceId("ORD-1");
        order.setStatus(OrderStatus.INVOICED.name());

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(order);

        ApiException ex = assertThrows(ApiException.class, () -> invoiceFlow.prepareInvoiceRequest("ORD-1"));
        assertTrue(ex.getMessage().toLowerCase().contains("already exists"));

        verify(orderApi).getByOrderReferenceId("ORD-1");
        verifyNoInteractions(productApi);
    }

    @Test
    void prepareInvoiceRequest_whenNotFulfillable_throws() throws Exception {
        OrderPojo order = sampleOrder();
        order.setOrderReferenceId("ORD-1");
        order.setStatus(OrderStatus.UNFULFILLABLE.name());

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(order);

        ApiException ex = assertThrows(ApiException.class, () -> invoiceFlow.prepareInvoiceRequest("ORD-1"));
        assertTrue(ex.getMessage().toLowerCase().contains("only fulfillable"));

        verify(orderApi).getByOrderReferenceId("ORD-1");
        verifyNoInteractions(productApi);
    }

    @Test
    void prepareInvoiceRequest_happyPath_buildsInvoiceGenerateForm_usingProductId() throws Exception {
        OrderPojo order = sampleOrder();
        order.setOrderReferenceId("ORD-1");
        order.setStatus(OrderStatus.FULFILLABLE.name());

        ProductPojo p = new ProductPojo();
        p.setId("PID-1");
        p.setBarcode("B1");
        p.setName("Product 1");

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(order);
        when(productApi.findByIds(List.of("PID-1"))).thenReturn(List.of(p));

        InvoiceGenerateForm form = invoiceFlow.prepareInvoiceRequest("ORD-1");

        assertEquals("ORD-1", form.getOrderReferenceId());
        assertEquals(1, form.getItems().size());
        assertEquals("B1", form.getItems().get(0).getBarcode());
        assertEquals("Product 1", form.getItems().get(0).getProductName());
        assertEquals(2, form.getItems().get(0).getQuantity());
        assertEquals(99.0, form.getItems().get(0).getSellingPrice());

        verify(orderApi).getByOrderReferenceId("ORD-1");
        verify(productApi).findByIds(List.of("PID-1"));
    }

    @Test
    void prepareInvoiceRequest_missingProduct_throws() throws Exception {
        OrderPojo order = sampleOrder();
        order.setOrderReferenceId("ORD-1");
        order.setStatus(OrderStatus.FULFILLABLE.name());

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(order);
        when(productApi.findByIds(List.of("PID-1"))).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () -> invoiceFlow.prepareInvoiceRequest("ORD-1"));
        assertTrue(ex.getMessage().toLowerCase().contains("missing product data"));

        verify(orderApi).getByOrderReferenceId("ORD-1");
        verify(productApi).findByIds(List.of("PID-1"));
    }

    @Test
    void markOrderInvoiced_happyPath_setsStatusAndUpdates() throws Exception {
        OrderPojo order = sampleOrder();
        order.setOrderReferenceId("ORD-1");
        order.setStatus(OrderStatus.FULFILLABLE.name());

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(order);

        invoiceFlow.markOrderInvoiced("ORD-1");

        ArgumentCaptor<OrderPojo> captor = ArgumentCaptor.forClass(OrderPojo.class);
        verify(orderApi).updateOrder(captor.capture());

        OrderPojo updated = captor.getValue();
        assertEquals(OrderStatus.INVOICED.name(), updated.getStatus());
    }

    @Test
    void markOrderInvoiced_whenAlreadyInvoiced_throws() throws Exception {
        OrderPojo order = sampleOrder();
        order.setOrderReferenceId("ORD-1");
        order.setStatus(OrderStatus.INVOICED.name());

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(order);

        ApiException ex = assertThrows(ApiException.class, () -> invoiceFlow.markOrderInvoiced("ORD-1"));
        assertTrue(ex.getMessage().toLowerCase().contains("already invoiced"));

        verify(orderApi, never()).updateOrder(any());
    }

    @Test
    void markOrderInvoiced_whenNotFulfillable_throws() throws Exception {
        OrderPojo order = sampleOrder();
        order.setOrderReferenceId("ORD-1");
        order.setStatus(OrderStatus.UNFULFILLABLE.name());

        when(orderApi.getByOrderReferenceId("ORD-1")).thenReturn(order);

        ApiException ex = assertThrows(ApiException.class, () -> invoiceFlow.markOrderInvoiced("ORD-1"));
        assertTrue(ex.getMessage().toLowerCase().contains("only fulfillable"));

        verify(orderApi, never()).updateOrder(any());
    }

    private OrderPojo sampleOrder() {
        OrderItemPojo item = new OrderItemPojo();
        item.setProductId("PID-1");
        item.setOrderedQuantity(2);
        item.setSellingPrice(99.0);

        OrderPojo order = new OrderPojo();
        order.setOrderItems(List.of(item));
        return order;
    }
}
