"""M6 夜栈灯码 + 存量对比度验收（≥4.5:1）

用法（在 frontend 目录）：
  python scripts/wcag_verify.py
"""

from __future__ import annotations

import math
import sys


def srgb_to_lin(c: float) -> float:
    c = c / 255.0
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def luminance_hex(hex_color: str) -> float:
    h = hex_color.lstrip("#")
    r, g, b = int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)
    return 0.2126 * srgb_to_lin(r) + 0.7152 * srgb_to_lin(g) + 0.0722 * srgb_to_lin(b)


def ratio_hex(fg: str, bg: str) -> float:
    l1, l2 = sorted([luminance_hex(fg), luminance_hex(bg)], reverse=True)
    return (l1 + 0.05) / (l2 + 0.05)


def composite(fg: str, bg_hex: str, alpha: float) -> str:
    h = bg_hex.lstrip("#")
    f = fg.lstrip("#")
    rgb = [
        round(int(f[i : i + 2], 16) * alpha + int(h[i : i + 2], 16) * (1 - alpha))
        for i in (0, 2, 4)
    ]
    return "#%02x%02x%02x" % tuple(rgb)


def mark(ok: bool) -> str:
    return "[OK] 达标" if ok else "[FAIL] 不达标"


def _oklab_to_linear_srgb(L: float, a: float, b: float) -> tuple[float, float, float]:
    l_ = L + 0.3963377774 * a + 0.2158037573 * b
    m_ = L - 0.1055613458 * a - 0.0638541728 * b
    s_ = L - 0.0894841775 * a - 1.2914855480 * b
    l, m, s = l_**3, m_**3, s_**3
    r = +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
    g = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
    b2 = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
    return r, g, b2


def _lin_to_srgb(c: float) -> float:
    if c <= 0.0031308:
        return 12.92 * c
    return 1.055 * (c ** (1 / 2.4)) - 0.055


def oklch_to_hex(L_pct: float, C: float, H: float) -> str:
    L = L_pct / 100.0
    h_rad = math.radians(H)
    a = C * math.cos(h_rad)
    b = C * math.sin(h_rad)
    r, g, bl = _oklab_to_linear_srgb(L, a, b)
    rgb = [max(0, min(255, round(_lin_to_srgb(x) * 255))) for x in (r, g, bl)]
    return "#%02x%02x%02x" % tuple(rgb)


def ratio_oklch(fg: tuple[float, float, float], bg: tuple[float, float, float]) -> float:
    return ratio_hex(oklch_to_hex(*fg), oklch_to_hex(*bg))


all_ok = True


def check(name: str, r: float, threshold: float = 4.5) -> None:
    global all_ok
    ok = r >= threshold
    all_ok &= ok
    print(f"  {name:<44} {r:.2f}:1  {mark(ok)}")


print("== M6-1 夜栈灯码 · Dark ==")
check("Dark text-1 / bg", ratio_oklch((93, 0.008, 262), (13, 0.015, 262)))
check("Dark text-2 / bg", ratio_oklch((72, 0.01, 262), (13, 0.015, 262)))
check("Dark accent-text / accent", ratio_oklch((13, 0.015, 262), (76, 0.15, 75)))
check("Dark text-1 / surface-1", ratio_oklch((93, 0.008, 262), (16, 0.018, 262)))

print("\n== M6-1 夜栈灯码 · Light ==")
check("Light text-1 / bg", ratio_oklch((22, 0.018, 75), (97.5, 0.008, 80)))
check("Light text-2 / bg", ratio_oklch((38, 0.015, 75), (97.5, 0.008, 80)))
# Light Primary：深色字压 amber-600（白字实测不足）
check(
    "Light accent-text(paper-900) / accent",
    ratio_oklch((22, 0.018, 75), (68, 0.15, 70)),
)
check("Light text-1 / #fff", ratio_hex(oklch_to_hex(22, 0.018, 75), "#ffffff"))

print("\n== 语义色徽章口径（字 / 基色12%叠底）==")
dark_bg = oklch_to_hex(13, 0.015, 262)
light_bg = oklch_to_hex(97.5, 0.008, 80)
success = oklch_to_hex(70, 0.14, 150)
error = oklch_to_hex(62, 0.19, 25)
success_on_light = oklch_to_hex(42, 0.12, 150)
error_on_light = oklch_to_hex(48, 0.16, 25)
check("success 字 / dark 着色底", ratio_hex(success, composite(success, dark_bg, 0.12)))
check("error 字 / dark 着色底", ratio_hex(error, composite(error, dark_bg, 0.12)))
check(
    "success-on / light 着色底",
    ratio_hex(success_on_light, composite(success, light_bg, 0.12)),
)
check(
    "error-on / light 着色底",
    ratio_hex(error_on_light, composite(error, light_bg, 0.12)),
)
check("success-on / light #fff", ratio_hex(success_on_light, "#ffffff"))
check("error-on / light #fff", ratio_hex(error_on_light, "#ffffff"))

print("\n== 存量语义文字变体（兼容）==")
check("success #217f17 / 白", ratio_hex("#217f17", "#ffffff"))
check("warning #a15e00 / 白", ratio_hex("#a15e00", "#ffffff"))
check("error #c72d31 / 白", ratio_hex("#c72d31", "#ffffff"))

if not all_ok:
    print("\n结论: 存在不达标项")
    sys.exit(1)

print("\n结论: 全部抽检 ≥4.5:1，验收通过")
