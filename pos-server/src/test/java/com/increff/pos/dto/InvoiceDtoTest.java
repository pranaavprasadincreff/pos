package com.increff.pos.dto;

import com.increff.pos.flow.InvoiceFlow;
import com.increff.pos.model.data.InvoiceData;
import com.increff.pos.model.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceDtoTest {

    @Mock
    private InvoiceFlow invoiceFlow;

    @InjectMocks
    private InvoiceDto invoiceDto;

    @Test
    void generateInvoice_normalizesReferenceId_andDelegates() throws Exception {
        InvoiceData data = new InvoiceData();
        data.setOrderReferenceId("ORD-1");

        when(invoiceFlow.generateInvoice("ORD-1")).thenReturn(data);

        InvoiceData result = invoiceDto.generateInvoice(" ord-1 ");

        assertEquals("ORD-1", result.getOrderReferenceId());
        verify(invoiceFlow).generateInvoice("ORD-1");
    }

    @Test
    void getInvoice_normalizesReferenceId_andDelegates() throws Exception {
        InvoiceData data = new InvoiceData();
        data.setOrderReferenceId("ORD-99");

        when(invoiceFlow.getInvoice("ORD-99")).thenReturn(data);

        InvoiceData result = invoiceDto.getInvoice(" ord-99 ");

        assertEquals("ORD-99", result.getOrderReferenceId());
        verify(invoiceFlow).getInvoice("ORD-99");
    }

    @Test
    void generateInvoice_blankReference_throws_andDoesNotCallFlow() {
        ApiException ex = assertThrows(ApiException.class, () -> invoiceDto.generateInvoice("   "));
        assertTrue(ex.getMessage().toLowerCase().contains("cannot be empty"));

        verifyNoInteractions(invoiceFlow);
    }
}
