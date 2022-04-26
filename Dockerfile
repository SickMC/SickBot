FROM openjdk:17-jdk as builder

COPY . .
WORKDIR /src
RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon installDist

FROM openjdk:17-jre

COPY --from=builder build/install/SickBot/ .

CMD ["./bin/SickBot"]
