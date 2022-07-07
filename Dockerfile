# FROM --platform=$BUILDPLATFORM node:17.7-alpine3.14 AS client-builder
# WORKDIR /ui
# # cache packages in layer
# COPY ui/package.json /ui/package.json
# COPY ui/package-lock.json /ui/package-lock.json
# RUN --mount=type=cache,target=/usr/src/app/.npm \
    # npm set cache /usr/src/app/.npm && \
    # npm ci
# # install
# COPY ui /ui
# RUN npm run build

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

COPY metadata.json .
COPY atomist.svg .
COPY ui/index.html /ui/index.html
COPY --from=client-builder /ui/build ui
