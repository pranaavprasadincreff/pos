package com.increff.pos.controller;

import com.increff.pos.dto.SupervisorUserDto;
import com.increff.pos.model.data.AuthUserData;
import com.increff.pos.model.exception.ApiException;
import com.increff.pos.model.form.CreateOperatorForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/supervisor/operators")
public class SupervisorUserController {

    @Autowired
    private SupervisorUserDto supervisorUserDto;

    @PostMapping
    public AuthUserData create(@RequestBody CreateOperatorForm form) throws ApiException {
        return supervisorUserDto.create(form);
    }

    @GetMapping
    public List<AuthUserData> list() {
        return supervisorUserDto.list();
    }
}
