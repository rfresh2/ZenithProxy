This wiki will outline recommended tools, workflows, and tips for hosting ZenithProxy on a linux server.

## Hosting Providers

I recommend DigitalOcean `1 GB Memory / 1 vCPU` droplets in NYC-1. [Free DigitalOcean $200 credit ](https://m.do.co/c/f3afffef9a46)[for new accounts](https://m.do.co/c/3a3a226e4936). Each droplet is $6/month.

Additional providers:

* Google Cloud
* OVH
* Linode

### A note on other providers

There are other small VPS providers that purport to offer more hardware specs (more CPU cores, more memory) at very low prices. If it sounds too good to be true, it probably is. Most of the time I recommend avoiding these providers for the following reasons:

1. Old and slow hardware - A server may come with more CPU cores but still be slower overall due to using CPU models from more than a decade ago. Like in Minecraft servers, single core performance will have a large impact on performance. Old hardware will also lead to more compatibility issues
2. Lack of support - large cloud providers like DigitalOcean employ full-time teams of people to offer support and provide many services like 1-click backups and restores.
3. Location - Hosting your proxy as close to 2b2t's servers as possible is key to reducing your ping. Large providers will have multiple locations to select from. 2b2t servers are currently on the US East but change locations occasionally.

## Linux Distributions

I recommend using Ubuntu but most will work just as fine. e.g. Debian, CentOS, Fedora.

The main differences for this context will be the package managers and applications pre-installed.

Ubuntu has the most available documentation searchable on google so it will be the easiest to learn for inexperienced users.

## Recommended Command-line Tools

* tmux - A terminal multiplexer. You can open and detach multiple terminal sessions. This means you don't need to be constantly SSH'd in for your terminal applications to continue running.
  * [tmux commands cheat sheet](https://tmuxcheatsheet.com/)
* vim - terminal text editor.
  * [vim commands cheat sheet](https://devhints.io/vim)
* sdkman - manage the java versions installed and used on your system
* unzip - unzip zip files easily: `unzip <zip file>`

## Terminal basics

* installing applications on ubuntu: `sudo apt install tmux`
* `ssh <ip>` to remotely connect to your server
* `cd <directory>` will change your current directory
* `ls` print the current directory contents
* Ctrl-c keys to close the current application
* `mv <file> <destination>` move a file to a destination directory
* `mkdir <dir name>` create a directory
* `wget https://github.com/rfresh2/ZenithProxy/releases/download/launcher-v3/ZenithProxy-launcher-linux-amd64.zip` Download a file from the internet
* `cp <file> <destination>` copy a file to a destination file


