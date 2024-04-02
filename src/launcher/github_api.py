import requests


class GitHubAPI:
    launch_config = None

    def __init__(self, launch_config):
        self.launch_config = launch_config

    def get_base_url(self):
        if self.launch_config.repo_owner == "rfresh2" and self.launch_config.repo_name == "ZenithProxy":
            host = "github.2b2t.vc"
        else:
            host = "api.github.com"
        return f"https://{host}/repos/{self.launch_config.repo_owner}/{self.launch_config.repo_name}/releases"

    def get_headers(self):
        return {
            "User-Agent": "ZenithProxy/" + self.launch_config.local_version,
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "Connection": "close",
        }

    def get_releases(self, params):
        try:
            response = requests.get(self.get_base_url(), headers=self.get_headers(), params=params, timeout=10)
            return response.json() if response.status_code == 200 else None
        except Exception as e:
            print("Failed to get releases:", e)
            return None

    def get_latest_release_and_ver(self, channel):
        releases = self.get_releases({"per_page": 100})
        if releases:
            latest_release = max(
                (r for r in releases if not r["draft"] and r["tag_name"].endswith("+" + channel)),
                key=lambda r: r["published_at"],
                default=None,
            )
            return (latest_release["id"], latest_release["tag_name"]) if latest_release else None

    def get_release_for_ver(self, target_version):
        for page in range(1, 10):
            releases = self.get_releases({"per_page": 100, "page": page})
            if releases:
                for release in releases:
                    if not release["draft"] and release["tag_name"] == target_version:
                        return release["id"], release["tag_name"]

    def get_asset_id(self, release_id, asset_name, tag=False):
        url = f"{self.get_base_url()}/{'tags/' if tag else ''}{release_id}"
        try:
            response = requests.get(url, headers=self.get_headers(), timeout=10)
            if response.status_code == 200:
                return next((asset["id"] for asset in response.json()["assets"] if asset["name"] == asset_name), None)
        except Exception as e:
            print("Failed to get release asset ID:", e)
        return None

    def get_release_asset_id(self, release_id, asset_name):
        return self.get_asset_id(release_id, asset_name)

    def get_release_tag_asset_id(self, release_id, asset_name):
        return self.get_asset_id(release_id, asset_name, True)

    def download_asset(self, asset_id):
        url = f"{self.get_base_url()}/assets/{asset_id}"
        download_headers = self.get_headers()
        download_headers["Accept"] = "application/octet-stream"
        try:
            response = requests.get(url, headers=download_headers, allow_redirects=True, timeout=10)
            if response.status_code == 200:
                return response.content
            else:
                print("Failed to download asset:", response.status_code, response.reason)
                return None
        except Exception as e:
            print("Failed to download asset:", e)
            return None
