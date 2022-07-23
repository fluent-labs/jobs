FROM amazoncorretto:18-alpine3.15

LABEL maintainer="reader@lucaskjaerozhang.com"
WORKDIR /app
ENV INSTALL_DIR /usr/local
ENV SBT_HOME /usr/local/sbt
ENV S3CMD_DIR $INSTALL_DIR/s3cmd-2.2.0
ENV PATH ${PATH}:${SBT_HOME}/bin:$S3CMD_DIR

# Needed because sbt will barf if we don't have one
# Even though we aren't actually accessing the maven package repository
ENV GITHUB_TOKEN=faketoken

# Keep failing pipe command from reporting success to the build.
SHELL ["/bin/ash", "-eo", "pipefail", "-c"]

# Sbt requires this for some reason
RUN apk add --no-cache bash=5.1.16-r0

# Install wget to download dictionaries and full tar to open them
RUN apk add --no-cache wget=1.21.2-r2 tar=1.34-r0

# Used to upload the spark job to s3
RUN apk add --no-cache python3=3.9.7-r4 && \
  wget -qO - --no-check-certificate "https://github.com/s3tools/s3cmd/releases/download/v2.2.0/s3cmd-2.2.0.tar.gz" |  tar xz -C $INSTALL_DIR && \
  cd $S3CMD_DIR && \
  python setup.py install

ENV SBT_VERSION 1.7.0

# Install sbt
RUN mkdir -p "$SBT_HOME" && \
    wget -qO - --no-check-certificate "https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz" |  tar xz -C $INSTALL_DIR && \
    echo "- with sbt $SBT_VERSION" >> /root/.built

# Cache dependencies
COPY . .
COPY coursier_cache /root/.cache/coursier/v1/
RUN sbt assembly