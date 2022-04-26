FROM openjdk:17-slim AS builder

WORKDIR "/usr/src/SickBot/"
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew installDist --no-daemon

COPY . /bot

CMD ["./bin/SickBot"]
