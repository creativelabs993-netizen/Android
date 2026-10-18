# CoinForge Bitcoin Core Fork — Implementation Plan

## Status

**Planning only. No CoinForge node or live network exists yet.** This document is not evidence of a deployed chain, valid genesis block, or available coin rewards.

The goal is a Bitcoin-derived CoinForge full node and an Android miner that submits work the node validates. Prefer a maintained upstream Bitcoin Core release and a small, auditable patch set over reimplementing consensus from scratch.

## Non-negotiable acceptance criteria

Do not describe CoinForge as mineable or credit rewards until all of these pass:

1. A clean, reproducible build of the pinned upstream source plus CoinForge patches.
2. A deterministic genesis block whose header/hash/merkle root are independently checked and committed as test vectors.
3. Consensus tests for block header serialization, double-SHA-256 proof-of-work, target/compact-target handling, chainwork, subsidy/halving, transaction validity, and difficulty adjustment.
4. Two or more independent nodes connect, exchange blocks, reject invalid blocks, and converge on the same best chain.
5. Mining tests create a valid block template, find a nonce meeting the actual target, submit the block, and verify that the node accepts it.
6. The Android miner uses the exact header serialization and algorithm expected by the node, and displays a reward only after acceptance.
7. A security review of network/RPC exposure, wallet keys, and reward accounting.

## Proposed implementation sequence

### Phase A — Pin and build upstream

- Select a supported Bitcoin Core release and record its exact tag and commit.
- Preserve upstream license and notices.
- Add a reproducible CI build and test job before modifying consensus.
- Keep CoinForge changes isolated and documented; avoid broad search-and-replace edits.

### Phase B — Define the network before coding consensus

Record and review these parameters in a versioned specification:

- Network name and unique message-start bytes / chain identifier.
- P2P and RPC ports; data-directory name.
- Address encoding and wallet derivation prefixes.
- Genesis timestamp, initial transaction/coinbase data, nonce, bits, merkle root, and resulting block hash.
- Proof-of-work algorithm, maximum target, target spacing, difficulty adjustment algorithm, and minimum-difficulty rules.
- Initial subsidy, subsidy units, halving interval, and maximum-money rules.
- Transaction relay and block-size/weight limits.
- Testnet/regtest parameters distinct from any future public network.

**Draft values from earlier discussion are unverified and must not be treated as final:** P2P 19444, RPC 19443, address prefix `cf`, initial subsidy 50 CF, halving interval 210,000 blocks, and proposed issuance ceiling 20,999,999.9769 CF. A previously mentioned genesis hash is also unverified and must be recomputed from the final genesis fields. Do not expose an RPC port publicly.

### Phase C — Implement and test chain parameters

- Add dedicated CoinForge chain parameters and unique network identifiers.
- Generate the genesis block from the approved fields; verify the hash meets the configured target.
- Add unit/regression tests for all changed consensus rules.
- Add separate regtest/testnet configurations so testing cannot accidentally use public-network settings.
- Run upstream and CoinForge test suites in CI.

### Phase D — Private multi-node test network

- Run at least two independently configured nodes.
- Verify peer discovery/connectivity, propagation, reorg behavior, invalid-block rejection, and restart/reindex behavior.
- Mine and validate test blocks using node-generated templates.
- Test subsidy accounting and coinbase maturity with a wallet.

### Phase E — Android integration

- Do not expose unrestricted Bitcoin Core RPC credentials in the APK or ship a wallet private key to a server.
- Define an authenticated mining protocol or a pool/Stratum-compatible gateway that issues templates and accepts submissions.
- Use the exact CoinForge block-header encoding and proof-of-work hash.
- Count only node-accepted blocks; distinguish submitted shares from accepted blocks.
- Add stop controls, thermal/battery throttling, reconnect logic, and clear warnings that phone CPU mining may be uncompetitive against specialized hardware.

## Launch gates

No public launch or monetary reward claims until consensus parameters are reviewed, independent nodes interoperate, security testing is complete, and network/reward operation is independently verifiable. A private test chain is for testing only; test coins have no promised monetary value.
