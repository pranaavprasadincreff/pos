package com.increff.pos.helper;

import com.increff.pos.db.ClientPojo;
import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.form.ClientForm;
import com.increff.pos.model.form.ClientUpdateForm;

public class ClientHelper {
    public static ClientPojo convertFormToEntity(ClientForm dto) {
        ClientPojo clientPojo = new ClientPojo();
        clientPojo.setName(dto.getName());
        clientPojo.setEmail(dto.getEmail());
        return clientPojo;
    }

    public static ClientPojo convertUpdateFormToClientPojo(ClientUpdateForm dto) {
        ClientPojo clientPojo = new ClientPojo();
        clientPojo.setName(dto.getName());
        clientPojo.setEmail(dto.getNewEmail());
        return clientPojo;
    }

    public static ClientData convertFormToDto(ClientPojo clientPojo) {
        ClientData clientData = new ClientData();
        clientData.setName(clientPojo.getName());
        clientData.setEmail(clientPojo.getEmail());
        return clientData;
    }
}
