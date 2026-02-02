package com.increff.pos.flow;

import com.increff.pos.api.ProductApi;
import com.increff.pos.client.InvoiceClient;
import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.InvoiceGenerateForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
        verifyNoInteractions(productApi);
    }

    @Test
    void generateInvoice_whenNotFulfillable_throws() throws Exception {
        OrderPojo order = sampleOrder();
        order.setStatus(OrderStatus.UNFULFILLABLE.name());
        order.setOrderReferenceId("ORD-1");

        when(orderFlow.getByRef("ORD-1")).thenReturn(order);

        ApiException ex = assertThrows(ApiException.class, () -> invoiceFlow.generateInvoice("ORD-1"));
        assertTrue(ex.getMessage().toLowerCase().contains("only fulfillable"));

        verify(invoiceClient, never()).getInvoice(anyString());
        verify(invoiceClient, never()).generateInvoice(any());
        verify(orderFlow, never()).markInvoiced(anyString());
        verifyNoInteractions(productApi);
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
        when(invoiceClient.generateInvoice(any(InvoiceGenerateForm.class))).thenReturn(data);

        InvoiceData result = invoiceFlow.generateInvoice("ORD-1");

        assertEquals("ORD-1", result.getOrderReferenceId());

        ArgumentCaptor<InvoiceGenerateForm> cap = ArgumentCaptor.forClass(InvoiceGenerateForm.class);
        verify(invoiceClient).generateInvoice(cap.capture());

        InvoiceGenerateForm sent = cap.getValue();
        assertEquals("ORD-1", sent.getOrderReferenceId());
        assertEquals(1, sent.getItems().size());
        assertEquals("P1", sent.getItems().get(0).getBarcode());
        assertEquals("Product 1", sent.getItems().get(0).getProductName());
        assertEquals(2, sent.getItems().get(0).getQuantity());
        assertEquals(99.0, sent.getItems().get(0).getSellingPrice());

        verify(orderFlow).markInvoiced("ORD-1");
    }

    @Test
    void generateInvoice_missingProduct_throws_andDoesNotMarkInvoiced() throws Exception {
        OrderPojo order = sampleOrder();
        order.setStatus(OrderStatus.FULFILLABLE.name());
        order.setOrderReferenceId("ORD-1");

        when(orderFlow.getByRef("ORD-1")).thenReturn(order);
        when(productApi.findByBarcodes(List.of("P1"))).thenReturn(List.of()); // missing

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

    @Test
    void getInvoice_delegatesToClient() throws Exception {
        InvoiceData data = new InvoiceData();
        data.setOrderReferenceId("ORD-9");

        when(invoiceClient.getInvoice("ORD-9")).thenReturn(data);

        InvoiceData out = invoiceFlow.getInvoice("ORD-9");
        assertEquals("ORD-9", out.getOrderReferenceId());
        verify(invoiceClient).getInvoice("ORD-9");
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
