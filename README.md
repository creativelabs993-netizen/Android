# CoinForge Android

Native Android app and GitHub Actions APK build.

## Current status

The app now includes an **on-device SHA-256 proof-of-work benchmark**. It runs on the phone's CPU, reports hashes/second, and counts hashes meeting a local 20-leading-zero-bit test target.

**This is not CoinForge blockchain mining and does not earn coins.** The repository does not yet contain a verified CoinForge consensus/node implementation, valid network block templates, a peer network, or a block submission path. A benchmark hash is not a valid CoinForge block.

## Phone safety

- Runs only while the app screen is open; it is not a background service.
- Yields the CPU regularly and automatically stops after five minutes.
- Stop immediately if the phone becomes hot. Do not leave it running unattended or while charging.
- Results and local solutions are diagnostic only; no balance or reward is created.

## Next required engineering milestone

Implement and test CoinForge consensus and block validation, then provide the phone miner with a valid block template, the current network target, and a verified block-submission path. Only blocks accepted by the node should count as mined blocks or rewards.

## Build APK

1. Open the **Actions** tab.
2. Select **Build CoinForge Android APK** and click **Run workflow**.
3. After a successful run, download the `coinforge-android-debug` artifact from the workflow run.
