FROM openjdk:17-jdk as builder

COPY . .
RUN chmod +x gradlew
RUN ./gradlew installDist --no-daemon 

FROM openjdk:17

WORKDIR /app/
COPY --from=builder build/install/SickBot/ .

CMD ["./bin/SickBot"]
