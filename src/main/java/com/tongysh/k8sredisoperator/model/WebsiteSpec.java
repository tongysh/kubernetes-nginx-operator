package com.tongysh.k8sredisoperator.model;


import lombok.Data;

@Data
public class WebsiteSpec {
    private String image;
    private int replicas = 1;
    private int port = 8080;
}
