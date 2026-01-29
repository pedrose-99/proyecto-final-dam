package com.smartcart.smartcart.modules.store.entity;


import org.springframework.data.annotation.Id;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "store")
@Data
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Integer storeId;

    @Column(name = "name")
    private String name;

    @Column(name = "slug") 
    private String slug;

    @Column(name = "logo")
    private String logo;

    @Column(name = "website")
    private String website;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "scraping_url")
    private String scrapingUrl;

    @Column(name = "scraping_conf")
    private String scrapingConf;

    //empty constructor
    public Store() {}
    //full constructor

    public Store(Boolean active, String logo, String name, String scrapingConf, String scrapingUrl, String slug, Integer storeId, String website) {
        this.active = active;
        this.logo = logo;
        this.name = name;
        this.scrapingConf = scrapingConf;
        this.scrapingUrl = scrapingUrl;
        this.slug = slug;
        this.storeId = storeId;
        this.website = website;
    }
    
    
}