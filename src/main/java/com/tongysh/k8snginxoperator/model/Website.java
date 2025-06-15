package com.tongysh.k8snginxoperator.model;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;

public class Website extends CustomResource<WebsiteSpec, WebsiteStatus> implements Namespaced {
}
