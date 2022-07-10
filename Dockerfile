FROM amazoncorretto:18-alpine3.15

LABEL maintainer="reader@lucaskjaerozhang.com"
WORKDIR /app
ENV INSTALL_DIR /usr/local
ENV SBT_HOME /usr/local/sbt
ENV PATH ${PATH}:${SBT_HOME}/bin

# Needed because sbt will barf if we don't have one
# Even though we aren't actually accessing the maven package repository
ENV GITHUB_TOKEN=faketoken

# Keep failing pipe command from reporting success to the build.
SHELL ["/bin/ash", "-eo", "pipefail", "-c"]

# Sbt requires this for some reason
RUN apk add --no-cache bash=5.1.16-r0

# Install wget to download dictionaries.
RUN apk add --no-cache wget=1.21.2-r2

ENV SBT_VERSION 1.7.0

# Install sbt
RUN mkdir -p "$SBT_HOME" && \
    wget -qO - --no-check-certificate "https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz" |  tar xz -C $INSTALL_DIR && \
    echo "- with sbt $SBT_VERSION" >> /root/.built


# Cache dependencies
COPY project project
COPY build.sbt build.sbt
COPY coursier_cache /root/.cache/coursier/v1/
RUN sbt compile