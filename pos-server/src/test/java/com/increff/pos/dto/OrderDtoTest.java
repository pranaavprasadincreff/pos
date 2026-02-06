package com.increff.pos.dto;

import com.increff.pos.api.ProductApi;
import com.increff.pos.db.OrderItemPojo;
import com.increff.pos.db.OrderPojo;
import com.increff.pos.db.ProductPojo;
import com.increff.pos.flow.OrderFlow;
import com.increff.pos.model.constants.OrderStatus;
import com.increff.pos.model.constants.OrderTimeframe;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.OrderSearchForm;
import com.increff.pos.model.form.PageForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @Mock
    private ProductApi productApi;

    @InjectMocks
    private OrderDto orderDto;

    @Test
    void searchOrders_happyPath_delegatesToFlow_andBuildsDataUsingProductIds() throws Exception {
        OrderSearchForm form = new OrderSearchForm();
        form.setOrderReferenceId(" ord-12 ");
        form.setStatus("fulfillable");
        form.setTimeframe(OrderTimeframe.LAST_DAY);
        form.setPage(0);
        form.setSize(10);

        // OrderPojo returned from flow must have productIds (DB stores productId)
        OrderItemPojo item = new OrderItemPojo();
        item.setProductId("PID-1");
        item.setOrderedQuantity(1);
        item.setSellingPrice(10.0);

        OrderPojo pojo = new OrderPojo();
        pojo.setOrderReferenceId("ORD-0000-0000");
        pojo.setStatus(OrderStatus.FULFILLABLE.name());
        pojo.setOrderTime(ZonedDateTime.now());
        pojo.setOrderItems(List.of(item));

        Page<OrderPojo> returned = new PageImpl<>(List.of(pojo), PageRequest.of(0, 10), 1);

        when(orderFlow.search(anyString(), anyString(), any(), any(), anyInt(), anyInt()))
                .thenReturn(returned);

        // DTO needs productApi.findByIds to map back productBarcode into OrderItemData
        ProductPojo product = new ProductPojo();
        product.setId("PID-1");
        product.setBarcode("B1");
        when(productApi.findByIds(eq(List.of("PID-1")))).thenReturn(List.of(product));

        Page<OrderData> page = orderDto.searchOrders(form);

        assertEquals(1, page.getTotalElements());
        assertEquals("ORD-0000-0000", page.getContent().getFirst().getOrderReferenceId());
        assertEquals(1, page.getContent().getFirst().getItems().size());
        assertEquals("B1", page.getContent().getFirst().getItems().getFirst().getProductBarcode());

        ArgumentCaptor<String> refCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ZonedDateTime> fromCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);
        ArgumentCaptor<ZonedDateTime> toCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);

        verify(orderFlow).search(
                refCaptor.capture(),
                statusCaptor.capture(),
                fromCaptor.capture(),
                toCaptor.capture(),
                eq(0),
                eq(10)
        );

        // Your DTO currently does NOT normalize ref/status in code itself, only ValidationUtil does.
        // So we assert pass-through of what form provides (minus whatever ValidationUtil changes).
        // If ValidationUtil normalizes: update expected values accordingly.
        assertEquals(" ord-12 ", refCaptor.getValue());
        assertEquals("fulfillable", statusCaptor.getValue());

        assertNotNull(fromCaptor.getValue());
        assertNotNull(toCaptor.getValue());
        assertTrue(fromCaptor.getValue().isBefore(toCaptor.getValue()));

        verify(productApi).findByIds(eq(List.of("PID-1")));
    }

    @Test
    void getAllOrders_happyPath_delegatesToFlow_andBuildsData() throws Exception {
        PageForm form = new PageForm();
        form.setPage(0);
        form.setSize(10);

        OrderItemPojo item = new OrderItemPojo();
        item.setProductId("PID-1");
        item.setOrderedQuantity(2);
        item.setSellingPrice(20.0);

        OrderPojo pojo = new OrderPojo();
        pojo.setOrderReferenceId("ORD-1");
        pojo.setStatus(OrderStatus.FULFILLABLE.name());
        pojo.setOrderTime(ZonedDateTime.now());
        pojo.setOrderItems(List.of(item));

        Page<OrderPojo> returned = new PageImpl<>(List.of(pojo), PageRequest.of(0, 10), 1);
        when(orderFlow.search(isNull(), isNull(), isNull(), isNull(), eq(0), eq(10))).thenReturn(returned);

        ProductPojo product = new ProductPojo();
        product.setId("PID-1");
        product.setBarcode("B1");
        when(productApi.findByIds(eq(List.of("PID-1")))).thenReturn(List.of(product));

        Page<OrderData> page = orderDto.getAllOrders(form);

        assertEquals(1, page.getTotalElements());
        assertEquals("ORD-1", page.getContent().getFirst().getOrderReferenceId());
        assertEquals("B1", page.getContent().getFirst().getItems().getFirst().getProductBarcode());

        verify(orderFlow).search(null, null, null, null, 0, 10);
        verify(productApi).findByIds(eq(List.of("PID-1")));
    }

    @Test
    void searchOrders_invalidStatus_throws_andDoesNotCallFlow() {
        OrderSearchForm form = new OrderSearchForm();
        form.setStatus("NOT_A_STATUS");
        form.setTimeframe(OrderTimeframe.LAST_DAY);
        form.setPage(0);
        form.setSize(10);

        ApiException ex = assertThrows(ApiException.class, () -> orderDto.searchOrders(form));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid"));

        verifyNoInteractions(orderFlow);
        verifyNoInteractions(productApi);
    }

    @Test
    void searchOrders_invalidPage_throws_andDoesNotCallFlow() {
        OrderSearchForm form = new OrderSearchForm();
        form.setStatus(OrderStatus.FULFILLABLE.name());
        form.setTimeframe(OrderTimeframe.LAST_DAY);
        form.setPage(-1);
        form.setSize(10);

        assertThrows(ApiException.class, () -> orderDto.searchOrders(form));

        verifyNoInteractions(orderFlow);
        verifyNoInteractions(productApi);
    }
}
