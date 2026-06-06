#!/usr/bin/env python3
"""
真实压力测试：每个并发级别持续20秒，连续发送请求
服务器规格：4核4G
"""
import urllib.request
import concurrent.futures
import time
import threading
import statistics
import sys

URL = "http://superorange.top:8080/"
DURATION = 20       # 每级别持续时间(秒)
TIMEOUT = 15         # 单个请求超时(秒)
COOLDOWN = 5         # 级别间冷却(秒)

class Metrics:
    def __init__(self):
        self.lock = threading.Lock()
        self.success = 0
        self.fail = 0
        self.latencies = []
        self.errors = {}

    def record(self, ok, latency, error=None):
        with self.lock:
            if ok:
                self.success += 1
                self.latencies.append(latency)
            else:
                self.fail += 1
                err = str(error)[:80]
                self.errors[err] = self.errors.get(err, 0) + 1

    @property
    def total(self):
        return self.success + self.fail

def worker(metrics, stop_event):
    """持续发送请求直到 stop_event 被设置"""
    opener = urllib.request.build_opener()
    while not stop_event.is_set():
        start = time.time()
        try:
            req = urllib.request.Request(URL, headers={
                "User-Agent": "StressTest/2.0",
                "Connection": "close"
            })
            with opener.open(req, timeout=TIMEOUT) as resp:
                resp.read()
                latency = (time.time() - start) * 1000
                metrics.record(True, latency)
        except Exception as e:
            latency = (time.time() - start) * 1000
            metrics.record(False, latency, type(e).__name__)

def run_level(concurrency):
    """运行单个并发级别"""
    m = Metrics()
    stop = threading.Event()

    threads = []
    for i in range(concurrency):
        t = threading.Thread(target=worker, args=(m, stop), daemon=True)
        t.start()
        threads.append(t)

    # 输出进度
    start_time = time.time()
    for sec in range(DURATION):
        time.sleep(1)
        elapsed = sec + 1
        with m.lock:
            if m.latencies and m.success > 0:
                recent = m.latencies[-max(1, min(concurrency * 2, len(m.latencies))):]
                avg = sum(recent) / len(recent) if recent else 0
            else:
                avg = 0
        rate = m.success / elapsed if elapsed > 0 else 0
        done = elapsed * 20 // DURATION
        bar = "#" * done + "-" * (20 - done)
        print(f"  [{bar}] {elapsed:2d}s | OK={m.success:5d} FAIL={m.fail:3d} | "
              f"RPS~{rate:.0f} | Latency~{avg:.0f}ms", flush=True)

    stop.set()
    for t in threads:
        t.join(timeout=2)

    return m

print("=" * 68)
print("  压 力 测 试 — superorange.top")
print("=" * 68)
print(f"  目标:         {URL}")
print(f"  服务器配置:   4核 4GB（用户提供）")
print(f"  每级持续:     {DURATION}s")
print(f"  请求超时:     {TIMEOUT}s")
print(f"  策略:         每个级别固定并发持续 {DURATION}s")
print("=" * 68)

# 并发级别设计（对4核4G服务器）:
# 10→20→40→80→120→160→200→250→300→400→500→600→800→1000
levels = [10, 20, 40, 80, 120, 160, 200, 250, 300, 400, 500, 600, 800, 1000]

print()
print(f"{'并发':>5s} | {'总请求':>7s} | {'成功':>6s} | {'失败':>5s} | "
      f"{'RPS':>7s} | {'成功率':>6s} | {'avg':>6s} | {'P50':>6s} | "
      f"{'P95':>6s} | {'P99':>6s} | {'min':>5s} | {'max':>6s}")
print("-" * 105)

for level in levels:
    print(f"\n{'='*50}")
    print(f"  并发级别: {level} 个并发连接")
    print(f"{'='*50}")

    metrics = run_level(level)
    if metrics.total == 0:
        print(f"  [无响应，停止测试]")
        break

    rps = metrics.total / DURATION
    success_rate = (metrics.success / metrics.total) * 100

    if metrics.latencies:
        sl = sorted(metrics.latencies)
        avg = statistics.mean(sl)
        p50 = sl[len(sl) // 2]
        p95 = sl[int(len(sl) * 0.95)]
        p99 = sl[int(len(sl) * 0.99)]
        mn, mx = sl[0], sl[-1]
    else:
        avg = p50 = p95 = p99 = mn = mx = 0

    print(f"  {'':->48}")
    print(f"  {level:3d}并发 | {metrics.total:7d} | {metrics.success:6d} | {metrics.fail:5d} | "
          f"{rps:6.1f}/s | {success_rate:5.1f}% | {avg:5.0f}ms | {p50:5.0f}ms | "
          f"{p95:5.0f}ms | {p99:5.0f}ms | {mn:4.0f}ms | {mx:5.0f}ms")

    if metrics.errors:
        for err, count in sorted(metrics.errors.items(), key=lambda x: -x[1])[:3]:
            print(f"        错误: {err} x{count}")

    # 停止条件
    if success_rate < 90:
        print(f"\n  >>> 成功率 {success_rate:.1f}% < 90%，已触及承载上限")
        break
    if p99 > 25000:
        print(f"\n  >>> P99 延迟 {p99:.0f}ms > 25s，已触及承载上限")
        break
    if rps < 1 and level > 40:
        print(f"\n  >>> 吞吐量降至 {rps:.1f}/s，已触及承载上限")
        break

    print(f"  [冷却 {COOLDOWN}s...]")
    time.sleep(COOLDOWN)

print("\n" + "=" * 68)
print("  测试完成！")
print("=" * 68)
