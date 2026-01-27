package com.increff.pos.helper;


import com.increff.pos.db.ClientPojo;
import com.increff.pos.db.ClientUpdatePojo;
import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.form.ClientForm;
import com.increff.pos.model.form.ClientUpdateForm;

import java.util.List;
import java.util.stream.Collectors;

public class ClientHelper {
    public static ClientPojo convertFormToEntity(ClientForm dto) {
        ClientPojo clientPojo = new ClientPojo();
        clientPojo.setName(dto.getName());
        clientPojo.setEmail(dto.getEmail());
        return clientPojo;
    }

    public static ClientUpdatePojo convertUpdateFormToEntity(ClientUpdateForm dto) {
        ClientUpdatePojo clientUpdatePojo = new ClientUpdatePojo();
        clientUpdatePojo.setName(dto.getName());
        clientUpdatePojo.setOldEmail(dto.getOldEmail());
        clientUpdatePojo.setNewEmail(dto.getNewEmail());
        return clientUpdatePojo;
    }

    public static List<ClientData> convertToUserDataList(List<ClientPojo> clientPojoDataList) {
        return clientPojoDataList.stream().map(ClientHelper::convertFormToDto).collect(Collectors.toList());
    }

    public static ClientData convertFormToDto(ClientPojo clientPojo) {
        ClientData clientData = new ClientData();
        clientData.setId(clientPojo.getId());
        clientData.setName(clientPojo.getName());
        clientData.setEmail(clientPojo.getEmail());
        return clientData;
    }
}
