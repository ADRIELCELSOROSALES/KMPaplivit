#!/usr/bin/env python3
"""
Genera el bundle en el schema PLANO que espera POST /api/content/import del backend
(refactor-education): lista plana de ejercicios, camelCase, (difficultyLevel, order) único global.

Solo idioma base español (el import no lleva `language`; las traducciones van aparte).
Uso: python3 flatten_for_import.py > content-import.flat.json
"""
import json
import hashlib
import pathlib
import sys

REPO = pathlib.Path(__file__).resolve().parents[2]
ES = REPO / "composeApp/src/commonMain/composeResources/files/levels_es.json"


def main() -> None:
    levels = json.loads(ES.read_text(encoding="utf-8"))
    exercises = []
    order = 1
    for lvl in levels:
        lid = lvl["id"]
        for i, syl in enumerate(lvl["syllables"], start=1):
            exercises.append({
                "externalId": f"es-lvl-{lid}-syl-{i}",
                "type": "VoiceRecognition",
                "difficultyLevel": "Beginner",
                "order": order,
                "content": {"syllables": [syl]},
                "payload": {
                    "appType": "VOCALIZE", "vocalizeType": "SYLLABLE",
                    "content": syl, "instruction": lvl["instruction"], "language": "es",
                },
            })
            order += 1
        exercises.append({
            "externalId": f"es-lvl-{lid}-word",
            "type": "VoiceRecognition",
            "difficultyLevel": "Beginner",
            "order": order,
            "content": {"targetWord": lvl["word"], "syllables": lvl["syllables"]},
            "payload": {
                "appType": "VOCALIZE", "vocalizeType": "WORD",
                "content": lvl["word"], "instruction": lvl["instruction"], "language": "es",
            },
        })
        order += 1

    version = hashlib.sha256(
        json.dumps(exercises, ensure_ascii=False, sort_keys=True).encode()
    ).hexdigest()[:12]
    bundle = {"schemaVersion": 1, "contentVersion": version, "exercises": exercises}
    json.dump(bundle, sys.stdout, ensure_ascii=False)
    print(f"\n# {len(exercises)} ejercicios, contentVersion={version}", file=sys.stderr)


if __name__ == "__main__":
    main()
