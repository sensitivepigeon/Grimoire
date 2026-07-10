# QuestTome (Fabric 1.20.1)


## Features

***QuestTome*** is a fantasy questing/bounty mod for getting those harder to farm resources, built for custom mod pack makers and servers in mind!

It adds the **Book of Bargains**, an item players use to view available Bargains (bounty-style quests) for the taking. Accepting an offered bargain will put you in progress to earn its reward and additional glory! Higher tiers of bargains unlock over time and offer increasingly rare rewards, making consistent daily bounty completion a draw for active players looking to acquire difficult resources.

The mod is designed more for assistance in getting those harder-to-acquire niche resources you may not want to dive into the ocean or kill a bunch of rabbits for. This adds an alternative method of acquisition, perfect for mod makers who want to add a stop-gap to delayed progression and players who value their time!


### Player Features
- **The Book of Bargains**: An ethereal book that is crafted using an ender pearl and a writable book, which enables you to access the "Bargains" system. A mysterious entity inhabits the book, offering you bargains from across the realms from various patrons to fulfill for rewards. Greater rewards await for the diligent... but be careful: you never know who or what might be hiding their intentions from you, no matter their promised rewards.

- **How the Book Works:** A modeled, held book item that opens your Bargains UI on right click. The left page will hold your *accepted bargains,* up to three at a time from any mix of available tiers. Each show the required item as a handy icon and have a live "gathered" count in your inventory in real time! The right page will display available bounties for that tier and allows for paging through tiers to see what's on offer for that day.

- **Daily Rolls**: Offers a reroll each Minecraft day per player and also shuffles all offers each day! Different offers for each player. Anything you don't accept can vanish in the shuffle, so re-roll carefully. Anything you accepted is yours to fulfill and you remain committed no matter how many days pass. Careful, you can only pick 3 bargains at a time maximum! Each bounty is once-daily to complete, and can show back up later.

- **Tier Progression:** Five ranks! I-V ranks that offer rarities adjacent to the difficulty. Fulfilling bounties of your tier counts toward unsealing the next: locked tier pages will show you how many you need to complete. Lower tiers never close, so end-game players can still grab those niche items they might need later on. Tier IV only offers two bounties a day, and Tier V only offers one due to their exclusive nature.

### For Modders!!
- This resource was designed for YOU to edit and adjust for your modpacks, include custom items and adjust balancing for grinds!! You can also include lore!
- Quests are a JSON file! Easy structure, easy to copy and add your quests for custom content in the quests directory. Tier directories are organizational. Drop a file into your datapack and you can /reload for live adjustments!
- Progression lives in one file, the tiers.json! Override if you want to tune stuff like tiers or limits.
- Override model follows vanilla conventions: quests are per-file (which can be tedious sorry), config is one file (please be careful overriding it!) A malformed Quest JSON logs and error and skips it, doesn't disrupt the whole system so your typos are forgivable...
-  Multiplayer safe by construction. This was built for modpacks and servers. All states are server authoritative. Turn-ins, accepts, rerolls, tiers unlock - it's all on server side, quest data is synced on join. Everything is per-player!


### Future Stuff
 - art/design commissioned from actual artists. current icon/model I hand-made and looks bad
 - multiple item reward bundles
 - new quest types and chain quests across tiers with story
 - new ability to click on bargain and see full write up (optional)
 - The Codex that keeps past quest info 
 - quest making resource for modders and devs to solve tedious nature of inputting quests manually one-by-one.
 - first quest pack of handwritten quests with lore flavor
 - datapacks for popular modpacks
 - flavor packs for quests, such as Culinary

### Known Issues
- 1.0 was NOT validated to work with multiple players on the same server yet, but WAS validated to work *on* servers.


## Installation

***Requires Fabric loader for Minecraft 1.20.1***

Grab the jar from build/libs and drop in your mods folder. Make sure you have FabricAPI and correct minecraft version..

**Servers**: same config, same jar + dependencies. Drop in server's mod folder.

**QuestTome is required on both client and server.**


## Important Notes About AI
Transparency: AI assistance was used for first code iterations but I decided _I hate that_ so I rewrote stuff and I'm doing it myself... if it's broken, it's **human-broken** now!

**NO AI** was ever used for writing or art design and **NEVER** will be!!

Please do not use AI if you attempt to touch this project including feedback! Please be human or I can't help you...

## License
creative commons! please make edits, distribute, have fun. Please credit me for baseline stuff I'd appreciate it.
Artist credit goes here.
