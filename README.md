# 🚀 LoadLensAI

> 실시간 과적 차량 단속 솔루션

<br>

## 📖 목차

1. [프로젝트 소개](#-프로젝트-소개)
2. [주요 기능](#-주요-기능)
3. [미리보기](#%EF%B8%8F-미리보기)
4. [사용 기술](#%EF%B8%8F-사용-기술)
5. [설치 및 실행 방법](#%EF%B8%8F-설치-및-실행-방법)
6. [트러블 슈팅](#%E2%80%8D%E2%80%8D%E2%80%8D-팀원-소개)
   
<br>

## 📌 프로젝트 소개

모바일 온디바이스(On-device) AI를 활용하여 실시간으로 과적 차량을 탐지하는 안드로이드 애플리케이션입니다.
서버 통신 없이 스마트폰 자체 성능만으로 트럭의 과적 여부를 판별하여, 단속 현장에서의 효율성과 안전을 높이는 것을 목표로 합니다.

<br>

## ✨ 주요 기능

- **실시간 탐지:** 카메라 프리뷰 화면에서 즉각적인 객체 인식 (30fps 이상)  
- **과적 분류:** 차종과 과적 여부를 결합한 6가지 클래스 판별
- **오프라인 작동:** 인터넷 연결 없이 스마트폰 자체(On-device)에서 독립적으로 수행

<br>

## 🖼️ 미리보기
<img width="425" height="931" alt="image" src="https://github.com/user-attachments/assets/5b2cbf48-3b80-4622-8f79-59db09d07c1c" />

<br>

## 🛠️ 사용 기술

- **AI Model:** YOLOv8 (Trained on Custom Dataset -> .tflite Quantization)
- **Mobile:** Android (Kotlin), CameraX
- **Inference:** TensorFlow Lite (GPU Delegate)
- **Environment:** Python 3.9, Android Studio
<br>

## ⚙️ 설치 및 실행 방법

```bash
# 1. 이 저장소를 클론합니다.
# 2. Android Studio (Iguana 이상)에서 프로젝트를 엽니다.
# 3. Android 기기를 연결하고 실행합니다. (Min SDK: 26)
```

<br>

## 👨‍👩‍👧‍👦 트러블 슈팅

| 이름 | 역할 |  
| 이윤기 | Android 앱 구현 / DB 설계 및 API 개발 |  
| 최광민 | 디자인 시안 및 전체 UI 흐름 기획 |  
| 홍정원 | 음악 추천 로직 구현 / 챗봇 메시지 처리 |  
