package dev.ayoubelb25.mapex.model;

import jakarta.persistence.*;


@Entity
@Table(name = "client_field_config",
       uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "field_key"}))
public class ClientFieldConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "field_key", nullable = false, length = 30)
    private String fieldKey;      

    @Column(name = "label_name", nullable = false, length = 80)
    private String labelName;

    @Column(name = "input_type", nullable = false, length = 10)
    private String inputType;     

    @Column(name = "options", length = 500)
    private String options;       

    public Long   getId()                    { return id; }
    public void   setId(Long id)             { this.id = id; }
    public Client getClient()               { return client; }
    public void   setClient(Client c)       { this.client = c; }
    public String getFieldKey()             { return fieldKey; }
    public void   setFieldKey(String k)     { this.fieldKey = k; }
    public String getLabelName()            { return labelName; }
    public void   setLabelName(String l)    { this.labelName = l; }
    public String getInputType()            { return inputType; }
    public void   setInputType(String t)    { this.inputType = t; }
    public String getOptions()              { return options; }
    public void   setOptions(String o)      { this.options = o; }
}


