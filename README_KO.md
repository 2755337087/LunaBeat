<div align="center">

# LunaBeat

**재생할 때마다 멜로디와 가사가 만납니다.**

Android 로컬 음악 플레이어 · 단어별 가사 편집 · 음악 라이브러리와 태그 관리

[최신 버전 다운로드](https://github.com/2755337087/LunaBeat/releases/latest) · [사용 안내](#quick-start) · [문제 신고](https://github.com/2755337087/LunaBeat/issues) · [커뮤니티](#community-and-feedback)

</div>

[简体中文](https://github.com/2755337087/LunaBeat/blob/main/README.md) | [繁體中文](https://github.com/2755337087/LunaBeat/blob/main/README_ZH_TW.md) | [English](https://github.com/2755337087/LunaBeat/blob/main/README_EN.md) | [日本語](https://github.com/2755337087/LunaBeat/blob/main/README_JA.md) | [Türkçe](https://github.com/2755337087/LunaBeat/blob/main/README_TR.md) | [한국어](https://github.com/2755337087/LunaBeat/blob/main/README_KO.md) | [Русский](https://github.com/2755337087/LunaBeat/blob/main/README_RU.md) | [Bahasa Indonesia](https://github.com/2755337087/LunaBeat/blob/main/README_ID.md) | [Tiếng Việt](https://github.com/2755337087/LunaBeat/blob/main/README_VI.md) | [ไทย](https://github.com/2755337087/LunaBeat/blob/main/README_TH.md) | [Español](https://github.com/2755337087/LunaBeat/blob/main/README_ES.md) | [हिन्दी](https://github.com/2755337087/LunaBeat/blob/main/README_HI.md) | [Português](https://github.com/2755337087/LunaBeat/blob/main/README_PT.md) | [Français](https://github.com/2755337087/LunaBeat/blob/main/README_FR.md) | [Deutsch](https://github.com/2755337087/LunaBeat/blob/main/README_DE.md) | [العربية](https://github.com/2755337087/LunaBeat/blob/main/README_AR.md)

---

LunaBeat는 로컬 음악 재생, 단어별 가사 제작, 음악 라이브러리 정리를 하나의 앱에 담았습니다. 편안하게 음악을 들으며 멜로디에 따라 펼쳐지는 가사를 감상하거나, 노래의 가사를 단어로 나누고 타이밍을 맞추고 번역과 듀엣 정보를 추가한 뒤 자신만의 가사 작업 흐름에 맞게 내보낼 수 있습니다.

## 화면 미리보기

### 스마트폰 화면

<p align="center">
  <img src="Screenshots/phone.png" alt="LunaBeat · 스마트폰 화면" width="960" />
</p>

### 태블릿 화면

<p align="center">
  <img src="Screenshots/pad.png" alt="LunaBeat · 태블릿 화면" width="960" />
</p>

## 주요 기능

| 용도 | 할 수 있는 작업 |
| --- | --- |
| 로컬 음악 감상 | 로컬 오디오를 검색하고 아티스트, 앨범, 재생목록별로 탐색합니다. 백그라운드 재생, 시스템 미디어 제어, 재생 대기열, 종료 타이머를 지원합니다. |
| 몰입감 있는 가사 감상 | 밝은 테마와 어두운 테마, 재생 제어 막대와 배경 설정으로 단어별 가사를 미리 봅니다. 홈 화면, 상태 표시줄, 차량 Bluetooth 연결을 통한 가사 표시를 지원합니다. |
| 단어별 타이밍 제작 | 시작 / 계속 / 종료 버튼으로 타이밍을 설정하고 재생 속도, 시간 이동, 따라가기 모드, 실행 취소와 다시 실행으로 세밀하게 조정합니다. |
| 가사 편집 | 단어 분할, 가사 나누기와 합치기, 행 이동, 타임스탬프 일괄 이동, 중국어 간체·번체 변환, 빈 행 삭제, 번역·듀엣·코러스 설정을 지원합니다. |
| 음악 정리 | 재생목록, 즐겨찾기, 별 다섯 개 곡을 관리합니다. 표지·태그·가사를 일괄 매칭하고 메타데이터를 편집하며 파일 이름을 변경합니다. |

### 가져오기와 내보내기

| 내용 | 가져오기 | 내보내기 |
| --- | --- | --- |
| 일반 텍스트 가사 | 지원 | — |
| LRC(행별 / 단어별) | 지원 | 지원 |
| 확장 LRC(ELRC) | 지원 | 지원 |
| TTML | 지원 | 지원, 듀엣과 코러스 정보 유지 |

가사를 검색하거나 오디오를 가져와 제작을 시작할 수도 있습니다. 듀엣과 코러스 같은 구조 정보를 유지하려면 TTML을, 일반적인 가사 표시에는 LRC를 권장합니다.

<a id="quick-start"></a>

## 빠른 시작

### 음악 감상과 라이브러리 정리

1. 음악 라이브러리를 열고 기기의 로컬 오디오를 검색합니다.
2. 아티스트, 앨범, 재생목록 또는 즐겨찾기를 탐색하고 곡을 눌러 재생합니다.
3. 전체 플레이어를 열어 재생 모드를 바꾸고 대기열을 관리하거나 가사를 보고 종료 타이머를 설정합니다.
4. 파일을 정리하려면 태그와 가사를 일괄 매칭한 뒤 태그를 편집하거나 파일 이름을 변경합니다.

### 한 곡의 단어별 가사 제작

1. **가져오기**: 오디오를 선택한 뒤 일반 텍스트, LRC, ELRC 또는 TTML 가사를 가져옵니다. 가사를 검색할 수도 있습니다.
2. **준비**: 먼저 단어를 나눈 뒤 빈 행을 삭제하거나 가사를 합치거나 중국어 간체·번체를 변환합니다. 기존 타이밍이 있다면 먼저 전체 타임라인을 이동해 맞춥니다.
3. **타이밍 설정**: 오디오를 재생하고 가사 한 행의 리듬에 맞춰 아래 세 버튼을 사용합니다.
4. **세부 조정**: 가사를 두 번 눌러 해당 부분으로 이동해 다시 듣습니다. 따라가기 모드, 실행 취소와 다시 실행으로 시간을 확인합니다. 가사 단위나 행을 길게 누르면 나누기, 합치기, 이동, 번역 편집을 할 수 있습니다.
5. **미리보기와 내보내기**: 단어별 효과를 확인합니다. 일반 표시에는 LRC를, 듀엣과 코러스 정보가 필요하면 TTML을 선택합니다.

| 버튼 | 동작 |
| --- | --- |
| 시작 | 현재 단위의 시작 시간을 설정하고 현재 단위에 머무릅니다. |
| 계속 | 현재 단위의 종료 시간을 설정하고 다음 단위로 이동하며, 다음 단위의 시작 시간도 설정합니다. |
| 종료 | 현재 단위의 종료 시간을 설정하고 다음 단위로 이동합니다. 다음 단위의 시작 시간은 나중에 설정합니다. |

권장 순서: `시작 → 계속 → 계속 → … → 종료`를 각 행마다 반복합니다.

<details>
<summary>타이밍 설정 팁</summary>

- 시간과 텍스트 열에는 시작 시간, 가사, 종료 시간, 번역이 표시됩니다.
- 가사 단위를 한 번 누르면 선택하고, 두 번 누르면 해당 단위의 시작 시간으로 이동해 다시 들을 수 있습니다.
- 되감기 / 빨리 감기, 재생 / 일시 정지, 재생 속도 조절, 지정 시간으로 이동을 지원합니다.
- 재생 시간을 누르면 따라가기 모드가 켜지고, 타이밍 영역의 시간을 누르면 이동할 위치를 지정할 수 있습니다.
- 듀엣은 왼쪽(주 보컬), 오른쪽(두 번째 보컬), 배경(코러스)으로 설정하고 TTML로 내보낼 수 있습니다.

</details>

## 자주 묻는 질문

<details>
<summary>LRC와 TTML 중 무엇을 선택해야 하나요?</summary>

일반적인 가사 표시에는 LRC를, 듀엣과 코러스 같은 구조 정보를 유지하려면 TTML을 선택하세요. 내보내기 전에 미리보기에서 단어별 효과를 확인할 수 있습니다.

</details>

<details>
<summary>듀엣 가사를 내보내거나 재생할 때 무엇을 주의해야 하나요?</summary>

듀엣과 코러스 정보는 TTML로 내보내는 것을 우선 권장하며, 해당 효과를 지원하는 플레이어에서 확인하세요. 실제 표시 결과는 플레이어의 가사 형식 및 관련 정보 지원 여부에 따라 달라집니다.

</details>

<details>
<summary>가사 전체가 빠르거나 늦게 나오면 어떻게 조정하나요?</summary>

타임스탬프를 일괄 이동해 전체 타임라인을 먼저 맞춘 뒤 타이밍 화면으로 돌아가 부분별 시간을 세밀하게 조정하세요.

</details>

<details>
<summary>처음 타이밍을 맞춘다면 어디서 시작해야 하나요?</summary>

단어 분할과 텍스트 정리를 마친 뒤 시작하세요. 익숙한 곡을 선택하고 각 행마다 “시작 → 계속 → … → 종료”를 사용한 다음 다시 들으며 조금씩 조정하세요.

</details>

<a id="community-and-feedback"></a>

## 커뮤니티와 피드백

- [GitHub Issues · 버그 신고와 기능 제안](https://github.com/2755337087/LunaBeat/issues)
- [Telegram · LunaBeat 커뮤니티 그룹](https://t.me/+qXs6mjKqwhw3Zjll)
- [QQ · LunaBeat 커뮤니티 그룹](https://qm.qq.com/q/N0fBvuWKOY) · 그룹 번호: **964680520**

문제를 신고할 때는 원인 파악을 위해 앱 버전, 기기와 Android 버전, 재현 단계, 관련 스크린샷이나 파일 형식을 가능하면 함께 알려 주세요.

## 특별히 감사드립니다

### 기술 지원

- [TagLib](https://github.com/taglib/taglib)
- [Lyricon](https://github.com/tomakino/lyricon)
- [OpenCC](https://github.com/BYVoid/OpenCC)
- [any-listen-extension-online-metadata](https://github.com/any-listen/any-listen-extension-online-metadata)
- [163Music2Tag](https://gitee.com/Wangs-official/163Music2Tag)
- [TagLib(Kyant0)](https://github.com/Kyant0/taglib)
- [uCrop](https://github.com/Yalantis/uCrop)
- [accompanist-lyrics-ui](https://github.com/6xingyv/accompanist-lyrics-ui)
- [lottie-android](https://github.com/airbnb/lottie-android)
- [ICU Transliterator](https://unicode-org.github.io/icu/userguide/transforms/general/)
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)
- [AMLLTTML歌词站](https://amlldb.bikonoo.com/)
- [LunaBeat歌词站](https://github.com/2755337087/ttml-hub/blob/main/README.md)

### 영감을 준 프로젝트

- [amll-ttml-tool](https://github.com/amll-dev/amll-ttml-tool)
- [音乐标签](https://www.cnblogs.com/vinlxc/p/11932130.html)
- [光锥音乐](https://coneplayer.trantor.ink/)
- [Halcyon](https://github.com/Kifranei/Halcyon)
- [Flamingo](https://github.com/Yos-X)
