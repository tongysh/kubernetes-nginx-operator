package com.tongysh.k8sredisoperator;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class K8sRedisOperatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(K8sRedisOperatorApplication.class, args);
    }


    @Bean
    public KubernetesClient kubernetesClient() {
        return new DefaultKubernetesClient();
    }

}
