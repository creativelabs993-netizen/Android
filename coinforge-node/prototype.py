#!/usr/bin/env python3
"""Experimental single-node CoinForge PoW prototype. NOT Bitcoin consensus.

This is deliberately a private test harness: no wallet, P2P networking, economic
value, persistence, or production security. Do not expose it to the public internet.
"""
from __future__ import annotations

import hashlib
import json
import os
import secrets
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any

# Easy target for local end-to-end testing only. Not a production difficulty.
TARGET_HEX_PREFIX = os.environ.get("COINFORGE_TEST_TARGET_PREFIX", "0000")
CHAIN_ID = "coinforge-private-pow-prototype-v0"
MAX_PENDING = 256


def canonical_bytes(value: dict[str, Any]) -> bytes:
    return json.dumps(value, sort_keys=True, separators=(",", ":")).encode("utf-8")


def hash_header(header: dict[str, Any]) -> str:
    first = hashlib.sha256(canonical_bytes(header)).digest()
    return hashlib.sha256(first).hexdigest()


def meets_target(digest_hex: str, prefix: str = TARGET_HEX_PREFIX) -> bool:
    return digest_hex.startswith(prefix)


class TestChain:
    def __init__(self, target_prefix: str = TARGET_HEX_PREFIX):
        if not target_prefix or any(c not in "0123456789abcdef" for c in target_prefix):
            raise ValueError("target prefix must be lowercase hexadecimal")
        self.target_prefix = target_prefix
        self.lock = threading.RLock()
        self.blocks: list[dict[str, Any]] = []
        self.pending: dict[str, dict[str, Any]] = {}

    def issue_work(self, miner: str = "android-test-miner") -> dict[str, Any]:
        if not miner or len(miner) > 128:
            raise ValueError("miner label must be 1..128 characters")
        with self.lock:
            tip = self.blocks[-1]["hash"] if self.blocks else "0" * 64
            header = {
                "chain_id": CHAIN_ID,
                "height": len(self.blocks),
                "previous_hash": tip,
                "timestamp": int(time.time()),
                "miner": miner,
            }
            work_id = secrets.token_hex(16)
            if len(self.pending) >= MAX_PENDING:
                self.pending.pop(next(iter(self.pending)))
            self.pending[work_id] = header
            return {
                "chain_id": CHAIN_ID,
                "work_id": work_id,
                "header": header,
                "target_hex_prefix": self.target_prefix,
                "height": header["height"],
            }

    def submit(self, work_id: str, nonce: int) -> dict[str, Any]:
        if not isinstance(nonce, int) or isinstance(nonce, bool) or not 0 <= nonce <= 0xFFFFFFFFFFFFFFFF:
            return {"accepted": False, "error": "nonce must be an unsigned 64-bit integer"}
        with self.lock:
            header = self.pending.get(work_id)
            if header is None:
                return {"accepted": False, "error": "unknown or expired work"}
            candidate = dict(header, nonce=nonce)
            digest = hash_header(candidate)
            if not meets_target(digest, self.target_prefix):
                return {"accepted": False, "error": "proof of work does not meet target", "hash": digest}
            # Reject stale work if another candidate already extended the tip.
            current_tip = self.blocks[-1]["hash"] if self.blocks else "0" * 64
            if candidate["previous_hash"] != current_tip or candidate["height"] != len(self.blocks):
                self.pending.pop(work_id, None)
                return {"accepted": False, "error": "stale work; request a new template", "hash": digest}
            block = {**candidate, "hash": digest}
            self.blocks.append(block)
            self.pending.clear()
            return {"accepted": True, "block": block, "height": block["height"], "reward": 0,
                    "note": "test block accepted; no coin reward or monetary value exists"}

    def status(self) -> dict[str, Any]:
        with self.lock:
            return {
                "ok": True,
                "chain_id": CHAIN_ID,
                "mode": "private-test-prototype",
                "height": len(self.blocks),
                "tip_hash": self.blocks[-1]["hash"] if self.blocks else "0" * 64,
                "target_hex_prefix": self.target_prefix,
                "coin_rewards_enabled": False,
                "bitcoin_core_compatible": False,
            }


def make_handler(chain: TestChain):
    class Handler(BaseHTTPRequestHandler):
        server_version = "CoinForgePrototype/0.1"

        def _send(self, code: int, payload: dict[str, Any]) -> None:
            data = json.dumps(payload).encode("utf-8")
            self.send_response(code)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(data)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(data)

        def do_GET(self):
            if self.path == "/health":
                self._send(200, chain.status())
            elif self.path == "/work":
                self._send(200, chain.issue_work())
            else:
                self._send(404, {"error": "not found"})

        def do_POST(self):
            if self.path != "/submit":
                self._send(404, {"error": "not found"})
                return
            try:
                length = int(self.headers.get("Content-Length", "0"))
                if length < 2 or length > 4096:
                    self._send(400, {"error": "invalid request size"})
                    return
                body = json.loads(self.rfile.read(length))
                if not isinstance(body, dict) or not isinstance(body.get("work_id"), str):
                    self._send(400, {"error": "expected work_id and nonce"})
                    return
                result = chain.submit(body["work_id"], body.get("nonce"))
                self._send(200 if result.get("accepted") else 400, result)
            except (ValueError, json.JSONDecodeError):
                self._send(400, {"error": "invalid JSON"})
        def log_message(self, fmt, *args):
            print("%s - %s" % (self.client_address[0], fmt % args), flush=True)
    return Handler


def main():
    # Bind to localhost by default; change only for a trusted private test LAN.
    host = os.environ.get("COINFORGE_BIND", "127.0.0.1")
    port = int(os.environ.get("COINFORGE_PORT", "19444"))
    chain = TestChain()
    server = ThreadingHTTPServer((host, port), make_handler(chain))
    print(f"{CHAIN_ID} listening on http://{host}:{port}; test-only, no rewards", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
