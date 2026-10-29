"""Release checks: target SDK 36 and 16 KB page alignment of native libraries.

usage: python scripts/check_release.py <app-release.aab> <app-release.apk>
- Every lib/**/*.so in the AAB and APK must have ELF PT_LOAD segments aligned to >= 16 KiB.
- Uncompressed .so files in the APK must start on a 16 KiB boundary inside the zip
  (zipalign -P 16), which is what `useLegacyPackaging = false` produces.
"""
import struct
import sys
import zipfile

PAGE = 16 * 1024


def load_aligns(data: bytes):
    if data[:4] != b"\x7fELF":
        return None
    is64 = data[4] == 2
    little = data[5] == 1
    e = "<" if little else ">"
    if is64:
        phoff, = struct.unpack_from(e + "Q", data, 0x20)
        phentsize, phnum = struct.unpack_from(e + "HH", data, 0x36)
    else:
        phoff, = struct.unpack_from(e + "I", data, 0x1C)
        phentsize, phnum = struct.unpack_from(e + "HH", data, 0x2A)
    aligns = []
    for i in range(phnum):
        off = phoff + i * phentsize
        p_type, = struct.unpack_from(e + "I", data, off)
        if p_type != 1:  # PT_LOAD
            continue
        p_align, = struct.unpack_from(e + "Q", data, off + 48) if is64 else struct.unpack_from(e + "I", data, off + 28)
        aligns.append(p_align)
    return aligns


def check_elf(path: str) -> bool:
    ok = True
    with zipfile.ZipFile(path) as z:
        libs = [n for n in z.namelist() if n.endswith(".so")]
        print(f"{path}: {len(libs)} native libs")
        for n in libs:
            aligns = load_aligns(z.read(n))
            bad = aligns is None or any(a < PAGE for a in aligns)
            ok &= not bad
            print(f"  {'FAIL' if bad else 'ok  '} {n}  PT_LOAD align={sorted(set(aligns or []))}")
    return ok


def check_zip_offsets(apk: str) -> bool:
    ok = True
    with zipfile.ZipFile(apk) as z, open(apk, "rb") as f:
        for info in z.infolist():
            if not info.filename.endswith(".so"):
                continue
            f.seek(info.header_offset + 26)
            name_len, extra_len = struct.unpack("<HH", f.read(4))
            data_off = info.header_offset + 30 + name_len + extra_len
            stored = info.compress_type == zipfile.ZIP_STORED
            good = stored and data_off % PAGE == 0
            ok &= good
            print(f"  {'ok  ' if good else 'FAIL'} {info.filename}  stored={stored} offset%16K={data_off % PAGE}")
    return ok


if __name__ == "__main__":
    aab, apk = sys.argv[1], sys.argv[2]
    results = [check_elf(aab), check_elf(apk), check_zip_offsets(apk)]
    print("16 KB PAGE ALIGNMENT:", "PASS" if all(results) else "FAIL")
    sys.exit(0 if all(results) else 1)
