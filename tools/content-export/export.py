#!/usr/bin/env python3
"""
Exportador ÚNICO del contenido de la app aplivit hacia el backend aplivlit.

Regla del formato (la que entiende la app, ver BackendLevelMapper.kt):
    UN ejercicio de backend = UN nivel de la app.
    El nivel viaja en `payload` (appType=LEVEL, level, word, syllables, instruction), que el
    backend guarda opaco y la app usa para reconstruir sus mini-juegos.
Si el contenido publicado no respeta eso, la app no puede mapear ningún nivel y cae al JSON
local de respaldo (en silencio, salvo el aviso de GetLevelsUseCase). Por eso este script verifica
el bundle antes de escribirlo.

Emite dos artefactos:
  content-bundle.v1.json  -> POST /api/content/import
                             (camelCase, `payload` como OBJETO, solo el idioma base español)
  translations.v1.json    -> POST /api/exercises/{exerciseId}/translations  (o PUT .../{language})
                             (snake_case, `payload` como STRING, un item por idioma no base)

Uso:
    python3 tools/content-export/export.py

Import (el id del ejercicio se resuelve por externalId con GET /api/exercises):
    curl -X POST "$API/api/content/import" -H "Authorization: Bearer $ADMIN_JWT" \
         -H 'Content-Type: application/json' -d @tools/content-export/content-bundle.v1.json
"""
import hashlib
import json
import pathlib

REPO = pathlib.Path(__file__).resolve().parents[2]
FILES_DIR = REPO / "composeApp/src/commonMain/composeResources/files"
HERE = pathlib.Path(__file__).resolve().parent

BUNDLE_OUT = HERE / "content-bundle.v1.json"
TRANSLATIONS_OUT = HERE / "translations.v1.json"

SCHEMA_VERSION = 1

# Idioma base del catálogo (el que se importa como contenido del ejercicio).
BASE_LANG = "es"
# Idiomas que van como traducción (RF-09b). Clave = código de la app, valor = enum del backend.
TRANSLATED_LANGS = {"en": "English", "fr": "French"}

# Todos los niveles son de vocalización guiada: la app los completa con el motor de voz, así que
# el backend no los evalúa (client-evaluated).
EXERCISE_TYPE = "VoiceRecognition"
DIFFICULTY = "Beginner"


def read_levels(lang: str) -> list[dict]:
    return json.loads((FILES_DIR / f"levels_{lang}.json").read_text(encoding="utf-8"))


def level_payload(level: dict, lang: str) -> dict:
    return {
        "appType": "LEVEL",
        "level": level["id"],
        "word": level["word"],
        "syllables": level["syllables"],
        "instruction": level["instruction"],
        "language": lang,
    }


def build_exercises(levels: list[dict]) -> list[dict]:
    exercises = []
    for level in levels:
        exercises.append({
            # externalId estable: es la clave del upsert idempotente del import. NO cambiarla sin
            # migrar, o el import marca eliminados los ejercicios viejos y crea otros nuevos,
            # perdiendo la relación con los intentos ya registrados (y con ellos, el progreso).
            "externalId": f"{BASE_LANG}-lvl-{level['id']}",
            "type": EXERCISE_TYPE,
            "difficultyLevel": DIFFICULTY,
            # El orden en la secuencia global (RF-06) = el número de nivel.
            "order": level["id"],
            # Campos que el backend sí entiende, para que pase su validación por tipo.
            "content": {"targetWord": level["word"], "syllables": level["syllables"]},
            "payload": level_payload(level, BASE_LANG),
        })
    return exercises


def build_translations(base_levels: list[dict]) -> list[dict]:
    translations = []
    for lang, backend_language in TRANSLATED_LANGS.items():
        levels = read_levels(lang)
        by_id = {lvl["id"]: lvl for lvl in levels}
        missing = [lvl["id"] for lvl in base_levels if lvl["id"] not in by_id]
        if missing:
            raise SystemExit(f"ERROR: levels_{lang}.json no tiene los niveles {missing}")

        for base in base_levels:
            level = by_id[base["id"]]
            translations.append({
                "externalId": f"{BASE_LANG}-lvl-{base['id']}",
                "language": backend_language,
                "content": {
                    "target_word": level["word"],
                    "syllables": level["syllables"],
                    # En este endpoint el payload va stringificado (ver ExerciseContentRequest).
                    "payload": json.dumps(level_payload(level, lang), ensure_ascii=False),
                },
            })
    return translations


def verify(exercises: list[dict]) -> None:
    """Mismas reglas que BackendLevelMapper.toLevel: sin esto, la app ignora todo el catálogo."""
    if not exercises:
        raise SystemExit("ERROR: bundle vacío")

    external_ids = [e["externalId"] for e in exercises]
    if len(set(external_ids)) != len(external_ids):
        raise SystemExit("ERROR: externalId duplicado")

    orders = [e["order"] for e in exercises]
    if len(set(orders)) != len(orders):
        raise SystemExit("ERROR: order duplicado (el backend lo exige único por dificultad)")

    for exercise in exercises:
        payload = exercise.get("payload") or {}
        level_id = payload.get("level")
        if payload.get("appType") != "LEVEL":
            raise SystemExit(f"ERROR: {exercise['externalId']} no tiene payload.appType=LEVEL")
        if not isinstance(level_id, int) or level_id < 1:
            raise SystemExit(f"ERROR: {exercise['externalId']} sin payload.level válido")
        if not payload.get("word"):
            raise SystemExit(f"ERROR: {exercise['externalId']} sin payload.word")
        if not payload.get("syllables"):
            raise SystemExit(f"ERROR: {exercise['externalId']} sin payload.syllables")

    levels = sorted(e["payload"]["level"] for e in exercises)
    if levels != list(range(1, len(levels) + 1)):
        raise SystemExit(
            "ERROR: los niveles deben ser 1..N sin huecos (la app deriva de ahí qué niveles "
            f"están completos); llegaron: {levels}"
        )


def content_version(exercises: list[dict]) -> str:
    raw = json.dumps(exercises, ensure_ascii=False, sort_keys=True).encode("utf-8")
    return hashlib.sha256(raw).hexdigest()[:12]


def main() -> None:
    base_levels = read_levels(BASE_LANG)
    exercises = build_exercises(base_levels)
    verify(exercises)

    version = content_version(exercises)
    bundle = {
        "schemaVersion": SCHEMA_VERSION,
        "contentVersion": version,
        "exercises": exercises,
    }
    translations = {
        "schemaVersion": SCHEMA_VERSION,
        "contentVersion": version,
        "translations": build_translations(base_levels),
    }

    BUNDLE_OUT.write_text(json.dumps(bundle, ensure_ascii=False, indent=2), encoding="utf-8")
    TRANSLATIONS_OUT.write_text(json.dumps(translations, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"OK -> {BUNDLE_OUT.name}: {len(exercises)} niveles, contentVersion={version}")
    print(f"OK -> {TRANSLATIONS_OUT.name}: {len(translations['translations'])} traducciones "
          f"({', '.join(TRANSLATED_LANGS)})")


if __name__ == "__main__":
    main()
