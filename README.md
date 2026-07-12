# QuestTome (Fabric)


## Features
*Beta version 1.0 for Minecraft 1.20.1.*

***QuestTome*** is a fantasy questing/bounty mod for getting those harder to farm resources, built for custom mod pack makers and servers in mind!

It adds the **Book of Bargains**, an item players use to view available Bargains (bounty-style quests) for the taking. Accepting an offered bargain will put you in progress to earn its reward and additional glory! Higher tiers of bargains unlock over time and offer increasingly rare rewards, making consistent daily bounty completion a draw for active players looking to acquire difficult resources.

This adds an alternative method of acquisition of resources, perfect for mod makers who want to add a stop-gap to delayed progression and players who value their time!

Read the [wiki](https://github.com/sensitivepigeon/QuestTome/wiki) for more and the future features roadmap.

Post issues on Github [here.](https://github.com/sensitivepigeon/QuestTome/issues)

### Player Features

- **The Book of Bargains**: An ethereal book that is crafted using a diamond and a writable book, which enables you to access the "Bargains" system. A mysterious entity inhabits the book, offering you bargains from across the realms from various patrons to fulfill for rewards.

- **Daily Rolls**: Careful, you can only accept 3 bargains at a time! Anything else rerolls daily or with the press of a once-daily button. You have unlimited time to complete your bargains.

- **Tier Progression:** Five ranks! I-V ranks that offer rarities adjacent to the difficulty. Fulfilling bounties of your tier counts toward unsealing the next: locked tier pages will show you how many you need to complete.  Tier IV only offers two bounties a day, and Tier V only offers one due to their exclusive nature.

### For Modders!
- This resource was designed for YOU to edit and adjust for your modpacks, include custom items and adjust balancing for grinds!! You can also include lore!
- Quests are a JSON file! Easy structure, easy to copy and add your quests for custom content in the quests directory. Tier directories are organizational. Drop a file into your datapack and you can /reload for live adjustments!
- Progression lives in one file, the tiers.json! Override if you want to tune stuff like tiers or limits.
- Override model follows vanilla conventions: quests are per-file (which can be tedious sorry), config is one file (please be careful overriding it!) A malformed Quest JSON logs and error and skips it, doesn't disrupt the whole system so your typos are forgivable...
-  Multiplayer safe by construction. This was built for modpacks and servers. All states are server authoritative. Turn-ins, accepts, rerolls, tiers unlock - it's all on server side, quest data is synced on join. Everything is per-player!


## Installation

***Requires Fabric loader (0.18.4 or higher) for Minecraft 1.20.1***

Grab off Curseforge or Modrinth. Make sure you have FabricAPI and correct minecraft version..

**Servers**: same config, same jar + dependency. Drop in server's mod folder.

**QuestTome is required on both client and server.**

## Artist Credit
All writing done by me.

Amazing GUI / 3D Model by [Matija](https://matijasworkshop.framer.website/)! Linked to their workshop. Definitely commission them!

# Discord

Find me in the Curseforge/Modrinth/Fabric discords for now as @sensitivepigeon, please feel free to ask questions or send issues directly.

## Important Notes About AI
Transparency: AI assistance was used for first code iterations but I decided _I hate that_ so I rewrote stuff and I'm doing it myself... if it's broken, it's **human-broken** now!

**NO AI** was ever used for writing or art design and **NEVER** will be!

Please do not use AI if you attempt to touch this project including feedback! Please be human or I can't help you...
