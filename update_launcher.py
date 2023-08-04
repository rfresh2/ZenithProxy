import hashlib
import http.client
import json
import os

with open("launch_config.json", "r") as f:
    launch_config = json.load(f)

auto_update = launch_config["auto_update"]
repo_owner = launch_config["repo_owner"]
repo_name = launch_config["repo_name"]
repo_branch = launch_config["repo_branch"]
launcher_tag = "launcher"

if not auto_update:
    exit(0)

github_headers = {
    "User-Agent": "ZenithProxy/1.0",
    "Accept": "application/vnd.github+json",
    "Authorization": f"Bearer {os.getenv('GITHUB_TOKEN')}",
    "X-GitHub-Api-Version": "2022-11-28",
    "Connection": "close"
}
payload = {}


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
        if response.status == 200:
            asset_data = response.read()
            print("Downloaded asset:", len(asset_data), "bytes")
            return asset_data
        else:
            print("Failed to download asset:", response.status, response.reason)
            return None
    except Exception as e:
        print("Failed to download asset:", e)
        return None
    finally:
        connection.close()


startAssetId = get_release_asset_id(launcher_tag, "start.py")
startAsset = download_release_asset(startAssetId)
contents = startAsset.decode("utf-8")
if len(contents) < 100:
    print("Failed to download start.py")
    exit(1)

with open("start.py", "r") as f:
    current = f.read()
    currentHash = hashlib.sha1(current.encode("utf-8")).hexdigest()
    newHash = hashlib.sha1(contents.encode("utf-8")).hexdigest()
    if currentHash != newHash:
        print("Updating to remote start.py...")
    else:
        exit(0)

with open("start.py", "w") as f:
    f.write(contents)
