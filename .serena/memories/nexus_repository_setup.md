# Nexus Repository 설정 가이드

## 추가된 Nexus 설정 위치

### 1. build.gradle (루트)
- **repositories 섹션**: Nexus에서 패키지 다운로드 설정
- **publishing 섹션**: Nexus에 패키지 업로드 설정  
- 모든 설정이 주석 처리되어 있어 필요시 주석 해제하여 사용

### 2. gradle.properties
- Nexus 인증 정보 설정 템플릿
- 환경변수 사용법 안내
- Docker 환경 설정 포함

### 3. README.md
- Nexus 사용법 섹션 추가
- 단계별 설정 가이드
- 명령어 예시 제공

## 사용 방법
1. **build.gradle**: Nexus 설정 주석 해제 및 URL 변경
2. **gradle.properties**: 인증 정보 설정 (또는 환경변수 사용)
3. **배포**: `./gradlew publish` 명령어로 패키지 배포

## 보안 고려사항
- gradle.properties를 .gitignore에 추가 권장
- 환경변수 사용 권장 (더 안전)
- HTTPS 사용 권장 (현재는 HTTP 설정)