#!/usr/bin/env python3
"""
Bundle para POST /api/content/import con UN ejercicio de backend POR NIVEL.
El backend decide el contenido (palabra/sílabas/instrucción y su orden); la app reconstruye el
Level desde el payload y genera sus mini-juegos con el formato de siempre.

type=VoiceRecognition (client-evaluated: el nivel se completa en la app). content lleva
targetWord+syllables (para pasar la validación); payload lleva los datos del nivel.

Uso: python3 level_import.py > level-import.json
"""
import json, hashlib, pathlib, sys

REPO = pathlib.Path(__file__).resolve().parents[2]
ES = REPO / "composeApp/src/commonMain/composeResources/files/levels_es.json"


def main() -> None:
    levels = json.loads(ES.read_text(encoding="utf-8"))
    exercises = []
    for lvl in levels:
        lid = lvl["id"]
        exercises.append({
            "externalId": f"es-lvl-{lid}",
            "type": "VoiceRecognition",
            "difficultyLevel": "Beginner",
            "order": lid,
            "content": {"targetWord": lvl["word"], "syllables": lvl["syllables"]},
            "payload": {
                "appType": "LEVEL",
                "level": lid,
                "word": lvl["word"],
                "syllables": lvl["syllables"],
                "instruction": lvl["instruction"],
                "language": "es",
            },
        })
    version = hashlib.sha256(
        json.dumps(exercises, ensure_ascii=False, sort_keys=True).encode()
    ).hexdigest()[:12]
    json.dump({"schemaVersion": 1, "contentVersion": version, "exercises": exercises},
              sys.stdout, ensure_ascii=False)
    print(f"\n# {len(exercises)} niveles, contentVersion={version}", file=sys.stderr)


if __name__ == "__main__":
    main()
