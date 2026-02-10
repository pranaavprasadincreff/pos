package com.increff.pos.db;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "clients")
@CompoundIndexes({
        @CompoundIndex(name = "email_createdAt_idx", def = "{'email': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "name_createdAt_idx",  def = "{'name': 1, 'createdAt': -1}")
})
public class ClientPojo extends AbstractPojo {

    @Indexed(unique = true)
    private String email;

    private String name;
}
