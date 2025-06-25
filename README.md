# Starbucks Map App

スターバックスを地図上で探し、訪れた店舗のスタンプを収集できるAndroidアプリです。

## 特徴
- 現在地周辺のスターバックス店舗をGoogle Maps上に表示
- 店舗の情報ウィンドウをタップしてスタンプを獲得
- 集めたスタンプは一覧画面で確認可能

## セットアップ
1. Google Maps & Places APIキーを取得します。
2. `app/src/main/AndroidManifest.xml` と `app/src/main/res/values/strings.xml` の `YOUR_API_KEY` を取得したキーに置き換えます。
3. Android Studio または `./gradlew assembleDebug` を使ってビルドします。

## 使い方
アプリを起動すると現在地周辺の地図が表示されます。スターバックスのマーカーをタップし、表示されるウィンドウを押すとスタンプが保存されます。画面右下の **Stamps** ボタンからスタンプ一覧を確認できます。
