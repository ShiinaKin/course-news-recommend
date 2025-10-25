#!/usr/bin/env python3
"""
CLI utility that restructures raw text into a standard news article JSON using OpenAI.

Usage:
    python tools/structure_text/main.py <api_key> <text_file_path>

The script prints a JSON object like:
{
  "title": "...",
  "author": "... or null",
  "content": "...",
  "publishTime": "... or null"
}
"""

from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any, Dict, Optional

from openai import OpenAI, OpenAIError

MODEL_NAME = "gpt-5-nano"

SCHEMA: Dict[str, Any] = {
    "type": "object",
    "properties": {
        "title": {"type": "string", "description": "Concise headline summarising the text."},
        "author": {"type": ["string", "null"], "description": "Author name when identifiable, otherwise null."},
        "content": {"type": "string", "description": "Article-style narrative rewritten from the source text."},
        "publishTime": {"type": ["string", "null"], "description": "ISO-8601 publish time if it can be inferred."},
    },
    "required": ["title", "author", "content", "publishTime"],
    "additionalProperties": False,
}

SYSTEM_PROMPT = (
    "You are an assistant that rewrites raw OCR or speech transcripts into structured news articles. "
    "Always respond with a single JSON object that follows the provided schema. "
    "Keep the title concise (<= 80 characters). "
    "Leave author or publishTime as null when the information cannot be reliably determined."
)


class StructureError(RuntimeError):
    """Raised when structuring fails."""


def extract_json(response_dict: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    """Locate the structured JSON payload in a Responses API dictionary."""
    parsed = response_dict.get("output_parsed")
    if isinstance(parsed, dict):
        return parsed

    output = response_dict.get("output")
    if isinstance(output, list):
        for item in output:
            if not isinstance(item, dict):
                continue
            if isinstance(item.get("json"), dict):
                return item["json"]
            content = item.get("content")
            if isinstance(content, list):
                for chunk in content:
                    if isinstance(chunk, dict) and isinstance(chunk.get("json"), dict):
                        return chunk["json"]
                    if isinstance(chunk, dict) and isinstance(chunk.get("parsed"), dict):
                        return chunk["parsed"]
                    if isinstance(chunk, dict) and chunk.get("type") == "output_text":
                        text = chunk.get("text")
                        if isinstance(text, str):
                            try:
                                return json.loads(text)
                            except json.JSONDecodeError:
                                continue

    choices = response_dict.get("choices")
    if isinstance(choices, list) and choices:
        message = choices[0].get("message")
        if isinstance(message, dict):
            parsed = message.get("parsed")
            if isinstance(parsed, dict):
                return parsed
            content = message.get("content")
            if isinstance(content, list):
                for chunk in content:
                    if isinstance(chunk, dict) and isinstance(chunk.get("json"), dict):
                        return chunk["json"]
                    if isinstance(chunk, dict) and isinstance(chunk.get("parsed"), dict):
                        return chunk["parsed"]
                    if isinstance(chunk, dict) and chunk.get("type") == "output_text":
                        text = chunk.get("text")
                        if isinstance(text, str):
                            try:
                                return json.loads(text)
                            except json.JSONDecodeError:
                                continue
            elif isinstance(content, dict) and isinstance(content.get("json"), dict):
                return content["json"]
    return None


def structure_text(api_key: str, raw_text: str) -> Dict[str, Any]:
    if not raw_text.strip():
        raise StructureError("Input text is empty.")

    client = OpenAI(api_key=api_key)
    try:
        response = client.responses.create(
            model=MODEL_NAME,
            input=[
                {"role": "system", "content": [{"type": "input_text", "text": SYSTEM_PROMPT}]},
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "input_text",
                            "text": (
                                "Restructure the following material according to the schema.\n\n"
                                f"<source>\n{raw_text}\n</source>"
                            ),
                        },
                    ],
                },
            ],
            text={
                "format": {
                    "type": "json_schema",
                    "name": "structured_article",
                    "schema": SCHEMA,
                    "strict": True,
                },
            },
            max_output_tokens=5000,
        )
    except OpenAIError as exc:
        raise StructureError(f"OpenAI API error: {exc}") from exc

    payload = extract_json(response.model_dump())
    if payload is None:
        raise StructureError("Failed to locate JSON payload in model response.")

    for field in ("title", "author", "content", "publishTime"):
        if field not in payload:
            raise StructureError(f"Missing required field '{field}' in model output.")
    return payload


def main() -> int:
    if len(sys.argv) < 3:
        sys.stderr.write("Usage: python tools/structure_text/main.py <api_key> <text_file_path>\n")
        return 1

    api_key = sys.argv[1]
    text_path = Path(sys.argv[2])
    if not text_path.exists():
        sys.stderr.write(f"Error: file not found: {text_path}\n")
        return 1
    try:
        raw_text = text_path.read_text(encoding="utf-8")
    except Exception as exc:  # noqa: BLE001
        sys.stderr.write(f"Error reading file {text_path}: {exc}\n")
        return 1
    try:
        structured = structure_text(api_key, raw_text)
    except StructureError as exc:
        sys.stderr.write(f"Error: {exc}\n")
        return 2
    except Exception as exc:  # noqa: BLE001
        sys.stderr.write(f"Unexpected error: {exc}\n")
        return 3

    sys.stdout.write(json.dumps(structured, ensure_ascii=False))
    sys.stdout.write("\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
