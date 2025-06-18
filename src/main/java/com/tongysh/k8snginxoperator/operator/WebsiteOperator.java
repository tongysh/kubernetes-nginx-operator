package com.tongysh.k8snginxoperator.operator;

import cn.hutool.log.StaticLog;
import com.tongysh.k8snginxoperator.model.Website;
import com.tongysh.k8snginxoperator.model.WebsiteStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import io.fabric8.kubernetes.client.utils.Serialization;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class WebsiteOperator {


    @Autowired
    private KubernetesClient client;



    /* 应用启动后自动开始执行 */
    @EventListener(ApplicationReadyEvent.class)
    public void startWatching() {
        client.resources(Website.class)
                .inAnyNamespace()
                .watch(new Watcher<>() {
                    @Override
                    public void eventReceived(Action action, Website website) {
                        StaticLog.info("监控到动作：{}", action);
                        if (action == Action.ADDED) {
                            StaticLog.info("执行website资源新增逻辑===>add");
                            doAdd(website);
                        } else if (action == Action.DELETED) {
                            StaticLog.info("执行website资源删除逻辑===>delete");
                            doDelete(website);
                        }
                    }

                    @Override
                    public void onClose(WatcherException cause) {
                        // 处理连接中断
                    }
                });
    }


    /* 执行删除逻辑 */
    private void doDelete(Website website) {
        String namespace = website.getMetadata().getNamespace();
        String name = website.getMetadata().getName();

        try {
            // 删除Deployment
            List<StatusDetails> delete = client.apps().deployments()
                    .inNamespace(namespace)
                    .withName(name)
                    .delete();
            // 删除Service
            client.services()
                    .inNamespace(namespace)
                    .withName(name)
                    .delete();
        } catch (Exception e) {
            StaticLog.error("删除命名空间 {} 下的 Deployment 和 Service 时出错: {}", namespace, e.getMessage(), e);
        }

    }

    private void doAdd(Website website) {
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
        StaticLog.info("Starting to create Deployment for website: {}", website.getMetadata().getName());
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
        StaticLog.info("Successfully created Deployment for website: {}", website.getMetadata().getName());
    }

    private void createService(Website website) {
        StaticLog.info("Starting to create Service for website: {}", website.getMetadata().getName());
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
                        .withType("NodePort")
                        .endSpec()
                        .build())
                .serverSideApply();
        StaticLog.info("Successfully created Service for website: {}", website.getMetadata().getName());
    }


    private void updateStatus(Website website, String phase, String message) {
        website.setStatus(new WebsiteStatus());
        website.getStatus().setPhase(phase);
        website.getStatus().setMessage(message);

        if ("Running".equals(phase)) {
            website.getStatus().setUrl(message);
        }

        int maxRetries = 5;
        int retryCount = 1;

        Website refreshedWebsite = null;

        while (retryCount <= maxRetries) {
            try {
                // 重新获取资源
                refreshedWebsite = client.resources(Website.class)
                        .inNamespace(website.getMetadata().getNamespace())
                        .withName(website.getMetadata().getName())
                        .get();

                /* 更新状态 */
                refreshedWebsite.setStatus(website.getStatus());

                String statusPatch = "["
                        + "{\"op\":\"replace\",\"path\":\"/status\",\"value\":"
                        + Serialization.asJson(refreshedWebsite.getStatus())
                        + "}"
                        + "]";

                client.resources(Website.class)
                        .inNamespace(refreshedWebsite.getMetadata().getNamespace())
                        .withName(refreshedWebsite.getMetadata().getName())
                        .patch(PatchContext.of(PatchType.JSON), statusPatch); // 会被operator监控到一次modify操作
//                        .updateStatus(refreshedWebsite);  不要使用该方法  会有问题  暂未解决

                StaticLog.info("Successfully updated status for website: {}", refreshedWebsite.getMetadata().getName());

                break; // 更新成功，退出循环
            } catch (KubernetesClientException e) {

                /* 发生异常 */
                if (e.getCode() == 404 && retryCount < maxRetries) {
                    StaticLog.error("Failed to update status for website: {} ---> 正在进行第{}次重试", refreshedWebsite.getMetadata().getName(), retryCount, e.getMessage());
                    retryCount++;
                    try {
                        TimeUnit.SECONDS.sleep(2); // 等待 2 秒后重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    StaticLog.error("Failed to update status for website: {}", refreshedWebsite.getMetadata().getName(), e);
                    throw e; // 非 404 错误或达到最大重试次数，抛出异常
                }
            }
        }


    }

    private ObjectMeta createMeta(Website website) {
        return new ObjectMetaBuilder()
                .withName(website.getMetadata().getName())
                .withNamespace(website.getMetadata().getNamespace())
                .addToLabels("managed-by", "website-operator")
                .build();
    }
}