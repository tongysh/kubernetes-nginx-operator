package com.tongysh.k8snginxoperator.model;


import lombok.Data;

@Data
public class WebsiteSpec {
    private String image;
    private int replicas = 1;
    private int port = 8080;
}
