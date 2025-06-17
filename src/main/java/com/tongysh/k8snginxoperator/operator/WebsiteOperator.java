package com.tongysh.k8snginxoperator.operator;

import cn.hutool.json.JSONUtil;
import cn.hutool.log.StaticLog;
import com.tongysh.k8snginxoperator.model.Website;
import com.tongysh.k8snginxoperator.model.WebsiteStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebsiteOperator {


    @Autowired
    private KubernetesClient client;

    @EventListener(ApplicationReadyEvent.class)
    public void startWatching() {
        client.resources(Website.class)
                .inAnyNamespace()
                .watch(new Watcher<>() {
                    @Override
                    public void eventReceived(Action action, Website website) {

                        StaticLog.info("监控到动作：{}", JSONUtil.toJsonPrettyStr(action));

                        reconcile(website);
                    }

                    @Override
                    public void onClose(WatcherException cause) {
                        // 处理连接中断
                    }
                });
    }

    private void reconcile(Website website) {
        String ns = website.getMetadata().getNamespace();
        String name = website.getMetadata().getName();

        try {
            // 1. 创建Deployment
            createDeployment(website);

            // 2. 创建Service
            createService(website);

            // 3. 更新状态
            updateStatus(website,
                    "Running",
                    "http://" + name + "." + ns + ".svc.cluster.local:" + website.getSpec().getPort());
        } catch (Exception e) {
            updateStatus(website, "Failed", e.getMessage());
        }
    }

    private void createDeployment(Website website) {
        client.apps().deployments()
                .inNamespace(website.getMetadata().getNamespace())
                .resource(new DeploymentBuilder()
                        .withMetadata(createMeta(website))
                        .withNewSpec()
                        .withReplicas(website.getSpec().getReplicas())
                        .withNewSelector()
                        .addToMatchLabels("app", website.getMetadata().getName())
                        .endSelector()
                        .withNewTemplate()
                        .withNewMetadata()
                        .addToLabels("app", website.getMetadata().getName())
                        .endMetadata()
                        .withNewSpec()
                        .addNewContainer()
                        .withName("web")
                        .withImage(website.getSpec().getImage())
                        .addNewPort()
                        .withContainerPort(website.getSpec().getPort())
                        .endPort()
                        .endContainer()
                        .endSpec()
                        .endTemplate()
                        .endSpec()
                        .build())
                .serverSideApply();
    }

    private void createService(Website website) {
        client.services()
                .inNamespace(website.getMetadata().getNamespace())
                .resource(new ServiceBuilder()
                        .withMetadata(createMeta(website))
                        .withNewSpec()
                        .addToSelector("app", website.getMetadata().getName())
                        .addNewPort()
                        .withPort(80)
                        .withNewTargetPort(website.getSpec().getPort())
                        .endPort()
                        .withType("ClusterIP")
                        .endSpec()
                        .build())
                .serverSideApply();
    }

    private void updateStatus(Website website, String phase, String message) {
        website.setStatus(new WebsiteStatus());
        website.getStatus().setPhase(phase);
        website.getStatus().setMessage(message);

        if ("Running".equals(phase)) {
            website.getStatus().setUrl(message);
        }

        client.resources(Website.class)
                .inNamespace(website.getMetadata().getNamespace())
                .withName(website.getMetadata().getName())
                .updateStatus(website);
    }

    private ObjectMeta createMeta(Website website) {
        return new ObjectMetaBuilder()
                .withName(website.getMetadata().getName())
                .withNamespace(website.getMetadata().getNamespace())
                .addToLabels("managed-by", "website-operator")
                .build();
    }
}