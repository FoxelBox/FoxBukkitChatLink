/**
 * This file is part of FoxBukkitChatLink.
 *
 * FoxBukkitChatLink is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoxBukkitChatLink is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FoxBukkitChatLink.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.foxelbox.foxbukkit.chatlink.commands;

import com.foxelbox.foxbukkit.chatlink.Main;
import com.foxelbox.foxbukkit.chatlink.Player;
import com.foxelbox.foxbukkit.chatlink.bans.BanResolver;
import com.foxelbox.foxbukkit.chatlink.bans.LogEntry;
import com.foxelbox.foxbukkit.chatlink.commands.system.ICommand;
import com.foxelbox.foxbukkit.chatlink.json.ChatMessageIn;
import com.foxelbox.foxbukkit.chatlink.json.ChatMessageOut;
import com.foxelbox.foxbukkit.chatlink.util.CommandException;
import com.foxelbox.foxbukkit.chatlink.util.PlayerHelper;
import com.foxelbox.foxbukkit.chatlink.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

@ICommand.Names({"who", "list"})
@ICommand.Help("Prints user list if used without parameters or information about the specified user")
@ICommand.Usage("")
@ICommand.Permission("foxbukkit.who")
public class ListCommand extends ICommand {
	private static final String LIST_FORMAT = "<color name=\"dark_purple\">[FBCL]</color> <color name=\"dark_gray\">[%1$s]</color> %2$s";

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");

	@Override
	public ChatMessageOut run(final Player commandSender, final ChatMessageIn messageIn, String formattedName, String[] args) throws CommandException {
		if(args.length > 0) {
			final Player target = PlayerHelper.matchPlayerSingle(args[0], false);

			ChatMessageOut reply = makeReply(messageIn);

			reply.setContentsPlain("\u00a75[FBCL]\u00a7f Name: " + target.getName());
			Main.chatQueueHandler.sendMessage(reply);

			reply.setContentsPlain("\u00a75[FBCL]\u00a7f Rank: " + PlayerHelper.getPlayerRank(target.getUniqueId()));
			Main.chatQueueHandler.sendMessage(reply);

			reply.setContentsPlain("\u00a75[FBCL]\u00a7f NameTag: " + PlayerHelper.getFullPlayerName(target.getUniqueId(), target.getName()));

			if(commandSender.hasPermission("foxbukkit.who.logdetails")) {
				Main.chatQueueHandler.sendMessage(reply);
				new Thread() {
					public void run() {
						ChatMessageOut reply = makeReply(messageIn);
						LogEntry logEntryLogout = BanResolver.getLatestEntry(target.getName(), target.getUniqueId(), "logout", messageIn.server);
						LogEntry logEntry = BanResolver.getLatestEntry(target.getName(), target.getUniqueId(), null, messageIn.server);

						if(logEntryLogout == null) {
							reply.setContentsPlain("\u00a75[FBCL]\u00a7f Last logout data not present");
						} else {
							reply.setContentsPlain("\u00a75[FBCL]\u00a7f Last logout: " + DATE_FORMAT.format(logEntryLogout.getTime()));
						}
						Main.chatQueueHandler.sendMessage(reply);

						if(logEntry == null) {
							reply.setContentsPlain("\u00a75[FBCL]\u00a7f IP data not present");
						} else {
							reply.setContentsPlain("\u00a75[FBCL]\u00a7f Last IP: " + logEntry.getIp().getHostAddress());
						}
						reply.finalizeContext = true;
						Main.chatQueueHandler.sendMessage(reply);
					}
				}.start();
				return null;
			}

			return reply;
		}

		ChatMessageOut message = makeReply(messageIn);
		for(String server : PlayerHelper.getAllServers()) {
			List<Player> players = PlayerHelper.getOnlinePlayersOnServer(server);
			String listText;
			if(players.isEmpty()) {
				listText = "\u00a7fEmpty";
			} else {
				final List<String> names = new LinkedList<>();
				for(Player ply : PlayerHelper.getOnlinePlayersOnServer(server)) {
					names.add(PlayerHelper.getPlayerRankTagRaw(ply.getUniqueId()) + ply.getName());
				}
				Collections.sort(names, new NameComparator());

				listText = Utils.joinList(names, "\u00a7f, ");
			}
			message.setContents(LIST_FORMAT, new String[]{server, listText});
			Main.chatQueueHandler.sendMessage(message);
		}

		return makeBlank(messageIn);
	}

	private static class NameComparator implements Comparator<String> {
		@Override
		public int compare(String a, String b) {
			final String strippedA = PlayerHelper.stripColor(a);
			final String strippedB = PlayerHelper.stripColor(b);

			return strippedA.compareToIgnoreCase(strippedB);
		}
	}
}
