package com.tongysh.k8snginxoperator.controller;


import cn.hutool.log.StaticLog;
import com.tongysh.k8snginxoperator.model.PodListResponse;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * pod资源增删改查的相关接口
 */
@RestController
@RequestMapping("/api/k8s")
public class PodController {


    @Autowired
    private KubernetesClient kubernetesClient;


    @GetMapping("/namespaces/{namespace}/pods")
    public ResponseEntity<PodListResponse> listPodsInNamespace(@PathVariable String namespace) {

        try {

            /* 打点日志 */
            StaticLog.info("Querying pods in namespace: {}", namespace);

            // 获取命名空间下的所有Pod
            List<Pod> podList = kubernetesClient.pods().inNamespace(namespace).list().getItems();
            List<PodListResponse.PodInfo> podInfoList = new ArrayList<>();

            for (Pod pod : podList) {
                PodListResponse.PodInfo podInfo = new PodListResponse.PodInfo();
                podInfo.setName(pod.getMetadata().getName());
                podInfo.setStatus(pod.getStatus() != null ? pod.getStatus().getPhase() : "Unknown");
                podInfo.setNode(pod.getSpec() != null && pod.getSpec().getNodeName() != null
                        ? pod.getSpec().getNodeName() : "Unknown Node");

                // 格式化创建时间
                if (pod.getMetadata().getCreationTimestamp() != null) {
                    String creationTimestamp = pod.getMetadata().getCreationTimestamp();
                    podInfo.setCreationTime(creationTimestamp);
                } else {
                    podInfo.setCreationTime("Unknown");
                }

                // 获取主容器镜像
                if (!pod.getSpec().getContainers().isEmpty()) {
                    podInfo.setImage(pod.getSpec().getContainers().get(0).getImage());
                } else {
                    podInfo.setImage("No container");
                }

                podInfoList.add(podInfo);
            }

            PodListResponse response = new PodListResponse();
            response.setNamespace(namespace);
            response.setTotalPods(podList.size());
            response.setPods(podInfoList);

            StaticLog.info("Found {} pods in namespace: {}", podList.size(), namespace);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            StaticLog.error("Error querying pods in namespace {}: {}", namespace, e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }

    }


}
