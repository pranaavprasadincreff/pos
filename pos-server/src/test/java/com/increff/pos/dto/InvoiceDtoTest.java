package com.increff.pos.dto;

import com.increff.pos.flow.InvoiceFlow;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.form.InvoiceGenerateForm;
import com.increff.pos.wrapper.InvoiceClientWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceDtoTest {

    @Mock
    private InvoiceFlow invoiceFlow;

    @Mock
    private InvoiceClientWrapper invoiceClientWrapper;

    @InjectMocks
    private InvoiceDto invoiceDto;

    @Test
    void generateInvoice_whenInvoiceAlreadyExists_normalizesAndFetchesExistingInvoice() throws Exception {
        when(invoiceFlow.invoiceAlreadyExists("ORD-1")).thenReturn(true);

        InvoiceData existing = new InvoiceData();
        existing.setOrderReferenceId("ORD-1");
        when(invoiceClientWrapper.getInvoice("ORD-1")).thenReturn(existing);

        InvoiceData result = invoiceDto.generateInvoice(" ord-1 ");

        assertEquals("ORD-1", result.getOrderReferenceId());

        verify(invoiceFlow).invoiceAlreadyExists("ORD-1");
        verify(invoiceClientWrapper).getInvoice("ORD-1");

        verify(invoiceFlow, never()).prepareInvoiceRequest(any());
        verify(invoiceClientWrapper, never()).generateInvoice(any());
        verify(invoiceFlow, never()).markOrderInvoiced(any());
    }

    @Test
    void generateInvoice_whenInvoiceDoesNotExist_generatesInvoice_andMarksOrderInvoiced() throws Exception {
        when(invoiceFlow.invoiceAlreadyExists("ORD-1")).thenReturn(false);

        InvoiceGenerateForm request = new InvoiceGenerateForm();
        request.setOrderReferenceId("ORD-1");
        when(invoiceFlow.prepareInvoiceRequest("ORD-1")).thenReturn(request);

        InvoiceData generated = new InvoiceData();
        generated.setOrderReferenceId("ORD-1");
        when(invoiceClientWrapper.generateInvoice(request)).thenReturn(generated);

        InvoiceData result = invoiceDto.generateInvoice(" ord-1 ");

        assertEquals("ORD-1", result.getOrderReferenceId());

        verify(invoiceFlow).invoiceAlreadyExists("ORD-1");
        verify(invoiceFlow).prepareInvoiceRequest("ORD-1");
        verify(invoiceClientWrapper).generateInvoice(request);
        verify(invoiceFlow).markOrderInvoiced("ORD-1");

        verify(invoiceClientWrapper, never()).getInvoice(any());
    }

    @Test
    void getInvoice_normalizesReferenceId_andDelegatesToWrapper() throws Exception {
        InvoiceData data = new InvoiceData();
        data.setOrderReferenceId("ORD-99");

        when(invoiceClientWrapper.getInvoice("ORD-99")).thenReturn(data);

        InvoiceData result = invoiceDto.getInvoice(" ord-99 ");

        assertEquals("ORD-99", result.getOrderReferenceId());
        verify(invoiceClientWrapper).getInvoice("ORD-99");
        verifyNoInteractions(invoiceFlow);
    }
}
