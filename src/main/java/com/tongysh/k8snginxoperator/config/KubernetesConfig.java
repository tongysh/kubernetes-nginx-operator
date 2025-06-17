package com.tongysh.k8snginxoperator.config;

import cn.hutool.log.StaticLog;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class KubernetesConfig {

    /**
     * 本地开发环境使用，有以下几个需要注意的点：
     * 1- master url使用master的宿主机ip加6443端口即可，无必要开放api-server的nodeport类型的svc，使用（lsof -i:6443）可以发现master宿主机
     * 的6443端口正在被监听，另外不要忘记是https，而不是http
     * 2- token使用的是service account对应的secret的token，token要base64解码（echo -n "base64编码字符串" | base64 -d）后使用，不能直接
     * 粘贴过来用，无过期时间
     * 3- 开发环境可信任自签名证书
     *
     * @return
     */
//    @Bean
//    public KubernetesClient kubernetesClient() {
//        Config config = new ConfigBuilder()
//                .withMasterUrl("https://192.168.8.3:6443")
//                .withTrustCerts(true)
//                .withOauthToken("eyJhbGciOiJSUzI1NiIsImtpZCI6IkJFaXdxRk9ESURKSlNMRWVkd0dwZDdyejlHOE5wQzdQUWZyYzVpVGhMdmcifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6IndlYnNpdGUtb3BlcmF0b3Itc2EiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoid2Vic2l0ZS1vcGVyYXRvci1zYSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImEwN2U1MGM3LWU0Y2EtNDA4Mi1hN2JjLTAxZTgyY2M1N2RiYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0OndlYnNpdGUtb3BlcmF0b3Itc2EifQ.2a1_jcI5Jk32OnAhmRIjBJKL_Ua1uvpyiOoL5js3AkhrkZad6QmMnMlFVGgB2RBjMbz0ql6gSeUd1oh8aRrcBeIlfC2mNkpmY97tsRlW9vAzDGSb4B37YxwXMtfATIDMFNr0XAoNFiceUiTG3Uu40DVhMcsK1wn6YJeBrtuDEOBCN88jvWsvhczdZZd1xGfQ5Lz2MduznLRH90auFxMLczy1gnsTR8YkBh5olgTdo1RvxZ1eQhIndsGW1yV3kCdQbE20feUD_KW7MJRyJRb9Hy7GOioGf-M-EjbJmn4yfbQaWnOs6x7ZZN7L5ag2DycNeelKmrmNyhkw38jgS9NSqQ")
//                .build();
//        return new DefaultKubernetesClient(config);
//    }


    /**
     * 生产环境使用，有以下几个需要注意的点：
     * 1- master url为https://10.96.0.1:443，这个可以在default命名空间下的名称为kubernetes的svc中查询到（）
     * 2- token可以硬编码（无过期时间），也可以直接从容器中读取，从容器中读取的时候需要将deployment挂载对应的sa，而sa要有对应的secret（deployment挂载sa的时候，会将其
     * 绑定的secret挂载到容器的/var/run/secrets/kubernetes.io/serviceaccount/token文件中）
     * 3- 在容器中获取的token（/var/run/secrets/kubernetes.io/serviceaccount/token）已经是base64解码后的，不需要在进行解码，直接读取就可以
     * 4- 通过deployment挂载sa的方式，拿到的token是有过期时间的，解决方案是手动将secret挂载到容器，使用volumemount（无过期时间，自动base64解码），而不是直接挂载sa的方式
     *
     * @return
     */
    @Bean
    public KubernetesClient kubernetesClient() {
        /* 直接从容器中获取token */
        String tokenPath = "/var/run/secrets/kubernetes.io/serviceaccount/token";

        /* 记录service account token */
        String saToken = null;
        try {
            saToken = Files.readString(Paths.get(tokenPath)).trim();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        StaticLog.info("token info: {}", saToken);

        /* 构建客户端  访问k8s集群的相关内容时  只需要注入该客户端即可 */
        Config config = new ConfigBuilder()
//                .withMasterUrl("https://10.96.0.1:443")
                .withTrustCerts(true)
                /* 此token无需进行解码 */
                .withOauthToken(saToken)
                .build();
        return new DefaultKubernetesClient(config);
    }


}
