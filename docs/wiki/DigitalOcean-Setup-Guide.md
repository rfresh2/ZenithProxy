[Create an account here ](https://m.do.co/c/f3afffef9a46)[for free $200 credit](https://m.do.co/c/3a3a226e4936)

## Create a Droplet (VPS)
![Create Droplet](./_assets/img/digitalocean-setup/DigitalOcean-Setup-Guide-1.png)

## Select OS and Specs
Select New York (any datacenter) for lowest ping to 2b2t and best connection reliability
![Select OS and Specs](./_assets/img/digitalocean-setup/DigitalOcean-Setup-Guide-2.png)

![OS and Plan Settings](./_assets/img/digitalocean-setup/DigitalOcean-Setup-Guide-3.png)

## Setup Authentication

Set a password, or an SSH key if you are familiar with them

![Authentication](./_assets/img/digitalocean-setup/DigitalOcean-Setup-Guide-4.png)

## Click Advanced Options
![Click Advanced Options](./_assets/img/digitalocean-setup/DigitalOcean-Setup-Guide-5.png)

## Click Add Initialization Scripts
![Click Add Initialization Scripts](./_assets/img/digitalocean-setup/DigitalOcean-Setup-Guide-6.png)

## Copy Paste the Setup script

Script link: https://github.com/rfresh2/ZenithProxy/blob/1.21.4/scripts/cloud-init.yaml

![Copy Paste the Setup script](./_assets/img/digitalocean-setup/DigitalOcean-Setup-Guide-7.png)

The setup script will automatically download the ZenithProxy launcher to `~/ZenithProxy`, and install recommended tools like `tmux`.

## Create Droplet
![Create Droplet](./_assets/img/digitalocean-setup/DigitalOcean-Setup-Guide-8.png)

Wait about 5-10 mins for the droplet to fully setup before proceeding.

## SSH to the droplet

Find and copy the droplet's IP address on the DigitalOcean homepage

![Droplet IP](./_assets/img/digitalocean-setup/DigitalOcean-Setup-Guide-9.png)


I recommend using [Windows Terminal](https://apps.microsoft.com/detail/9N0DX20HK701?hl=en-us&gl=US)

Open the terminal and type:

`ssh root@<IP>`

![SSH example](./_assets/img/digitalocean-setup/DigitalOcean-Setup-Guide-10.png)

After, it will prompt you for a password if one is set.

If so, type the password and press enter. The password input is hidden while you are typing.

## Setup and Launch ZenithProxy

Start a tmux session:

`tmux`

If you did this successfully you should see a big green bar appear at the bottom

![TMUX](./_assets/img/digitalocean-setup/DigitalOcean-Setup-Guide-11.png)

Change directories to the ZenithProxy folder:

`cd ZenithProxy`

Run the launcher:

`./launch`

During setup, select the `linux` platform:


![ZenithProxy Setup](./_assets/img/digitalocean-setup/DigitalOcean-Setup-Guide-12.png)

Complete the rest of the setup and you're done.

Refer to the other documentation pages for further help:

[Discord Bot Guide](Discord-Bot-Guide.md){ .md-button .md-button--primary }

[Commands](Commands.md){ .md-button .md-button--primary }

[Setup](Setup.md){ .md-button .md-button--primary }

for tmux help see: https://tmuxcheatsheet.com/
