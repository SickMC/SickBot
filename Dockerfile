FROM openjdk:17-slim AS builder

WORKDIR "/usr/src/SickBot/"
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew installDist --no-daemon

FROM opendjk:17-jre-slim

WORKDIR /bot/
COPY --from=builder /usr/src/SickBot/build/install/SickBot/ .

CMD ["./bin/SickBot"]
