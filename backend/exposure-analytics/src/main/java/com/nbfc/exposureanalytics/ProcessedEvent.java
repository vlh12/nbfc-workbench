package com.nbfc.exposureanalytics;
import jakarta.persistence.*;
@Entity
@Table(name="processed_event")
public class ProcessedEvent {
    @Id @Column(length=36) public String id;
    protected ProcessedEvent() {}
    public ProcessedEvent(String id) { this.id=id; }
}

