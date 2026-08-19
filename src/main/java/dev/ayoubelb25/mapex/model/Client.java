package dev.ayoubelb25.mapex.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "client")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "client_type", length = 30)
    private String clientType;

    @Column(name = "registration_number", length = 50)
    private String registrationNumber;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;


    @OneToMany(mappedBy = "client", fetch = FetchType.EAGER,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fieldKey ASC")
    private List<ClientFieldConfig> fieldConfigs = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String t) {
        this.clientType = t;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String r) {
        this.registrationNumber = r;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String e) {
        this.contactEmail = e;
    }

    public List<ClientFieldConfig> getFieldConfigs() {
        return fieldConfigs;
    }

    public void setFieldConfigs(List<ClientFieldConfig> f) {
        this.fieldConfigs = f;
    }
}
