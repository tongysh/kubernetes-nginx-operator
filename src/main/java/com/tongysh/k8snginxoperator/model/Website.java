package com.tongysh.k8snginxoperator.model;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;

// 指定 API 组和版本  一定要指定group和version   要不然会出现404找不到
@io.fabric8.kubernetes.model.annotation.Group("demo.example.com")
@io.fabric8.kubernetes.model.annotation.Version("v1alpha1")
public class Website extends CustomResource<WebsiteSpec, WebsiteStatus> implements Namespaced {
}
