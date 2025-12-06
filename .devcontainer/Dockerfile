FROM  --platform=linux/amd64 debian:bookworm-slim

ENV DEBIAN_FRONTEND=noninteractive
ENV KONAN_DATA_DIR=/home/test/.konan

RUN dpkg --add-architecture amd64 && dpkg --add-architecture arm64

RUN apt-get update && apt-get install -y \
        build-essential \
        git \
        curl \
        unzip \
        pkg-config \
        openjdk-17-jdk \
        libsystemd-dev:amd64 libsystemd-dev:arm64 \
        libc6-dev:amd64 libc6-dev:arm64 \
        libc6:amd64 libc6:arm64 \
        libgcc-s1:arm64 \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

RUN useradd -ms /bin/bash test && \
    echo "test ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

USER test
WORKDIR /project

