FROM clojure:lein-2.9.8@sha256:1a0a9a27d2a2f84c59cce0c4fde878349d6027b56ae1236cf809867c24fdefbd AS client-builder

RUN apt-get update && apt-get install -y \
    build-essential \
    curl \
    dumb-init \
    git \
    gnupg \
    rlwrap \
 && rm -rf /var/lib/apt/lists/*

RUN curl -sL https://deb.nodesource.com/setup_14.x  | bash - && \
    apt-get -y install nodejs

ENV CLOJURE_VERSION=1.10.3.1087

RUN curl -O https://download.clojure.org/install/linux-install-${CLOJURE_VERSION}.sh && \
    chmod +x linux-install-${CLOJURE_VERSION}.sh && \
    ./linux-install-${CLOJURE_VERSION}.sh

# TODO move the above to a base image

WORKDIR /ui
# cache packages in layer
COPY ui/package.json /ui/package.json
COPY ui/package-lock.json /ui/package-lock.json
RUN --mount=type=cache,target=/usr/src/app/.npm \
    npm set cache /usr/src/app/.npm && \
    npm ci
# install
COPY ui /ui
RUN npx shadow-cljs release app

FROM alpine
LABEL org.opencontainers.image.title="image-watcher" \
    org.opencontainers.image.description="My awesome Docker extension" \
    org.opencontainers.image.vendor="Docker Inc." \
    com.docker.desktop.extension.api.version=">= 0.2.3" \
    com.docker.extension.screenshots="" \
    com.docker.extension.detailed-description="" \
    com.docker.extension.publisher-url="" \
    com.docker.extension.additional-urls="" \
    com.docker.extension.changelog=""

WORKDIR /
COPY metadata.json .
COPY atomist.svg .
COPY ui/index.html ui/index.html
COPY --from=client-builder /ui/build ui/build
