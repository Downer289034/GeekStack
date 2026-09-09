"""T-DESIGN-2 / T-DESIGN-5 验收复测：对比度组合（≥4.5:1）

用法（在 frontend 目录）：
  python scripts/wcag_verify.py

口径：标签着色底 = 基色 12% 透明度叠白（与 naive-ui Tag 着色底一致）。
说明：原脚本在 .workbuddy/tmp/（gitignore）；本文件为可提交验收资产。
"""


def srgb_to_lin(c):
    c = c / 255.0
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def luminance(hex_color):
    h = hex_color.lstrip("#")
    r, g, b = int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)
    return 0.2126 * srgb_to_lin(r) + 0.7152 * srgb_to_lin(g) + 0.0722 * srgb_to_lin(b)


def ratio(fg, bg):
    l1, l2 = sorted([luminance(fg), luminance(bg)], reverse=True)
    return (l1 + 0.05) / (l2 + 0.05)


def composite(fg, bg_hex, alpha):
    h = bg_hex.lstrip("#")
    f = fg.lstrip("#")
    rgb = [
        round(int(f[i : i + 2], 16) * alpha + int(h[i : i + 2], 16) * (1 - alpha))
        for i in (0, 2, 4)
    ]
    return "#%02x%02x%02x" % tuple(rgb)


def mark(ok, fail_label="不达标"):
    return "[OK] 达标" if ok else f"[FAIL] {fail_label}"


print("== 原 4 项不达标组合 · 复测 ==")
checks = [
    ("F1 success 文字(变体#217f17) / 白底", "#217f17", "#ffffff"),
    ("F1b success 标签着色底", "#217f17", composite("#34c724", "#ffffff", 0.12)),
    ("F2 warning 文字(变体#a15e00) / 白底", "#a15e00", "#ffffff"),
    ("F2b warning 标签着色底", "#a15e00", composite("#ff9500", "#ffffff", 0.12)),
    ("F3 error 文字(变体#c72d31) / 白底", "#c72d31", "#ffffff"),
    ("F3b error 标签着色底", "#c72d31", composite("#d73035", "#ffffff", 0.12)),
    ("F3c 白字 / error 实心按钮(#d73035)", "#ffffff", "#d73035"),
    ("F4 白字 / hover 蓝(#1467e8)", "#ffffff", "#1467e8"),
    ("F4b hover 蓝作文字 / 白底", "#1467e8", "#ffffff"),
]
all_ok = True
for name, fg, bg in checks:
    r = ratio(fg, bg)
    ok = r >= 4.5
    all_ok &= ok
    print(f"  {name:<42} {r:.2f}:1  {mark(ok)}")

print("\n== T-DESIGN-5 内容分区色（3 基色 × 白底/着色底）==")
td5 = [
    ("TD5 forum 文字(变体#5b3fd1) / 白底", "#5b3fd1", "#ffffff"),
    ("TD5 forum 标签着色底", "#5b3fd1", composite("#7a5af8", "#ffffff", 0.12)),
    ("TD5 share 文字(变体#0a6e63) / 白底", "#0a6e63", "#ffffff"),
    ("TD5 share 标签着色底", "#0a6e63", composite("#0e9384", "#ffffff", 0.12)),
    ("TD5 notice 文字(变体#a54a0f) / 白底", "#a54a0f", "#ffffff"),
    ("TD5 notice 标签着色底", "#a54a0f", composite("#e06616", "#ffffff", 0.12)),
]
for name, fg, bg in td5:
    r = ratio(fg, bg)
    ok = r >= 4.5
    all_ok &= ok
    print(f"  {name:<42} {r:.2f}:1  {mark(ok)}")

print("\n== T-DESIGN-5 Hero 渐变白字预算 ==")
hero = [
    ("Hero 白字 / 渐变深端 #0540b4", "#ffffff", "#0540b4"),
    ("Hero 白字 / 渐变中段 #0a59f7", "#ffffff", "#0a59f7"),
    ("Hero 白字 / 渐变浅端 #2666f0", "#ffffff", "#2666f0"),
]
for name, fg, bg in hero:
    r = ratio(fg, bg)
    ok = r >= 4.5
    all_ok &= ok
    print(f"  {name:<42} {r:.2f}:1  {mark(ok)}")

print("\n== 存量组合回归（应保持达标）==")
regress = [
    ("正文 #182431 / 卡片白底", "#182431", "#ffffff"),
    ("正文 #182431 / 页面灰底", "#182431", "#f1f3f5"),
    ("次要文字 #5e6772 / 白底", "#5e6772", "#ffffff"),
    ("次要文字 #5e6772 / 灰底", "#5e6772", "#f1f3f5"),
    ("品牌文字 #0a59f7 / 白底", "#0a59f7", "#ffffff"),
    ("品牌文字 #0a59f7 / 灰底", "#0a59f7", "#f1f3f5"),
    ("白字 / 主按钮 #0a59f7", "#ffffff", "#0a59f7"),
    ("pressed 白字 / #0540b4", "#ffffff", "#0540b4"),
]
for name, fg, bg in regress:
    r = ratio(fg, bg)
    ok = r >= 4.5
    all_ok &= ok
    print(f"  {name:<42} {r:.2f}:1  {mark(ok, '回归!')}")

print("\n结论:", "全部组合 ≥4.5:1，验收通过" if all_ok else "存在不达标项，需返工")
raise SystemExit(0 if all_ok else 1)
