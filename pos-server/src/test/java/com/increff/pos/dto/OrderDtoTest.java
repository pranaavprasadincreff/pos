package com.increff.pos.dto;

import com.increff.pos.flow.OrderFlow;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.constants.OrderTimeframe;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.data.OrderItemData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.OrderSearchForm;
import com.increff.pos.util.FormValidationUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderDtoTest {

    @Mock
    private OrderFlow orderFlow;

    @InjectMocks
    private OrderDto orderDto;

    @Test
    void searchOrders_happyPath_normalizesInputs_validates_andDelegatesToFlow() throws Exception {
        OrderSearchForm form = new OrderSearchForm();
        form.setOrderReferenceId(" ord-12 ");
        form.setStatus("fulfillable");
        form.setTimeframe(OrderTimeframe.LAST_DAY);
        form.setPage(0);
        form.setSize(10);

        OrderItemData item = new OrderItemData();
        item.setProductBarcode("B1");
        item.setQuantity(1);
        item.setSellingPrice(10.0);

        OrderData data = new OrderData();
        data.setOrderReferenceId("ORD-0000-0000");
        data.setStatus(OrderStatus.FULFILLABLE.name());
        data.setCreatedAt(ZonedDateTime.now());
        data.setItems(List.of(item));

        Page<OrderData> returned = new PageImpl<>(List.of(data), PageRequest.of(0, 10), 1);

        when(orderFlow.searchOrderData(anyString(), anyString(), any(), any(), anyInt(), anyInt()))
                .thenReturn(returned);

        try (MockedStatic<FormValidationUtil> mockedValidator = Mockito.mockStatic(FormValidationUtil.class)) {
            mockedValidator.when(() -> FormValidationUtil.validate(Mockito.any(OrderSearchForm.class)))
                    .thenAnswer(inv -> null);

            Page<OrderData> page = orderDto.searchOrders(form);

            assertEquals(1, page.getTotalElements());
            assertEquals("ORD-0000-0000", page.getContent().getFirst().getOrderReferenceId());
            assertEquals(1, page.getContent().getFirst().getItems().size());
            assertEquals("B1", page.getContent().getFirst().getItems().getFirst().getProductBarcode());

            ArgumentCaptor<String> refCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<ZonedDateTime> fromCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);
            ArgumentCaptor<ZonedDateTime> toCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);

            verify(orderFlow).searchOrderData(
                    refCaptor.capture(),
                    statusCaptor.capture(),
                    fromCaptor.capture(),
                    toCaptor.capture(),
                    eq(0),
                    eq(10)
            );

            // NormalizationUtil.normalizeOrderSearchForm() should uppercase/trim
            assertEquals("ORD-12", refCaptor.getValue());
            assertEquals("FULFILLABLE", statusCaptor.getValue());

            assertNotNull(fromCaptor.getValue());
            assertNotNull(toCaptor.getValue());
            assertTrue(fromCaptor.getValue().isBefore(toCaptor.getValue()));

            mockedValidator.verify(() -> FormValidationUtil.validate(Mockito.any(OrderSearchForm.class)));
        }
    }

    @Test
    void searchOrders_invalidStatus_throws_andDoesNotCallFlow() throws Exception {
        OrderSearchForm form = new OrderSearchForm();
        form.setStatus("NOT_A_STATUS");
        form.setTimeframe(OrderTimeframe.LAST_DAY);
        form.setPage(0);
        form.setSize(10);

        try (MockedStatic<FormValidationUtil> mockedValidator = Mockito.mockStatic(FormValidationUtil.class)) {
            mockedValidator.when(() -> FormValidationUtil.validate(Mockito.any(OrderSearchForm.class)))
                    .thenThrow(new ApiException("status: Invalid status filter"));

            ApiException ex = assertThrows(ApiException.class, () -> orderDto.searchOrders(form));
            assertTrue(ex.getMessage().toLowerCase().contains("status"));

            verifyNoInteractions(orderFlow);

            mockedValidator.verify(() -> FormValidationUtil.validate(Mockito.any(OrderSearchForm.class)));
        }
    }

    @Test
    void searchOrders_invalidPage_throws_andDoesNotCallFlow() throws Exception {
        OrderSearchForm form = new OrderSearchForm();
        form.setStatus(OrderStatus.FULFILLABLE.name());
        form.setTimeframe(OrderTimeframe.LAST_DAY);
        form.setPage(-1);
        form.setSize(10);

        try (MockedStatic<FormValidationUtil> mockedValidator = Mockito.mockStatic(FormValidationUtil.class)) {
            mockedValidator.when(() -> FormValidationUtil.validate(Mockito.any(OrderSearchForm.class)))
                    .thenThrow(new ApiException("page: Page cannot be negative"));

            ApiException ex = assertThrows(ApiException.class, () -> orderDto.searchOrders(form));
            assertTrue(ex.getMessage().toLowerCase().contains("page"));

            verifyNoInteractions(orderFlow);

            mockedValidator.verify(() -> FormValidationUtil.validate(Mockito.any(OrderSearchForm.class)));
        }
    }

    @Test
    void cancelOrder_normalizesReferenceId_andDelegatesToFlow() throws Exception {
        OrderItemData item = new OrderItemData();
        item.setProductBarcode("B1");
        item.setQuantity(1);
        item.setSellingPrice(10.0);

        OrderData cancelled = new OrderData();
        cancelled.setOrderReferenceId("ORD-12");
        cancelled.setStatus(OrderStatus.CANCELLED.name());
        cancelled.setCreatedAt(ZonedDateTime.now());
        cancelled.setItems(List.of(item));

        when(orderFlow.cancel(eq("ORD-12"))).thenReturn(cancelled);

        OrderData out = orderDto.cancelOrder("  ord-12  ");

        assertNotNull(out);
        assertEquals("ORD-12", out.getOrderReferenceId());
        assertEquals(1, out.getItems().size());
        assertEquals("B1", out.getItems().getFirst().getProductBarcode());

        verify(orderFlow).cancel(eq("ORD-12"));
    }
}
