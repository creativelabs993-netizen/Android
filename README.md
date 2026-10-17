# CoinForge Android

Native Android project and GitHub Actions APK build.

## Current status — prototype, not a miner

This repository builds an Android APK shell, but **it does not mine CoinForge coins**. A real miner requires a working CoinForge consensus implementation, a deployed and reachable peer network, valid block templates, and a way to submit accepted blocks. Those pieces have not yet been built or verified. A local CPU hash loop or simulated balance would not be real mining, so this app intentionally does not claim to mine or award coins.

## Build the APK

1. Open the **Actions** tab.
2. Select **Build CoinForge Android APK**.
3. Click **Run workflow**.
4. When it completes successfully, open the run and download the `coinforge-android-debug` artifact.

The resulting debug APK is a prototype status client, not a production release or mining software. Do not treat CoinForge tokens as having monetary value.
