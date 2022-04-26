FROM openjdk:17 as Builder

WORKDIR /bot

COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew installDist --no-deamon

COPY . /bot

CMD ["./bin/SickBot"]
