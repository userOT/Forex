FROM openjdk:8
ENV JAVA_HOME /usr/lib/jvm/java-1.8-openjdk
ENV PATH $PATH:/usr/lib/jvm/java-1.8-openjdk/jre/bin:/usr/lib/jvm/java-1.8-openjdk/bin

ENV SBT_VERSION 1.2.1

# Install curl
RUN \
  apt-get update && \
  apt-get -y install curl && \
  apt-get -y install vim

# Install sbt
RUN \
  curl -L -o sbt-$SBT_VERSION.deb https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get -y install sbt

WORKDIR /src/main/java
COPY . /src/main/java
RUN sbt update
EXPOSE 9000
ENTRYPOINT ["sbt", "run"]