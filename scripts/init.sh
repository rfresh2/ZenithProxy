#!/usr/bin/env bash
set -euox pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "Please run as root (sudo)."
  exit 1
fi

export DEBIAN_FRONTEND=noninteractive

apt update
apt upgrade -y
apt install -y tmux zsh unzip zip wget

cat > /root/.tmux.conf <<'EOF'
set -g mouse on
EOF

ZSH_PATH=$(command -v zsh || echo /usr/bin/zsh)
if [ -x "$ZSH_PATH" ]; then
  chsh -s "$ZSH_PATH" root || true
fi

cd /root
wget -q -O /root/install.sh https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh
chmod +x /root/install.sh
"$ZSH_PATH" -c "/root/install.sh --unattended" || true
rm -f /root/install.sh

mkdir -p /root/ZenithProxy
cd /root/ZenithProxy
wget -q -O ZenithProxy-launcher-linux-amd64.zip https://github.com/rfresh2/ZenithProxy/releases/download/launcher-v3/ZenithProxy-launcher-linux-amd64.zip
unzip -o ZenithProxy-launcher-linux-amd64.zip

echo "Setup complete."
echo "Rebooting... You will need to reconnect..."
sleep 5
systemctl reboot
