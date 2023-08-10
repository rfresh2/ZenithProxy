import hashlib
import http.client
import json
import os
from urllib.parse import urlparse

with open("launch_config.json", "r") as f:
    launch_config = json.load(f)

release_channel = launch_config["release_channel"]
auto_update_launcher = launch_config["auto_update_launcher"]
repo_owner = launch_config["repo_owner"]
repo_name = launch_config["repo_name"]
repo_branch = launch_config["repo_branch"]
launcher_tag = "launch"

if not auto_update_launcher or release_channel == "git":
    exit(0)

github_headers = {
    "User-Agent": "ZenithProxy/1.0",
    "Accept": "application/vnd.github+json",
    "X-GitHub-Api-Version": "2022-11-28",
    "Connection": "close"
}

if os.getenv('GITHUB_TOKEN') is None:
    # todo: remove token requirement after repo is public
    print("GITHUB_TOKEN environment variable not found")
    exit(1)
else:
    github_headers["Authorization"] = f"Bearer {os.getenv('GITHUB_TOKEN')}"


def get_release_asset_id(tag, asset_name):
    url = f"/repos/{repo_owner}/{repo_name}/releases/tags/{tag}"
    try:
        connection = http.client.HTTPSConnection("api.github.com")
        connection.request("GET", url, headers=github_headers)
        response = connection.getresponse()
        if response.status == 200:
            release_data = json.loads(response.read())
            asset_id = next(
                (asset["id"] for asset in release_data["assets"] if asset["name"] == asset_name),
                None
            )
            return asset_id
        else:
            print("Failed to get release asset ID:", response.status, response.reason)
            return None
    except Exception as e:
        print("Failed to get release asset ID:", e)
        return None
    finally:
        connection.close()


def download_release_asset(asset_id):
    url = f"/repos/{repo_owner}/{repo_name}/releases/assets/{asset_id}"
    try:
        connection = http.client.HTTPSConnection("api.github.com")
        download_headers = github_headers.copy()
        download_headers["Accept"] = "application/octet-stream"
        connection.request("GET", url, headers=download_headers)
        response = connection.getresponse()
        # Follow redirects
        while response.status // 100 == 3:
            redirect_location = response.getheader('Location')
            connection.close()

            # Parse the redirect URL to extract the new host
            redirect_url = urlparse(redirect_location)
            redirect_host = redirect_url.netloc

            # Reopen connection to the new host
            connection = http.client.HTTPSConnection(redirect_host)
            connection.request("GET", redirect_location, headers=download_headers)
            response = connection.getresponse()
        if response.status == 200:
            asset_data = response.read()
            return asset_data
        else:
            print("Failed to download asset:", response.status, response.reason)
            return None
    except Exception as e:
        print("Failed to download asset:", e)
        return None
    finally:
        connection.close()


launcherAssetID = get_release_asset_id(launcher_tag, "launcher.py")
if launcherAssetID is None:
    exit(1)
launcherAssetBytes = download_release_asset(launcherAssetID)
if launcherAssetBytes is None:
    exit(1)
contents = launcherAssetBytes.decode()
if len(contents) < 100:
    print("Failed to download launcher.py")
    exit(1)

with open("launcher.py", "r", newline='') as f:
    current = f.read()
    currentHash = hashlib.sha1(current.encode()).hexdigest()
    newHash = hashlib.sha1(contents.encode()).hexdigest()
    if currentHash != newHash:
        print("Updating launcher.py to " + newHash)
    else:
        exit(0)

with open("launcher.py", "w", newline='') as f:
    f.write(contents)
