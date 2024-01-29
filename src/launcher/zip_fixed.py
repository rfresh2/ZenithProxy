import os
from zipfile import ZipFile, ZipInfo


class ZipFileWithPermissions(ZipFile):
    """Custom ZipFile class handling file permissions."""

    def _extract_member(self, member, targetpath, pwd):
        if not isinstance(member, ZipInfo):
            member = self.getinfo(member)

        targetpath = super()._extract_member(member, targetpath, pwd)

        attr = member.external_attr >> 16
        if attr != 0:
            os.chmod(targetpath, attr)
        return targetpath
