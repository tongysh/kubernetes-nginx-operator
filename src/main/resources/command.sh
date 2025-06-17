# 一些相关命令
docker build -t operator-nginx-website:1.0.0 .

docker save -o operator.tar operator-nginx-website:1.0.0

scp ./operator.tar root@k8s-worker2:/root/tongysh

docker load -i operator.tar


