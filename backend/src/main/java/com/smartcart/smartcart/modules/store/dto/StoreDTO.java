package com.smartcart.smartcart.modules.store.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreDTO {
    private Integer storeId;
    private String name;
    private String slug;
    private String logo;
    private String website;
    private Boolean active;
    private String scrapingUrl;
    private String scrapingConf;
    private Long productCount;
}
