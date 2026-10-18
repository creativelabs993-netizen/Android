import unittest

from prototype import TestChain, hash_header, meets_target


class TestChainTests(unittest.TestCase):
    def test_hash_is_deterministic(self):
        header = {"height": 0, "nonce": 7, "chain_id": "test"}
        self.assertEqual(hash_header(header), hash_header(header))

    def test_rejects_bad_work_then_accepts_valid_work(self):
        chain = TestChain("000")
        work = chain.issue_work("unit-test")
        nonce = 0
        while meets_target(hash_header(dict(work["header"], nonce=nonce)), "000"):
            nonce += 1
        rejected = chain.submit(work["work_id"], nonce)
        self.assertFalse(rejected["accepted"])
        self.assertIn("does not meet target", rejected["error"])
        while not meets_target(hash_header(dict(work["header"], nonce=nonce)), "000"):
            nonce += 1
        result = chain.submit(work["work_id"], nonce)
        self.assertTrue(result["accepted"])
        self.assertEqual(chain.status()["height"], 1)
        self.assertEqual(result["reward"], 0)

    def test_stale_work_rejected(self):
        chain = TestChain("0")
        first = chain.issue_work("miner-a")
        second = chain.issue_work("miner-b")

        def find_nonce(work):
            n = 0
            while not meets_target(hash_header(dict(work["header"], nonce=n)), "0"):
                n += 1
            return n

        self.assertTrue(chain.submit(first["work_id"], find_nonce(first))["accepted"])
        result = chain.submit(second["work_id"], find_nonce(second))
        self.assertFalse(result["accepted"])
        self.assertTrue("stale" in result["error"] or "unknown" in result["error"])

    def test_status_discloses_not_bitcoin_core(self):
        status = TestChain().status()
        self.assertFalse(status["bitcoin_core_compatible"])
        self.assertFalse(status["coin_rewards_enabled"])


if __name__ == "__main__":
    unittest.main()
