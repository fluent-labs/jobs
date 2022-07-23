FROM amazoncorretto:18-alpine3.15

LABEL maintainer="reader@lucaskjaerozhang.com"
WORKDIR /app
ENV INSTALL_DIR /usr/local
ENV SBT_HOME /usr/local/sbt
ENV S3CMD_DIR $INSTALL_DIR/s3cmd-2.2.0
ENV PATH ${PATH}:${SBT_HOME}/bin:$S3CMD_DIR

# Keep failing pipe command from reporting success to the build.
SHELL ["/bin/ash", "-eo", "pipefail", "-c"]

# Sbt requires bash for some reason
# Install wget to download dictionaries
# Python for s3cmd
RUN apk add --no-cache bash=5.1.16-r0 \
    wget=1.21.2-r2 \
    python3=3.9.7-r4 \
    py3-setuptools=52.0.0-r4

# Used to upload the spark job to s3
RUN wget -qO - --no-check-certificate "https://github.com/s3tools/s3cmd/releases/download/v2.2.0/s3cmd-2.2.0.tar.gz" |  tar xz -C $INSTALL_DIR && \
  cd $S3CMD_DIR && \
  python3 setup.py install

ENV SBT_VERSION 1.7.0

# Install sbt
RUN mkdir -p "$SBT_HOME" && \
    wget -qO - --no-check-certificate "https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz" |  tar xz -C $INSTALL_DIR && \
    echo "- with sbt $SBT_VERSION" >> /root/.built