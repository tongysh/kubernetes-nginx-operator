package com.tongysh.k8snginxoperator;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class K8sNginxOperatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(K8sNginxOperatorApplication.class, args);
    }


    @Bean
    public KubernetesClient kubernetesClient() {
        // 定义 service account 相关文件路径
        String tokenPath = "/var/run/secrets/kubernetes.io/serviceaccount/token";
        String caCertPath = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";
        String namespacePath = "/var/run/secrets/kubernetes.io/serviceaccount/namespace";

        // 读取 namespace 文件内容
        String namespace = null;
        try {
            namespace = java.nio.file.Files.readString(java.nio.file.Paths.get(namespacePath));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read namespace from service account", e);
        }

        // 构建 Config 对象
        Config config = new ConfigBuilder()
                .withOauthToken(readToken(tokenPath))
                .withCaCertFile(caCertPath)
                .withNamespace(namespace)
                .build();

        return new DefaultKubernetesClient(config);
    }

    private String readToken(String tokenPath) {
        try {
            return java.nio.file.Files.readString(java.nio.file.Paths.get(tokenPath));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read token from service account", e);
        }
    }

}
