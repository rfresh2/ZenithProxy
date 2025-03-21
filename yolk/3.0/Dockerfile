FROM        --platform=$TARGETOS/$TARGETARCH  eclipse-temurin:21-jdk

LABEL       author="Veslys" maintainer="vesly@femboy.tr"

RUN apt update && apt install -y wget apt-transport-https gpg 
RUN         apt update \
            && apt -y install gcc ca-certificates dnsutils wget iproute2 procps tini lsof unzip \
            && useradd -m -d /home/container container

USER        container
ENV         USER=container HOME=/home/container
WORKDIR     /home/container

STOPSIGNAL SIGINT

COPY        --chown=container:container ./../entrypoint.sh /entrypoint.sh
RUN         chmod +x /entrypoint.sh
ENTRYPOINT    ["/usr/bin/tini", "-g", "--"]
CMD         ["/entrypoint.sh"]
