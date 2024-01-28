#!/bin/bash

set -eu

CUR_USER=$(whoami)
HOME_DIR="$HOME"

echo "Updating OS packages..."
echo "=================================================="
sudo apt update
export DEBIAN_FRONTEND=noninteractive
sudo apt upgrade -y
sudo apt install tmux zsh unzip zip git python3 python3-pip -y
echo "Installing oh-my-zsh..."
echo "=================================================="
export RUNZSH=no
export CHSH=yes
sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"
echo "Installing sdkman..."
echo "=================================================="
curl -s "https://get.sdkman.io" | bash
/usr/bin/zsh -c 'source "$HOME_DIR/.sdkman/bin/sdkman-init.sh"'
echo "Installing Java..."
echo "=================================================="
/usr/bin/zsh -c 'sdk install java 21-graal'
echo "Downloading ZenithProxy Launcher to $HOME_DIR/ZenithProxy..."
echo "=================================================="
mkdir -p "$HOME_DIR/ZenithProxy"
cd "$HOME_DIR/ZenithProxy"
wget https://github.com/rfresh2/ZenithProxy/releases/download/launcher/ZenithProxyLauncher.zip
unzip ZenithProxyLauncher.zip
echo "=================================================="
echo "Setup complete!"
echo "ZenithProxy is downloaded to $HOME_DIR/ZenithProxy"
echo "Restart system (Recommended)? (y/n)"
read -r RESTART
if [ "$RESTART" = "y" ]; then
  sudo reboot
fi
