package com.increff.pos.db;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "clients")
@CompoundIndexes({
        @CompoundIndex(name = "idx_clients_email", def = "{'email': 1}", unique = true),
        @CompoundIndex(name = "idx_clients_email_createdAt", def = "{'email': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "idx_clients_name_createdAt",  def = "{'name': 1, 'createdAt': -1}")
})
public class ClientPojo extends AbstractPojo {

    private String email;

    private String name;
}
