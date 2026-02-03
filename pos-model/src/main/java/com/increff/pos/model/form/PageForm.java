package com.increff.pos.model.form;

import lombok.Data;

// TODO keep in pos-modal
@Data
public class PageForm {
    private int page = 0;
    private int size = 10;
}