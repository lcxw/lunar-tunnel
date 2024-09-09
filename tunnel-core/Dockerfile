FROM openjdk:21
#ARG JAR_FILE=target/*.jar
ARG JAR_FILE=build/libs/*.jar
WORKDIR app
COPY --chown=1001:0 ${JAR_FILE} application.jar

#ENV JVM_OPTS="-Xmx512m -Xss256k"

#RUN addgroup -S app && adduser -S app -G app
#USER app:app
#COPY --from=builder app/dependencies/ ./
#COPY --from=builder app/snapshot-dependencies/ ./
#COPY --from=builder app/spring-boot-loader/ ./
#COPY --from=builder app/application/ ./
ENV LANG C.UTF-8
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ENV JAVA_OPTS=""
#ENV JAVA_OPTS="-Dspring.profiles.active=k8s"
#ENV JVM_OPTS="-XX:MaxRAMPercentage=80.0 -Dspring.cloud.nacos.config.server-addr=rnacos -Dspring.cloud.nacos.discovery.server-addr=rnacos"
ENV JVM_OPTS="-XX:MaxRAMPercentage=80.0"
EXPOSE 23000
#ENTRYPOINT ["sh","-c","java $JVM_OPTS $JAVA_OPTS -jar /app.jar"]
#ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","org.springframework.boot.loader.JarLauncher"]
ENTRYPOINT ["sh","-c","java $JVM_OPTS $JAVA_OPTS -jar /app/application.jar"]
#ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar $APP_OPTS" ]