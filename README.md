🔒 PrisonVaults - The Ultimate Prison Core Solution
Vaults • Gangs • Economy • Jobs • Staff • Permissions • Pets • Combat Kits • Scoreboard

PrisonVaults is the complete ecosystem designed to power modern Prison servers. Built for Minecraft 1.21+, it combines massive player storage, a fully integrated Gangs system, a robust economy, a full Staff Management Suite, Combat-Ready Kits, Miniature Pets, and a built-in Permissions System.

Stop cluttering your server with 20 different plugins. Get the all-in-one solution that just works.

🔥 New in v1.1.2
Smart Tab Completion: Every command now features intelligent autocompletion. Arguments like players, kit names, gang names, shop types, and configuration settings now autofill instantly.

Pet AI Overhaul:

Hostile Pet Logic: Boss pets (Wither, Warden) and aggressive pets (Blaze, Skeleton) no longer target their owners. They use advanced vector-based movement to follow safely.

Safe Mode: Wither pets no longer destroy blocks with explosions, and Fireball pets (Ghast/Blaze) no longer create fire blocks (grief prevention).

Bee Upgrade: Bee pets now have infinite stingers—they do not die after attacking.

Axolotl Boost: Axolotls now move faster on land.

Targeting System: Added a PvE/PvP Toggle for pets. In PvE mode, pets will ignore players and only attack monsters.

New Pet: Added the Lil Phantom (Replaced Lil Happy Ghast).

XP Action Bar: Pet XP gains are now displayed cleanly in the action bar instead of spamming chat.

⚔️ Advanced Gang System
The most social feature of your server just got smarter.

GUI Management: Manage members, colors, and settings via a polished GUI.

Banishment Control: Gang leaders can ban players from their gang via the GUI ("Iron Bars" icon).

Smart Notifications: Members are notified via chat upon promotion, demotion, or kicks.

Safe Disband: Disbanding requires confirmation and alerts all online members.

Deep Hierarchy: 7 Ranks: Member -> Hustler -> Brute -> Thug -> Shot Caller -> Elite -> Co-Leader.

🐾 Pets Companion System
Pets are no longer just cosmetic—they are fully functional combat companions using Minecraft 1.21's scaling technology.

RPG Mechanics: Pets have HP, levels, and gain XP on kills (displayed in Action Bar).

Tactical Modes: Toggle between PvP (Aggressive) and PvE (Passive) modes via the GUI.

"Lil" Mobs: Collect combat-ready tiny monsters like the Lil Warden, Lil Wither, Lil Phantom, and Lil Blaze.

Custom AI: Pets like Bees and Wither Skeletons have custom attack logic tailored to assist the player without causing friendly fire.

📦 Infinite Storage Vaults & Economy
Rank-Based Limits: Players unlock more vaults as they rank up (Rank A = 1 Vault ... Rank Z = 104 Vaults).

Dynamic Autocomplete: /pv only suggests vault numbers you have actually unlocked.

Economy: Full support for decimals, massive numbers (Decillions), and transaction logging.

🛡️ Staff & Permissions
Zero Dependencies: You don't need LuckPerms. PrisonVaults handles groups and permissions natively.

Punishment Suite: Built-in Kick, Ban, Warn, and Pardon logic with history tracking.

Rank Safety: Owners/Co-Owners cannot be punished by lower staff.

🛠️ Commands & Arguments
👤 Player Essentials
/pv <number> - Open a specific personal vault (Autofills allowed numbers).

/sell or /sell <all|hand> - Sell items to the server.

/balance - Check your current wallet.

/pay <player> <amount> - Send money to another player.

/rankup - Advance to the next prison rank.

/rob <player> - Attempt to rob a player (Subject to cooldown & chance).

/colorify <style> - Set a gradient name color (e.g., Rainbow, Sunset).

/pvcompass - Toggle the navigation BossBar compass.

/pvhelp [page] - View the help menu.

/pvinfo - View plugin version information.

🩸 Gangs
/gang create <Tag> <Name> - Create a new gang.

/gang invite <player> - Invite a player to your gang.

/gang join <GangName> - Accept an invite.

/gang leave - Leave your current gang.

/gang info - View gang stats and leader.

/gang manage - Open the Gang Management GUI (Leaders only).

/gangs - List all gangs on the server.

/gangchat <message> - Send a message to gang members.

💼 Jobs & Shops
/job join <Cooking|Blacksmith> - Join a specific job.

/job quit - Quit your current job.

/job info - View your job level and XP.

/pvshop <buy|sell> <messhall|smithy> [page] - Open specific job shops.

🐾 Pets
/pets - Open your pet collection GUI (Summon/Despawn/Rename).

/petshop - Open the shop to buy new pets.

⚔️ Kits
/kit <name> - Equip a kit.

/kits - List all available kits.

/buykit - Open the Kit Shop GUI.

👤 Profile
/myprofile - View your stats card (K/D, Balance, Rank).

/whois <player> - View another player's profile.

/setbio <text> - Set your profile biography.

🛡️ Staff & Admin (OP/Permission Required)
Management:

/staff - Open the Staff Management GUI.

/setstaff <player> <rank> - Set a player's staff role (Helper -> Owner).

/staffchat <message> - Talk in the private staff channel.

/staffmail <read|clear> - View staff notifications.

/pvannounce <message> - Broadcast a server-wide alert with sound.

/resetcooldown - Reset all cooldowns for all players.

Punishments:

/pvkick <player> [reason]

/pvban <player> [reason]

/pvwarn <player> <reason>

/pvpardon <player>

Permissions:

/pvperm group <name> <create|delete|add|remove> [permission]

/pvperm user <player> <setgroup|reset> [group]

Configuration & Economy:

/addmoney <player> <amount> - Admin command to inject money.

/createkit <Name> <Color> <ToolMat> <ArmorMat> <EnchantLvl> <Price> - Create a procedural kit in-game.

/pvconfig set <rob|pickpocket> <0.0-1.0> - Edit success chances.

/pvconfig setworld <worldName> - Set the active world for Job scheduling.

/pvscoreboard <show|hide> - Toggle the scoreboard overlay.

/prisonvaults reload - Reload all configuration files.

⚙️ Configuration Files
config.yml: Main settings, chances, and world definitions.

pets.yml: Player pet data and leveling stats.

permissions.yml: Custom group and permission definitions.

kits.yml: Kit contents, cooldowns, and ability definitions.

gangs.yml: Gang data, members, and ban lists.

ranks.yml: Rank ladder prices and display names.

prices.yml: Item sell prices for /sell.

Ready to revolutionize your Prison Server? Download PrisonVaults today!