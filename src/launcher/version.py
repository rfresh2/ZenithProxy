from __future__ import annotations

import re
from functools import total_ordering
from typing import Tuple


@total_ordering
class Version:

    _PATTERN = re.compile(r"^[0-9]+\.[0-9]+\.[0-9]+$")

    def __init__(self, version: str):
        if not Version._PATTERN.match(version):
            raise ValueError(f"Invalid version format: {version}")
        self._version = version
        parts = version.split(".")
        if len(parts) != 3:
            raise ValueError(f"Invalid version format: {version}")
        try:
            self._parts: Tuple[int, int, int] = (int(parts[0]), int(parts[1]), int(parts[2]))
        except ValueError as exc:
            raise ValueError(f"Invalid version part in: {version}") from exc

    @classmethod
    def from_parts(cls, major: int, minor: int, patch: int) -> "Version":
        """Create a Version from integer parts."""
        return cls(f"{major}.{minor}.{patch}")

    @property
    def major(self) -> int:
        return self._parts[0]

    @property
    def minor(self) -> int:
        return self._parts[1]

    @property
    def patch(self) -> int:
        return self._parts[2]

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Version):
            return NotImplemented
        return self._parts == other._parts

    def __lt__(self, other: "Version") -> bool:
        if not isinstance(other, Version):
            return NotImplemented
        return self._parts < other._parts

    def __hash__(self) -> int:
        return hash(self._parts)

    def __str__(self) -> str:
        return self._version

    def __repr__(self) -> str:
        return f"Version('{self._version}')"

    @staticmethod
    def validate(version: str) -> bool:
        """Return True if the string matches the MAJOR.MINOR.PATCH pattern."""
        return bool(Version._PATTERN.match(version))
