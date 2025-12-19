🔒 PrisonVaults - The Ultimate Prison Core Solution
Vaults • Gangs • Economy • Jobs • Staff • Permissions • Pets • Combat Kits • Scoreboard

PrisonVaults is the complete ecosystem designed to power modern Prison servers. Built for Minecraft 1.21+, it combines massive player storage, a fully integrated Gangs system, a robust economy, a full Staff Management Suite, Combat-Ready Kits, Miniature Pets, and a built-in Permissions System.

Stop cluttering your server with 20 different plugins. Get the all-in-one solution that just works.

🔥 New in v1.1.4: "The Authority & Warden Update"
Granular Staff Management:

Split GUI System: The /staff command has been overhauled. You can now open specific views for Server Staff or Server Members directly via subcommands.

New Permissions: Added distinct permissions (prisonvaults.staff.serverstaff and prisonvaults.staff.servermember) so you can control exactly which lists your staff can view.

Protection Logic:

Owner Safety: Server Owners and Co-Owners are now immune to punishment via the GUI. The Ban, Kick, Warn, and IP-Ban buttons are automatically disabled when viewing top-ranking staff.

Rank Logic: Regular members cannot be promoted via the Staff GUI (preventing accidental promotions); they must be set via command first.

Advanced Punishment Tools:

IP-Banning: Added a Ban-IP button to the Staff Action Menu. This bans the target's IP address and their account simultaneously.

Visual Indicators: Punishment buttons now visually gray out (barrier/glass) if the action is not allowed on the specific target.

Pet & Gang Refinements:

Lil Warden Balance: Fixed the Darkness Effect logic.

Owner Immunity: The pet owner is never blinded by their own Warden.

PvE Safety: In PvE mode, the Warden will not blind innocent bystanders.

PvP Logic: In PvP mode, the darkness only affects enemy players.

Gang Ban Management: Gang leaders can now Left-Click a banned player's skull in the Gang Ban GUI to immediately unban them. This sends a notification to the Gang Owner confirming the action.

⚔️ Advanced Gang System
The most social feature of your server just got smarter.

GUI Management: Manage members, colors, and settings via a polished GUI.

Banishment Control: Gang leaders can ban players (offline or online). New: Simply Left-Click in the GUI to unban.

Smart Notifications: Members are notified via chat upon promotion, demotion, or kicks.

Deep Hierarchy: 7 Ranks: Member ➔ Hustler ➔ Brute ➔ Thug ➔ Shot Caller ➔ Elite ➔ Co-Leader.

🐾 Pets Companion System
Pets are no longer just cosmetic—they are fully functional combat companions using Minecraft 1.21's scaling technology.

RPG Mechanics: Pets have HP, levels, and gain XP on kills.

Tactical Modes: Aggressive vs Passive | Attack vs Defense.

Targeting: PvP (Players/Pets) vs PvE (Mobs Only).

"Lil" Mobs: Collect combat-ready tiny monsters like the Lil Warden, Lil Wither, Lil Phantom, and Lil Creeper.

Lil Warden Update: Smart Darkness effect handling based on combat mode.

🛠️ Commands & Arguments (v1.1.4)
🛡️ Staff & Admin (OP/Permission Required)
Management:

/staff serverstaff - Open the GUI listing only Server Staff.

/staff servermember - Open the GUI listing only Server Members.

/setstaff <player> <rank> - Set a player's staff role (Helper ➔ Owner).

/staffchat <message> - Talk in the private staff channel.

/staffmail <read|clear> - View staff notifications.

/pvannounce <message> - Broadcast a server-wide alert with sound.

/resetcooldown - Reset all cooldowns for all players.

Punishments:

/pvkick <player> [reason] - Kick player (Offline compatible).

/pvban <player> [reason] - Ban player (Offline compatible).

/pvwarn <player> <reason> - Issue a warning.

/pvpardon <player> - Unban a player.

Permissions & Config:

/pvperm group <name> <create|delete|add|remove> [permission]

/pvperm user <player> <setgroup|reset> [group]

/addmoney <player> <amount> - Admin command to inject money.

/createkit - Create a procedural kit in-game.

/pvconfig set <rob|pickpocket> <0.0-1.0> - Edit success chances.

/pvscoreboard <show|hide> - Toggle the scoreboard overlay.

/prisonvaults reload - Reload all configuration files.

👤 Player Essentials
/pv <number> - Open a specific personal vault.

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

🐾 Pets & Kits
/pets - Open your pet collection GUI (Summon/Manage).

/petshop - Open the shop to buy new pets.

/kit <name> - Equip a kit.

/kits - List all available kits.

/buykit - Open the Kit Shop GUI.

👤 Profile
/myprofile - View your stats card (K/D, Balance, Rank).

/whois <player> - View another player's profile.

/setbio <text> - Set your profile biography.

⚙️ Configuration Files
config.yml: Main settings, chances, and world definitions.

pets.yml: Player pet data and leveling stats.

permissions.yml: Custom group and permission definitions.

kits.yml: Kit contents, cooldowns, and ability definitions.

gangs.yml: Gang data, members, and ban lists.

ranks.yml: Rank ladder prices and display names.

prices.yml: Item sell prices for /sell.

Ready to revolutionize your Prison Server? Download PrisonVaults today!