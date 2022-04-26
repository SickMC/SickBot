FROM openjdk:17-slim as builder

COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon installDist

WORKDIR /user/app/
COPY --from=builder build/install/SickBot ./

ENTRYPOINT ["/user/app/bin/SickBot"]
