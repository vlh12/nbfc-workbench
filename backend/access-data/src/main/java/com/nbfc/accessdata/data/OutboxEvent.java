package com.nbfc.accessdata.data;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
@Entity
@Table(name="outbox_event")
public class OutboxEvent {
    @Id @Column(length=36) public String id;
    @Column(name="batch_id",nullable=false,length=36) public String batchId;
    @Lob @Column(nullable=false) public String payload;
    @Convert(converter=NumericBooleanConverter.class)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(nullable=false,precision=1,scale=0) public boolean published;
    @Column(name="created_at",nullable=false) public Instant createdAt;
    protected OutboxEvent() {}
    public OutboxEvent(String id,String batchId,String payload) {
        this.id=id; this.batchId=batchId; this.payload=payload; createdAt=Instant.now();
    }
}
