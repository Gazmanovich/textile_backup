/*
 * A simple backup mod for Fabric
 * Copyright (C) 2020  Szum123321
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.szum123321.textile_backup.core.create;

import net.minecraft.server.MinecraftServer;
import net.szum123321.textile_backup.Globals;
import net.szum123321.textile_backup.config.ConfigHelper;
import net.szum123321.textile_backup.core.ActionInitiator;

import java.time.Instant;

/**
 * Runs backup on a preset interval
 * <br>
 * The important thing to note: <br>
 * In the case that <code>doBackupsOnEmptyServer == false</code> and there have been made backups with players online,
 * then everyone left the backup that was scheduled with player is still going to run. So it might appear as though there
 * has been made backup with no players online despite the config. This is the expected behaviour
 * <br>
 * Furthermore, it uses system time
 */
public class BackupScheduler {
    private final static ConfigHelper config = ConfigHelper.INSTANCE;

    //Scheduled flag tells whether we have decided to run another backup
    private static boolean scheduled = false;
    private static long nextBackup = - 1;

    public static void tick(MinecraftServer server) {
        if(config.get().backupInterval < 1) return;
        long now = Instant.now().getEpochSecond();

        if(config.get().doBackupsOnEmptyServer || server.getPlayerManager().getCurrentPlayerCount() > 0) {
            //Either just run backup with no one playing or there's at least one player
            if(scheduled) {
                if(nextBackup <= now) {
                    //It's time to run
                    Globals.INSTANCE.getQueueExecutor().submit(
                            MakeBackupRunnableFactory.create(
                                    BackupContext.Builder
                                            .newBackupContextBuilder()
                                            .setServer(server)
                                            .setInitiator(ActionInitiator.Timer)
                                            .saveServer()
                                            .build()
                            )
                    );

                    nextBackup = now + config.get().backupInterval;
                }
            } else {
                //Either server just started or a new player joined after the last backup has finished
                //So let's schedule one some time from now
                nextBackup = now + config.get().backupInterval;
                scheduled = true;
            }
        } else if(!config.get().doBackupsOnEmptyServer && server.getPlayerManager().getCurrentPlayerCount() == 0) {
            //Do the final backup. No one's on-line and doBackupsOnEmptyServer == false
            if(scheduled && nextBackup <= now) {
                //Verify we hadn't done the final one and its time to do so
                Globals.INSTANCE.getQueueExecutor().submit(
                        MakeBackupRunnableFactory.create(
                                BackupContext.Builder
                                        .newBackupContextBuilder()
                                        .setServer(server)
                                        .setInitiator(ActionInitiator.Timer)
                                        .saveServer()
                                        .build()
                        )
                );

                scheduled = false;
            }
        }
    }
}