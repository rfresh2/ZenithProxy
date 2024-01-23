import requests


class GitHubAPI:
    launch_config = None

    def __init__(self, launch_config):
        self.launch_config = launch_config

    def get_github_releases_base_url(self):
        if self.launch_config.repo_owner == "rfresh2" and self.launch_config.repo_name == "ZenithProxy":
            host = "github.2b2t.vc"
        else:
            host = "api.github.com"
        return f"https://{host}/repos/{self.launch_config.repo_owner}/{self.launch_config.repo_name}/releases"

    def get_github_base_headers(self):
        return {
            "User-Agent": "ZenithProxy/" + self.launch_config.local_version,
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "Connection": "close"
        }

    def get_latest_release_and_ver(self, channel):
        latest_release = None
        url = self.get_github_releases_base_url()
        params = {'per_page': 100}
        try:
            response = requests.get(url, headers=self.get_github_base_headers(), params=params)
            if response.status_code == 200:
                releases = response.json()
                for release in releases:
                    if release["draft"]:
                        continue
                    if release["tag_name"].endswith("+" + channel):
                        if latest_release is None or release["published_at"] > latest_release["published_at"]:
                            latest_release = release
            else:
                print("Failed to get releases:", response.status_code, response.reason)
        except Exception as e:
            print("Failed to get releases:", e)
        return (latest_release["id"], latest_release["tag_name"]) if latest_release else None

    def get_release_for_ver(self, target_version):
        found_version = False
        page = 1
        url = self.get_github_releases_base_url()
        try:
            while not found_version and page < 10:
                response = requests.get(url, headers=self.get_github_base_headers(),
                                        params={'per_page': 100, 'page': page})
                if response.status_code == 200:
                    releases = response.json()
                    for release in releases:
                        if release["draft"]:
                            continue
                        if release["tag_name"] == target_version:
                            return release["id"], release["tag_name"]
                else:
                    print("Failed to get releases:", response.status_code, response.reason)
                    break
                page += 1
        except Exception as e:
            print("Failed to get release for version:", target_version, e)

    def get_release_asset_id(self, release_id, asset_name):
        url = f"{self.get_github_releases_base_url()}/{release_id}"
        try:
            response = requests.get(url, headers=self.get_github_base_headers())
            if response.status_code == 200:
                release_data = response.json()
                asset_id = next(
                    (asset["id"] for asset in release_data["assets"] if asset["name"] == asset_name),
                    None
                )
                return asset_id
            else:
                print("Failed to get release asset ID:", response.status_code, response.reason)
                return None
        except Exception as e:
            print("Failed to get release asset ID:", e)
            return None

    def get_release_tag_asset_id(self, release_id, asset_name):
        url = f"{self.get_github_releases_base_url()}/tags/{release_id}"
        try:
            response = requests.get(url, headers=self.get_github_base_headers())
            if response.status_code == 200:
                release_data = response.json()
                asset_id = next(
                    (asset["id"] for asset in release_data["assets"] if asset["name"] == asset_name),
                    None
                )
                return asset_id
            else:
                print("Failed to get release asset ID:", response.status_code, response.reason)
                return None
        except Exception as e:
            print("Failed to get release asset ID:", e)
            return None

    def download_release_asset(self, asset_id):
        url = f"{self.get_github_releases_base_url()}/assets/{asset_id}"
        download_headers = self.get_github_base_headers()
        download_headers["Accept"] = "application/octet-stream"
        try:
            response = requests.get(url, headers=download_headers, allow_redirects=True)
            if response.status_code == 200:
                asset_data = response.content
                return asset_data
            else:
                print("Failed to download asset:", response.status_code, response.reason)
                return None
        except Exception as e:
            print("Failed to download asset:", e)
            return None
