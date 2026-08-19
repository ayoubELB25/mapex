package dev.ayoubelb25.mapex.model;

import jakarta.persistence.*;

@Entity
@Table(name = "data_mapping")
public class DataMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_id", nullable = false, unique = true, length = 10)
    private String sourceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "system_id", nullable = false)
    private SystemList systemList;

    public Long       getId()                      { return id; }
    public void       setId(Long id)               { this.id = id; }
    public String     getSourceId()                { return sourceId; }
    public void       setSourceId(String s)        { this.sourceId = s; }
    public SystemList getSystemList()              { return systemList; }
    public void       setSystemList(SystemList sl) { this.systemList = sl; }
}