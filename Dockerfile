FROM openjdk:17-jdk as builder

COPY . /src
WORKDIR /src
RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon installDist

FROM openjdk:17-jre

COPY --from=builder build/install/SickBot .

ENTRYPOINT ["bin/SickBot"]
