package com.nbfc.riskgovernance;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
@Entity
@Table(name="governance_outbox_event")
public class GovernanceOutboxEvent {
    @Id @Column(length=36) public String id;
    @Column(name="event_type",nullable=false,length=80) public String eventType;
    @Column(name="aggregate_id",nullable=false,length=36) public String aggregateId;
    @Lob @Column(nullable=false) public String payload;
    @Convert(converter=NumericBooleanConverter.class) @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(nullable=false,precision=1,scale=0) public boolean published;
    @Column(name="created_at",nullable=false) public Instant createdAt;
    protected GovernanceOutboxEvent() {}
    GovernanceOutboxEvent(String id,String eventType,String aggregateId,String payload) {
        this.id=id; this.eventType=eventType; this.aggregateId=aggregateId; this.payload=payload; this.createdAt=Instant.now();
    }
}

