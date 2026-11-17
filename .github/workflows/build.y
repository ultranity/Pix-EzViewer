name: Build APK
on:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'

      - name: 生成临时签名
        run: |
          keytool -genkeypair -v -keystore keystore.jks -alias dsh \
            -keyalg RSA -keysize 2048 -validity 10000 \
            -storepass android -keypass android -dname "CN=DSH,O=DSH,C=CN"
          printf 'storeFile=../keystore.jks\nstorePassword=android\nkeyAlias=dsh\nkeyPassword=android\n' > keystore.properties

      - uses: gradle/actions/setup-gradle@v4

      - name: 编译
        run: ./gradlew assembleGitRelease

      - uses: actions/upload-artifact@v4
        with:
          name: PixEzViewer-apk
          path: app/build/outputs/apk/git/release/*.apk
