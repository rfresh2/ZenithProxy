# Hosting Providers

ZenithProxy can run on any computer, including your own. But many choose to rent a computer in a datacenter (called a VPS or dedicated server)

These are some companies that offer hosting services (none are affiliated with ZenithProxy)

## Managed Providers

### Zenith Hosting

https://zenith.hosting

One-click instance creation with a web interface for setting up and managing ZenithProxy instances. No VPS or Linux knowledge required.

## Providers with Free Tiers/Trials

### DigitalOcean

[Setup guide](DigitalOcean-Setup-Guide.md)

60-day trial with $200 free credits.

### Oracle Cloud

https://www.oracle.com/cloud/free/

"Always Free" tier includes 2 of each x64 and ARM "Compute Instances"

??? "Firewall Help"
    Setup the instance, then run these commands via SSH:
    ``` bash title="shell"
    sudo iptables -I INPUT -j ACCEPT
    sudo su
    iptables-save > /etc/iptables/rules.v4
    exit
    ```
    source: https://www.reddit.com/r/oraclecloud/comments/r8lkf7/a_quick_tips_to_people_who_are_having_issue/

### AWS

https://aws.amazon.com/free/

6 month Free Plan includes $200 in credits

??? "Firewall Help"
    Configure the "Security Group" of your EC2 instance to allow inbound TCP traffic:
    ![](./_assets/img/hosting-providers/aws-security-group.png)


### Google Cloud

https://cloud.google.com/free

90-day free trial with $300 credit.

And infinite Free Tier with 1 `e2-micro` instance

??? "Firewall Help"
    Edit the network attached to your VM and add a firewall rule that allows all inbound traffic:
    ![](./_assets/img/hosting-providers/google-cloud-firewall-rule.png)

## Paid-only Providers

### OVH

https://us.ovhcloud.com/vps/

VPS stock is usually limited, but very competitive prices for the specs.

Allows purchasing additional IP's which is useful for getting around 2b2t's IP limits.

### Hetzner

https://www.hetzner.com/cloud/

Primarily EU only, which will have higher ping to 2b2t

### Vultr

https://www.vultr.com/
