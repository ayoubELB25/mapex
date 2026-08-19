package dev.ayoubelb25.mapex.model;

import jakarta.persistence.*;

@Entity
@Table(name = "systemlist")
public class SystemList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;        

    @Column(name = "list_name", nullable = false, length = 30)
    private String listName;   

    public Long   getId()                  { return id; }
    public void   setId(Long id)           { this.id = id; }
    public String getName()               { return name; }
    public void   setName(String name)    { this.name = name; }
    public String getListName()           { return listName; }
    public void   setListName(String l)   { this.listName = l; }
}
