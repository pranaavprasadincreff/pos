package com.increff.pos.flow;

import com.increff.pos.api.ProductApi;
import com.increff.pos.client.InvoiceClient;
import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.data.InvoiceData;
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
class InvoiceFlowTest {

    @Mock private InvoiceClient invoiceClient;
    @Mock private OrderFlow orderFlow;
    @Mock private ProductApi productApi;

    @InjectMocks private InvoiceFlow invoiceFlow;

    @Test
    void generateInvoice_whenAlreadyInvoiced_returnsExistingInvoice() throws Exception {
        OrderPojo order = sampleOrder();
        order.setStatus(OrderStatus.INVOICED.name());
        order.setOrderReferenceId("ORD-1");

        InvoiceData existing = new InvoiceData();
        existing.setOrderReferenceId("ORD-1");

        when(orderFlow.getByRef("ORD-1")).thenReturn(order);
        when(invoiceClient.getInvoice("ORD-1")).thenReturn(existing);

        InvoiceData result = invoiceFlow.generateInvoice("ORD-1");

        assertEquals("ORD-1", result.getOrderReferenceId());
        verify(invoiceClient).getInvoice("ORD-1");
        verify(invoiceClient, never()).generateInvoice(any());
        verify(orderFlow, never()).markInvoiced(anyString());
    }

    @Test
    void generateInvoice_whenUnfulfillable_throws() throws Exception {
        OrderPojo order = sampleOrder();
        order.setStatus(OrderStatus.UNFULFILLABLE.name());
        order.setOrderReferenceId("ORD-1");

        when(orderFlow.getByRef("ORD-1")).thenReturn(order);

        ApiException ex = assertThrows(ApiException.class, () -> invoiceFlow.generateInvoice("ORD-1"));
        assertTrue(ex.getMessage().toLowerCase().contains("only fulfillable"));

        verify(invoiceClient, never()).generateInvoice(any());
        verify(orderFlow, never()).markInvoiced(anyString());
    }

    @Test
    void generateInvoice_happyPath_callsService_thenMarksInvoiced() throws Exception {
        OrderPojo order = sampleOrder();
        order.setStatus(OrderStatus.FULFILLABLE.name());
        order.setOrderReferenceId("ORD-1");

        ProductPojo p = new ProductPojo();
        p.setBarcode("P1");
        p.setName("Product 1");

        InvoiceData data = new InvoiceData();
        data.setOrderReferenceId("ORD-1");

        when(orderFlow.getByRef("ORD-1")).thenReturn(order);
        when(productApi.findByBarcodes(List.of("P1"))).thenReturn(List.of(p));
        when(invoiceClient.generateInvoice(any())).thenReturn(data);

        InvoiceData result = invoiceFlow.generateInvoice("ORD-1");

        assertEquals("ORD-1", result.getOrderReferenceId());
        verify(invoiceClient).generateInvoice(any());
        verify(orderFlow).markInvoiced("ORD-1");
    }

    @Test
    void generateInvoice_missingProduct_throws_andDoesNotMarkInvoiced() throws Exception {
        OrderPojo order = sampleOrder();
        order.setStatus(OrderStatus.FULFILLABLE.name());
        order.setOrderReferenceId("ORD-1");

        when(orderFlow.getByRef("ORD-1")).thenReturn(order);
        when(productApi.findByBarcodes(List.of("P1"))).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () -> invoiceFlow.generateInvoice("ORD-1"));
        assertTrue(ex.getMessage().toLowerCase().contains("missing product data"));

        verify(invoiceClient, never()).generateInvoice(any());
        verify(orderFlow, never()).markInvoiced(anyString());
    }

    @Test
    void generateInvoice_invoiceServiceFails_doesNotMarkInvoiced() throws Exception {
        OrderPojo order = sampleOrder();
        order.setStatus(OrderStatus.FULFILLABLE.name());
        order.setOrderReferenceId("ORD-1");

        ProductPojo p = new ProductPojo();
        p.setBarcode("P1");
        p.setName("Product 1");

        when(orderFlow.getByRef("ORD-1")).thenReturn(order);
        when(productApi.findByBarcodes(List.of("P1"))).thenReturn(List.of(p));
        when(invoiceClient.generateInvoice(any())).thenThrow(new ApiException("down"));

        assertThrows(ApiException.class, () -> invoiceFlow.generateInvoice("ORD-1"));

        verify(orderFlow, never()).markInvoiced(anyString());
    }

    private OrderPojo sampleOrder() {
        OrderItemPojo item = new OrderItemPojo();
        item.setProductBarcode("P1");
        item.setOrderedQuantity(2);
        item.setSellingPrice(99.0);

        OrderPojo order = new OrderPojo();
        order.setOrderItems(List.of(item));
        return order;
    }
}
