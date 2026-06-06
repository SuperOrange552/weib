#!/usr/bin/env python3
"""快速对比测试：登录页 vs 首页"""
import urllib.request, threading, time, statistics

def quick_test(url, concurrency, duration=10):
    results = {"ok": 0, "fail": 0, "lats": []}
    stop = threading.Event()
    lock = threading.Lock()

    def worker():
        opener = urllib.request.build_opener()
        while not stop.is_set():
            start = time.time()
            try:
                req = urllib.request.Request(url, headers={"User-Agent": "Test", "Connection": "close"})
                with opener.open(req, timeout=10) as resp:
                    resp.read()
                    with lock:
                        results["ok"] += 1
                        results["lats"].append((time.time() - start) * 1000)
            except:
                with lock:
                    results["fail"] += 1

    threads = [threading.Thread(target=worker, daemon=True) for _ in range(concurrency)]
    for t in threads: t.start()
    time.sleep(duration)
    stop.set()
    for t in threads: t.join(timeout=2)

    rps = results["ok"] / duration
    if results["lats"]:
        sl = sorted(results["lats"])
        avg, p50, p95, p99 = statistics.mean(sl), sl[len(sl)//2], sl[int(len(sl)*0.95)], sl[int(len(sl)*0.99)]
    else:
        avg = p50 = p95 = p99 = 0
    return rps, avg, p50, p95, p99, results["ok"], results["fail"]

levels = [10, 20, 40]
endpoints = [
    ("/login", "登录页(无DB)"),
    ("/", "首页(有DB)"),
]

for label, desc in endpoints:
    url = f"http://superorange.top:8080{label}"
    print(f"\n{'='*60}")
    print(f"  {desc} - {url}")
    print(f"{'='*60}")
    print(f"{'并发':>5s} | {'RPS':>8s} | {'avg':>6s} | {'P50':>6s} | {'P95':>6s} | {'P99':>6s} | {'成功':>5s} | {'失败':>3s}")
    print("-" * 80)
    for c in levels:
        rps, avg, p50, p95, p99, ok, fail = quick_test(url, c)
        print(f"  {c:3d}  | {rps:7.1f}/s | {avg:5.0f}ms | {p50:5.0f}ms | {p95:5.0f}ms | {p99:5.0f}ms | {ok:5d} | {fail:3d}")
        time.sleep(3)
