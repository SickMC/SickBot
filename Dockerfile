FROM openjdk:17-jdk as builder

COPY . .
WORKDIR /src
RUN ./gradlew --no-daemon installDist

FROM openjdk:17

COPY --from=builder build/install/SickBot/ .

CMD ["./bin/SickBot"]
