# 1단계: jar 빌드
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 캐시를 위해 Gradle 설정을 먼저 복사한다
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null

COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# 2단계: 실행
FROM eclipse-temurin:21-jre
WORKDIR /app

# 영수증 HEIC → JPG 변환(IngredientService)에 ImageMagick이 필요하다.
# 패키지가 ImageMagick 6이면 `magick` 명령이 없으므로 그때만 `convert`로 연결한다.
RUN apt-get update \
    && apt-get install -y --no-install-recommends imagemagick libheif1 \
    && rm -rf /var/lib/apt/lists/* \
    && if ! command -v magick > /dev/null; then \
         printf '#!/bin/sh\nexec convert "$@"\n' > /usr/local/bin/magick && chmod +x /usr/local/bin/magick; \
       fi

COPY --from=build /app/build/libs/*.jar app.jar

# application.properties는 이미지에 넣지 않고 실행 시 /app/config/에 마운트한다
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
