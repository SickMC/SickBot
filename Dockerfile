FROM openjdk:17-jdk as builder

COPY . .
WORKDIR /user/src/SickBot/
RUN chmod +x gradlew
RUN ./gradlew installDist --no-daemon 

FROM openjdk:17

COPY --from=builder build/install/SickBot/ .

CMD ["./bin/SickBot"]
