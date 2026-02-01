package com.increff.pos.dto;

import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.flow.OrderFlow;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.constants.OrderTimeframe;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.OrderFilterForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderDtoTest {

    @Mock
    private OrderFlow orderFlow;

    @InjectMocks
    private OrderDto orderDto;

    @Test
    void filterOrders_happyPath_normalizesInputs_andDelegatesToFlow() throws Exception {
        OrderFilterForm form = new OrderFilterForm();
        form.setOrderReferenceId(" ord-12 ");
        form.setStatus("fulfillable");
        form.setTimeframe(OrderTimeframe.LAST_DAY);
        form.setPage(0);
        form.setSize(10);

        OrderItemPojo item = new OrderItemPojo();
        item.setProductBarcode("P1");
        item.setOrderedQuantity(1);
        item.setSellingPrice(10.0);

        OrderPojo pojo = new OrderPojo();
        pojo.setOrderReferenceId("ORD-0000-0000");
        pojo.setStatus(OrderStatus.FULFILLABLE.name());
        pojo.setOrderTime(ZonedDateTime.now());
        pojo.setOrderItems(List.of(item)); // ✅ IMPORTANT

        Page<OrderPojo> returned = new PageImpl<>(List.of(pojo), PageRequest.of(0, 10), 1);

        when(orderFlow.search(anyString(), anyString(), any(), any(), anyInt(), anyInt()))
                .thenReturn(returned);

        Page<OrderData> page = orderDto.filterOrders(form);

        assertEquals(1, page.getTotalElements());
        assertEquals("ORD-0000-0000", page.getContent().getFirst().getOrderReferenceId());

        ArgumentCaptor<String> refCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);

        verify(orderFlow).search(
                refCaptor.capture(),
                statusCaptor.capture(),
                any(),
                any(),
                eq(0),
                eq(10)
        );

        assertEquals("ORD-12", refCaptor.getValue());
        assertEquals(OrderStatus.FULFILLABLE.name(), statusCaptor.getValue());
    }

    @Test
    void filterOrders_invalidStatus_throws_andDoesNotCallFlow() {
        OrderFilterForm form = new OrderFilterForm();
        form.setStatus("NOT_A_STATUS");
        form.setTimeframe(OrderTimeframe.LAST_DAY);
        form.setPage(0);
        form.setSize(10);

        ApiException ex = assertThrows(ApiException.class, () -> orderDto.filterOrders(form));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid order status"));

        verifyNoInteractions(orderFlow);
    }

    @Test
    void filterOrders_invalidPage_throws_andDoesNotCallFlow() {
        OrderFilterForm form = new OrderFilterForm();
        form.setStatus(OrderStatus.FULFILLABLE.name());
        form.setTimeframe(OrderTimeframe.LAST_DAY);
        form.setPage(-1);
        form.setSize(10);

        assertThrows(ApiException.class, () -> orderDto.filterOrders(form));
        verifyNoInteractions(orderFlow);
    }
}
