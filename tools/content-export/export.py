#!/usr/bin/env python3
"""
Exporter de contenido de la app aplivit -> bundle canonico hibrido.

Lee los levels_<lang>.json empaquetados en la app y emite un unico
content-bundle.v1.json en el schema que:
  1. se pushea al backend (Course -> Lesson -> Exercise), y
  2. la app cachea localmente para funcionar offline.

Schema hibrido: campos que el backend entiende (type/order/content) + un
"payload" opaco con los campos ricos propios de la app (subtipos, salience,
imagenes, indices...). externalId estable en cada nivel para permitir upsert
idempotente cuando el backend lo soporte.

Uso:
    python3 tools/content-export/export.py
"""
import json
import hashlib
import pathlib

REPO = pathlib.Path(__file__).resolve().parents[2]
FILES_DIR = REPO / "composeApp/src/commonMain/composeResources/files"
OUT = pathlib.Path(__file__).resolve().parent / "content-bundle.v1.json"

# code de idioma -> metadata del curso destino
LANGS = {
    "es": ("Alfabetización - Español", "Curso de lectoescritura por sílabas (español)."),
    "en": ("Literacy - English", "Syllable-based literacy course (English)."),
    "fr": ("Alphabétisation - Français", "Cours de lecture par syllabes (français)."),
}


def build_course(lang: str, title: str, description: str) -> dict:
    raw = json.loads((FILES_DIR / f"levels_{lang}.json").read_text(encoding="utf-8"))
    lessons = []
    for lvl in raw:
        lid = lvl["id"]
        syllables = lvl["syllables"]
        exercises = []
        order = 1
        # Una actividad de vocalizacion por cada silaba (el alumno pronuncia silaba por silaba).
        for i, syl in enumerate(syllables, start=1):
            exercises.append({
                "externalId": f"{lang}-lvl-{lid}-vocalize-syl-{i}",
                "type": "VoiceRecognition",      # enum del backend
                "order": order,
                "content": {                      # campos que el backend entiende hoy
                    "syllables": [syl],
                },
                "payload": {                      # HIBRIDO: schema propio de la app
                    "appType": "VOCALIZE",
                    "vocalizeType": "SYLLABLE",
                    "content": syl,
                    "instruction": lvl["instruction"],
                    "language": lang,
                },
            })
            order += 1
        # Y una actividad final de vocalizar la palabra completa.
        exercises.append({
            "externalId": f"{lang}-lvl-{lid}-vocalize-word",
            "type": "VoiceRecognition",
            "order": order,
            "content": {
                "targetWord": lvl["word"],
                "syllables": syllables,
            },
            "payload": {
                "appType": "VOCALIZE",
                "vocalizeType": "WORD",
                "content": lvl["word"],
                "instruction": lvl["instruction"],
                "language": lang,
            },
        })
        lessons.append({
            "externalId": f"{lang}-lvl-{lid}",
            "title": f"Nivel {lid} - {lvl['word']}",
            "order": lid,
            # Los game modes ricos adicionales (Touch/Link/Sentence/AudioPair) hoy se generan en
            # codigo en la app; se autoran dentro de este mismo schema (campo "payload") mas adelante.
            "exercises": exercises,
        })
    return {
        "externalId": f"course-{lang}",
        "title": title,
        "description": description,
        "difficultyLevel": "Beginner",
        "language": lang,
        "lessons": lessons,
    }


def main() -> None:
    courses = [build_course(l, t, d) for l, (t, d) in LANGS.items()]

    # contentVersion determinista: hash del contenido. Cambia solo si el contenido cambia,
    # asi la app compara su version cacheada contra esta para decidir si re-descarga.
    payload = json.dumps(courses, ensure_ascii=False, sort_keys=True).encode("utf-8")
    version = hashlib.sha256(payload).hexdigest()[:12]

    bundle = {
        "schemaVersion": 1,
        "contentVersion": version,
        "courses": courses,
    }
    OUT.write_text(json.dumps(bundle, ensure_ascii=False, indent=2), encoding="utf-8")

    n_lessons = sum(len(c["lessons"]) for c in bundle["courses"])
    n_ex = sum(len(le["exercises"]) for c in bundle["courses"] for le in c["lessons"])
    print(f"OK -> {OUT}")
    print(f"   contentVersion={version}")
    print(f"   cursos={len(bundle['courses'])}  lecciones={n_lessons}  ejercicios={n_ex}")


if __name__ == "__main__":
    main()
