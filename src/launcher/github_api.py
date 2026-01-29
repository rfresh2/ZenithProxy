import time
from typing import Optional, Tuple

from requests import Session, Response
from requests.adapters import HTTPAdapter
from urllib3 import Retry

from log import info, error, exception


class GitHubAPI:
    launch_config = None

    def __init__(self, launch_config):
        self.launch_config = launch_config

    def _get_base_url(self):
        if self.launch_config.repo_owner == "rfresh2" and self.launch_config.repo_name == "ZenithProxy":
            host = "github.2b2t.vc"
        else:
            host = "api.github.com"
        return f"https://{host}/repos/{self.launch_config.repo_owner}/{self.launch_config.repo_name}/releases"

    def _get_headers(self):
        return {
            "User-Agent": "ZenithProxy/" + self.launch_config.local_version,
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "Connection": "close",
        }

    def _create_session(self):
        retries = Retry(
            total=1,
            connect=1,
            read=1,
            status=1,
            redirect=5,
            backoff_factor=1,
            status_forcelist=[500, 502, 503, 504]
        )
        session = Session()
        adapter = HTTPAdapter(max_retries=retries)
        session.mount("https://", adapter)
        return session

    def _send_request(self, url, headers, params=None, timeout=10, allow_redirects=False, stream=False) -> Response:
        try:
            with self._create_session() as session:
                response = session.get(url, headers=headers, params=params, timeout=timeout, allow_redirects=allow_redirects, stream=stream)
                if response.status_code == 200:
                    return response
                raise Exception(f"Request to {url} failed with status code {response.status_code}")
        except Exception as e:
            raise Exception(f"Request to {url} failed with exception {e}")

    def get_latest_release_and_ver(self, channel) -> Optional[Tuple[int, str]]:
        try:
            response = self._send_request(self._get_base_url(), self._get_headers(), params={"per_page": 100})
            releases = response.json()
            latest_release = max(
                (r for r in releases if not r["draft"] and r["tag_name"].endswith("+" + channel)),
                key=lambda r: r["published_at"],
                default=None,
            )
            return (latest_release["id"], latest_release["tag_name"]) if latest_release else None
        except:
            exception("Failed to get releases")
        return None

    def get_release_for_ver(self, tag_name) -> Optional[Tuple[int, str]]:
        url = f"{self._get_base_url()}/tags/{tag_name}"
        try:
            response = self._send_request(url, self._get_headers())
            release = response.json()
            return release["id"], release["tag_name"]
        except:
            exception("Failed to get release for version")
        return None

    def get_asset_id(self, release_id, asset_name, tag=False) -> Optional[str]:
        url = f"{self._get_base_url()}/{'tags/' if tag else ''}{release_id}"
        try:
            response = self._send_request(url, self._get_headers())
            return next((asset["id"] for asset in response.json()["assets"] if asset["name"] == asset_name), None)
        except:
            error("Failed to get release asset ID")
        return None

    def get_release_tag_asset_id(self, release_id, asset_name) -> Optional[str]:
        return self.get_asset_id(release_id, asset_name, True)

    def download_asset(self, asset_id, verbose=False) -> Optional[bytes]:
        url = f"{self._get_base_url()}/assets/{asset_id}"
        download_headers = self._get_headers()
        download_headers["Accept"] = "application/octet-stream"
        try:
            response = self._send_request(url, download_headers, allow_redirects=True, timeout=60, stream=True)
            total_header = response.headers.get("Content-Length")
            total = int(total_header) if total_header and total_header.isdigit() else None
            chunk_size = 8192
            downloaded = 0
            data = bytearray()
            # start timer when streaming begins
            start_time = time.time()
            if verbose:
                info(f"Downloading {url}:")

            for chunk in response.iter_content(chunk_size=chunk_size):
                if not chunk:
                    continue
                data.extend(chunk)
                downloaded += len(chunk)
                downloaded_kb = downloaded / 1024
                # elapsed time -> format MM:SS.mmm (milliseconds)
                elapsed_total = time.time() - start_time
                elapsed_ms = int(elapsed_total * 1000)
                minutes = elapsed_ms // 60000
                seconds = (elapsed_ms % 60000) // 1000
                ms = elapsed_ms % 1000
                elapsed_str = f"{minutes:02d}:{seconds:02d}.{ms:03d}"
                if total:
                    percent = downloaded * 100 / total
                    total_kb = total / 1024
                    if verbose:
                        print(f"\r{percent:.1f}% ({downloaded_kb:.1f}/{total_kb:.1f} KB) {elapsed_str}", end="", flush=True)
                else:
                    if verbose:
                        print(f"\r{downloaded_kb:.1f} KB {elapsed_str}", end="", flush=True)
            if verbose:
                print()

            return bytes(data)
        except:
            exception("Failed to download asset")
            return None
