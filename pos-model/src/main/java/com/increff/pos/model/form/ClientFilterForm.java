package com.increff.pos.model.form;

import lombok.Data;

@Data
public class ClientFilterForm {
    private String name;
    private String email;
    private int page = 0;
    private int size = 9;
}
