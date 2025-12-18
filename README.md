🔒 PrisonVaults - The Ultimate Prison Core Solution
Vaults • Gangs • Economy • Jobs • Staff • Permissions • Pets • Combat Kits • Scoreboard

PrisonVaults is the complete ecosystem designed to power modern Prison servers. Built for Minecraft 1.21+, it combines massive player storage, a fully integrated Gangs system, a robust economy, a full Staff Management Suite, Combat-Ready Kits, Miniature Pets, and a built-in Permissions System.

Stop cluttering your server with 20 different plugins. Get the all-in-one solution that just works.

🔥 New in v1.1.3: "The Discipline Update"
Strict Pet Control & Logic Overhaul:

Strict Passive Mode:

Master Safety Switch: When a pet is set to Passive, it is strictly harmless. It will never attack, shoot projectiles (Wither Skulls/Fireballs), or explode, even if provoked.

Instant Neutralization: Switching to Passive immediately clears all anger and active targets from the pet.

Combat Logic Hierarchy:

Decision Tree: Pets now follow a strict hierarchy: Aggression Mode (Master) ➔ Combat Style (Attack/Defense) ➔ Target Mode (PvE/PvP).

True Defense: In Defense Mode, pets only attack entities that actively hurt their owner. They no longer auto-scan for targets.

Creeper Pet Rework:

Respawn on Detonation: If a Creeper pet explodes (Aggressive Mode), it is treated as a "death." It deals damage to enemies, vanishes, and respawns automatically after the standard cooldown.

Anti-Grief: Explosions are visual only and do not break blocks.

Offline Player & Punishment Overhaul:

Offline Staff Management: You can now Kick, Ban, and Demote staff members even if they are offline.

Staff Rank Stripping: Banning or Kicking a staff member via the GUI or command now automatically strips their staff rank and resets their permissions group to default.

Offline Gang Bans: Gang Leaders can now ban players from their gang even if the target is offline.

Vanilla Command Override: Default Minecraft commands (like /kick, /ban, /op) are now hidden from normal players to ensure they use the PrisonVaults system.

Quality of Life:

Smart Tab Completion: Every command now features intelligent autocompletion for players, kits, gangs, and shops.

XP Action Bar: Pet XP gains are displayed cleanly in the action bar.

⚔️ Advanced Gang System
The most social feature of your server just got smarter.

GUI Management: Manage members, colors, and settings via a polished GUI.

Banishment Control: Gang leaders can ban players (offline or online) from their gang via the GUI ("Iron Bars" icon).

Smart Notifications: Members are notified via chat upon promotion, demotion, or kicks.

Deep Hierarchy: 7 Ranks: Member ➔ Hustler ➔ Brute ➔ Thug ➔ Shot Caller ➔ Elite ➔ Co-Leader.

🐾 Pets Companion System
Pets are no longer just cosmetic—they are fully functional combat companions using Minecraft 1.21's scaling technology.

RPG Mechanics: Pets have HP, levels, and gain XP on kills.

Tactical Modes:

Aggression: Aggressive (Combat Enabled) vs Passive (Strictly Harmless).

Style: Attack (Hunt Enemies) vs Defense (Protect Owner).

Targeting: PvP (Players/Pets) vs PvE (Mobs Only).

"Lil" Mobs: Collect combat-ready tiny monsters like the Lil Warden, Lil Wither, Lil Phantom, and Lil Creeper.

Custom AI: Unique logic for every pet type, including infinite stinger Bees and safe Wither projectiles.

🛡️ Staff & Permissions
Zero Dependencies: You don't need LuckPerms. PrisonVaults handles groups and permissions natively.

Punishment Suite: Built-in Kick, Ban, Warn, and Pardon logic with history tracking.

Rank Safety: Owners/Co-Owners cannot be punished by lower staff.

Vanilla Protection: Automatically hides vanilla commands (/ban, /kick) from non-staff and secures Admin commands (/gamemode, /give).

🛠️ Commands & Arguments
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

🛡️ Staff & Admin (OP/Permission Required)
Management:

/staff - Open the Staff Management GUI.

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

⚙️ Configuration Files
config.yml: Main settings, chances, and world definitions.

pets.yml: Player pet data and leveling stats.

permissions.yml: Custom group and permission definitions.

kits.yml: Kit contents, cooldowns, and ability definitions.

gangs.yml: Gang data, members, and ban lists.

ranks.yml: Rank ladder prices and display names.

prices.yml: Item sell prices for /sell.

Ready to revolutionize your Prison Server? Download PrisonVaults today!