package com.tongysh.k8sredisoperator.model;

import lombok.Data;

@Data
public class WebsiteStatus {

    private String url;
    private String phase;  // Pending | Running | Failed
    private String message;
}
