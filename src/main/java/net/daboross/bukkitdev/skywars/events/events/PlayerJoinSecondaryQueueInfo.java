/*
 * Copyright (C) 2013-2014 Dabo Ross <http://www.daboross.net/>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.daboross.bukkitdev.skywars.events.events;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;

public class PlayerJoinSecondaryQueueInfo {

    private final Player player;
    private final String queueName;

    public PlayerJoinSecondaryQueueInfo(final Player player, String queueName) {
        Validate.notNull(player, "Player cannot be null");
        this.player = player;
        this.queueName = queueName;
    }

    public Player getPlayer() {
        return player;
    }

    public String getQueueName() {
        return queueName;
    }

    @Override
    public String toString() {
        return "PlayerJoinSecondaryQueueInfo{" +
                "player=" + player +
                ", queueName='" + queueName + '\'' +
                '}';
    }
}
