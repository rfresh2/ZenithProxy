import hashlib
import os
import sys

launcher_tag = "launcher"


def update_launcher_exec(config, api):
    if not config.auto_update_launcher:
        return
    print("Checking for launcher update...")
    launcher_asset_id = api.get_release_tag_asset_id(launcher_tag, "launcher.py")
    if launcher_asset_id is None:
        print("Failed to get launcher asset ID")
        return
    launcher_asset_bytes = api.download_release_asset(launcher_asset_id)
    if launcher_asset_bytes is None:
        print("Failed to download launcher.py asset")
        return
    contents = launcher_asset_bytes.decode()
    if len(contents) < 100:
        print("Failed to decode launcher.py")
        return

    if os.path.exists("launcher.py"):
        with open("launcher.py", "r", newline='') as f:
            current = f.read()
            current_hash = hashlib.sha1(current.encode()).hexdigest()
            new_hash = hashlib.sha1(contents.encode()).hexdigest()
            if current_hash != new_hash:
                print("Updating launcher.py to " + new_hash)
                do_update = True
    else:
        print("No launcher.py found. Downloading from GitHub.")
        do_update = True

    if do_update:
        with open("launcher.py.tmp", "w", newline='') as f:
            f.write(contents)
        os.replace("launcher.py.tmp", "launcher.py")
        print("Updated launcher.py successfully!")
        print("Relaunching launcher...")
        # todo: handle native launcher executable update downloads and stuff above
        if sys.argv[0].endswith(".py"):
            os.execl(sys.executable, sys.executable, *sys.argv)
        else:
            os.execl(sys.argv[0], *sys.argv)
