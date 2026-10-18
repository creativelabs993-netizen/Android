# Run the CoinForge prototype on an Android phone (Termux)

This runs the experimental Python test-chain service on the phone itself. It is a development demo only, **not Bitcoin Core, not the CoinForge production network, and not a coin miner that earns rewards**. It has no persistence; blocks disappear when the process stops.

## 1. Install Termux

Install Termux from its official F-Droid listing or official GitHub releases. Avoid unofficial APK download sites.

## 2. Install Python and Git

Open Termux and run:

```sh
pkg update
pkg install python git
```

## 3. Download this branch

```sh
git clone --branch coinforge-testnet-prototype https://github.com/creativelabs993-netizen/Android.git
cd Android/coinforge-node
```

## 4. Start the local test service

```sh
python prototype.py
```

The server binds to `127.0.0.1:19444` by default. Keep Termux open while testing. Do not change the bind address to `0.0.0.0` or expose this prototype to the internet.

## 5. Check it

Open the browser on the same phone and visit:

- `http://127.0.0.1:19444/health` — reports the test-chain height and explicitly states that rewards are disabled.
- `http://127.0.0.1:19444/work` — returns a test work template.

The current Android APK does **not** connect to this service yet. This guide only runs the backend locally and lets you inspect its endpoints. The next step is to integrate a mining client into the APK and verify that the app submits a valid nonce and the server accepts the block.

## Safety and limitations

- No monetary rewards, wallet, persistent blockchain database, peer-to-peer network, or Bitcoin Core compatibility.
- Do not use this as a public server or trust it with funds.
- Android may stop Termux in the background; keep it open for the demo.
