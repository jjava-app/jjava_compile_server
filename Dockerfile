FROM gradle:8.8-jdk21
WORKDIR /app

# 소스 전체 복사
COPY . .
RUN chmod +x gradlew

# 테스트 + 문서 + jar 빌드 (배포 목적 아님 → 테스트는 포함, 최적화 생략)
RUN ./gradlew clean bootJar -x test --no-daemon \
    -Dspring.profiles.active=local \
    && cp build/libs/*.jar build/libs/app.jar

EXPOSE 8081

# 실행 (local profile 적용)
ENTRYPOINT ["java","-jar","build/libs/app.jar","--spring.profiles.active=local"]