# JDK/Maven 环境配置

## 目录结构
```
.env/
├── jdk8u482-b08/          # JDK 8.482 (需手动下载)
├── apache-maven-3.9.6/    # Maven 3.9.6 (需手动下载)
└── env.sh                 # 环境变量初始化脚本
```

## 首次设置（沙箱重置后）

```bash
# 1. 创建目录
mkdir -p /workspace/projects/.env

# 2. 下载 JDK 8.482 (清华镜像)
cd /workspace/projects/.env
curl -L "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/8/jdk/x64/linux/OpenJDK8U-jdk_x64_linux_hotspot_8u482b08.tar.gz" -o jdk8.tar.gz
tar -xzf jdk8.tar.gz

# 3. 下载 Maven 3.9.6
curl -L "https://mirrors.huaweicloud.com/apache/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz" -o maven.tar.gz
tar -xzf maven.tar.gz

# 4. 加载环境变量
source /workspace/projects/.env/env.sh
```

## 使用方式

```bash
# 方式1: 手动加载
source /workspace/projects/.env/env.sh

# 方式2: 重新打开终端（已自动添加到 .bashrc）
```

## 验证

```bash
java -version
mvn -version
```
