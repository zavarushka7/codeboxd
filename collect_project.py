#!/usr/bin/env python3
"""
Собирает все текстовые файлы проекта в один .txt с оглавлением и разделителями.

Использование:
    python3 collect_project.py                     # текущая папка → project.txt
    python3 collect_project.py -r /path/to/project  # указать корень
    python3 collect_project.py -o output.txt        # указать выходной файл
    python3 collect_project.py --ext .kt .xml .json # только эти расширения
"""

import argparse
import os
import sys
from pathlib import Path

# ── Что пропускаем (папки) ──
DEFAULT_SKIP_DIRS = {
    ".git", ".gradle", ".idea", "build", "out", "node_modules",
    "__pycache__", ".venv", "venv", "env", ".dart_tool", ".next",
    ".cache", "dist", ".kotlin", "generated", ".fleet",
}

# ── Что пропускаем (файлы) ──
DEFAULT_SKIP_FILES = {
    ".DS_Store", "Thumbs.db", "local.properties",
    "google-services.json", ".gitignore",
}

# ── Расширения, которые считаем текстом, если не указано иное ──
DEFAULT_TEXT_EXT = {
    ".kt", ".kts", ".java", ".xml", ".json", ".gradle", ".properties",
    ".pro", ".toml", ".md", ".txt", ".yml", ".yaml", ".cfg", ".ini",
    ".html", ".css", ".js", ".ts", ".tsx", ".jsx", ".py", ".sh",
    ".gitignore", ".editorconfig", ".firebaserc", ".npmrc",
}

# ── Размер файла (пропускаем слишком большие — обычно бинарники) ──
MAX_FILE_SIZE = 5 * 1024 * 1024  # 5 MB

# ── Бинарные расширения — пропускаем всегда ──
BINARY_EXT = {
    ".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".bmp", ".svg",
    ".ttf", ".otf", ".woff", ".woff2", ".eot",
    ".mp3", ".mp4", ".wav", ".ogg", ".webm",
    ".zip", ".tar", ".gz", ".rar", ".7z",
    ".apk", ".aab", ".jar", ".aar", ".dex",
    ".so", ".dylib", ".dll", ".class",
    ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
    ".db", ".sqlite", ".sqlite3",
}

# ── Специальные имена файлов без расширения ──
SPECIAL_TEXT_NAMES = {
    "Dockerfile", "Makefile", "README", "LICENSE", ".gitignore",
    ".editorconfig", ".firebaserc", ".npmrc", "gradlew", "gradlew.bat",
    "firebase.json", "firestore.indexes.json", "firestore.rules",
}


def is_binary(path: Path) -> bool:
    """Проверка, что файл — бинарник (по первым 8 КБ)."""
    try:
        with open(path, "rb") as f:
            chunk = f.read(8192)
        return b"\x00" in chunk
    except OSError:
        return True


def should_include(path: Path, root: Path, text_exts: set) -> bool:
    """Решает, включать ли файл."""
    name = path.name

    if name in DEFAULT_SKIP_FILES:
        return False
    if path.suffix.lower() in BINARY_EXT:
        return False
    if path.stat().st_size > MAX_FILE_SIZE:
        return False

    # Файл по расширению?
    if path.suffix.lower() in text_exts:
        return True
    # Файл без расширения, но известный?
    if not path.suffix and name in SPECIAL_TEXT_NAMES:
        return True
    # Файл без расширения — эвристика: пробуем прочитать как UTF-8
    if not path.suffix:
        return not is_binary(path)

    return False


def collect_files(root: Path, text_exts: set) -> list[Path]:
    """Рекурсивно собирает все подходящие файлы, сортирует по пути."""
    files = []
    for current, dirs, filenames in os.walk(root):
        # Пропускаем мусорные папки
        dirs[:] = [d for d in dirs if d not in DEFAULT_SKIP_DIRS]
        for name in filenames:
            p = Path(current) / name
            if p.is_file() and should_include(p, root, text_exts):
                files.append(p)
    files.sort(key=lambda p: str(p).lower())
    return files


def write_output(files: list[Path], root: Path, out: Path) -> None:
    """Пишет все файлы в один .txt с оглавлением."""
    with open(out, "w", encoding="utf-8") as f:
        # ── Шапка ──
        f.write("#" * 78 + "\n")
        f.write(f"# PROJECT: {root.name}\n")
        f.write(f"# ROOT:    {root}\n")
        f.write(f"# FILES:   {len(files)}\n")
        f.write("#" * 78 + "\n\n")

        # ── Оглавление ──
        f.write("=" * 78 + "\n")
        f.write("ОГЛАВЛЕНИЕ\n")
        f.write("=" * 78 + "\n")
        for i, p in enumerate(files, 1):
            rel = p.relative_to(root)
            f.write(f"{i:4d}. {rel}\n")
        f.write("\n")

        # ── Содержимое ──
        for i, p in enumerate(files, 1):
            rel = p.relative_to(root)
            f.write("=" * 78 + "\n")
            f.write(f"FILE [{i}/{len(files)}]: {rel}\n")
            f.write("=" * 78 + "\n")
            try:
                content = p.read_text(encoding="utf-8")
            except UnicodeDecodeError:
                try:
                    content = p.read_text(encoding="cp1251")
                except Exception:
                    f.write("[[не удалось прочитать как текст]]\n\n")
                    continue
            except Exception as e:
                f.write(f"[[ошибка чтения: {e}]]\n\n")
                continue
            f.write(content)
            if not content.endswith("\n"):
                f.write("\n")
            f.write("\n")

    print(f"✔ Записано {len(files)} файлов в {out}")
    print(f"  Размер: {out.stat().st_size / 1024:.1f} KB")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Собирает все текстовые файлы проекта в один .txt"
    )
    parser.add_argument(
        "-r", "--root", default=".", help="Корень проекта (по умолчанию: .)"
    )
    parser.add_argument(
        "-o", "--output", default=None,
        help="Выходной файл (по умолчанию: <root>/project.txt)"
    )
    parser.add_argument(
        "--ext", nargs="+", default=None,
        help="Только эти расширения (например: --ext .kt .xml)"
    )
    parser.add_argument(
        "--include-skip-dirs", action="store_true",
        help="Не пропускать build/, .git/ и т.п. (осторожно)"
    )
    args = parser.parse_args()

    root = Path(args.root).resolve()
    if not root.is_dir():
        print(f"✖ Не найдена папка: {root}", file=sys.stderr)
        return 1

    out = Path(args.output) if args.output else root / "project.txt"
    out = out.resolve()

    text_exts = set(DEFAULT_TEXT_EXT)
    if args.ext:
        text_exts = {e if e.startswith(".") else "." + e for e in args.ext}

    if args.include_skip_dirs:
        DEFAULT_SKIP_DIRS.clear()

    files = collect_files(root, text_exts)
    if not files:
        print("✖ Не нашлось подходящих файлов", file=sys.stderr)
        return 1

    write_output(files, root, out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
