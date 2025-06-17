package com.tongysh.k8snginxoperator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PodListResponse {

    private String namespace;
    private int totalPods;
    private List<PodInfo> pods;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PodInfo {
        private String name;
        private String status;
        private String node;
        private String creationTime;
        private String image;
    }
}
