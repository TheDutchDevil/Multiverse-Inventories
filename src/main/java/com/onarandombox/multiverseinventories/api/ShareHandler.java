package com.onarandombox.multiverseinventories.api;

import com.onarandombox.multiverseinventories.MultiverseInventories;
import com.onarandombox.multiverseinventories.permission.MVIPerms;
import com.onarandombox.multiverseinventories.profile.PersistingProfile;
import com.onarandombox.multiverseinventories.profile.ProfileContainer;
import com.onarandombox.multiverseinventories.profile.SimplePersistingProfile;
import com.onarandombox.multiverseinventories.profile.WorldProfile;
import com.onarandombox.multiverseinventories.share.Sharable;
import com.onarandombox.multiverseinventories.share.Shares;
import com.onarandombox.multiverseinventories.share.SimpleShares;
import com.onarandombox.multiverseinventories.util.MVILog;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Simple implementation of ShareHandler.
 */
public class ShareHandler {

    private List<PersistingProfile> fromProfiles;
    private List<PersistingProfile> toProfiles;
    private Player player;
    private World fromWorld;
    private World toWorld;
    private MultiverseInventories plugin;
    private boolean hasBypass = false;

    public ShareHandler(MultiverseInventories plugin, Player player,
                        World fromWorld, World toWorld) {
        this.fromProfiles = new ArrayList<PersistingProfile>();
        this.toProfiles = new ArrayList<PersistingProfile>();
        this.player = player;
        this.fromWorld = fromWorld;
        this.toWorld = toWorld;
        this.plugin = plugin;
    }

    /**
     * @return The profiles for the world/groups the player is coming from.
     */
    public final List<PersistingProfile> getFromProfiles() {
        return this.fromProfiles;
    }

    /**
     * @return The profiles for the world/groups the player is going to.
     */
    public List<PersistingProfile> getToProfiles() {
        return this.toProfiles;
    }

    /**
     * @return The world travelling from.
     */
    public World getFromWorld() {
        return this.fromWorld;
    }

    /**
     * @return The world travelling to.
     */
    public World getToWorld() {
        return this.toWorld;
    }

    /**
     * @return The player involved in this sharing transaction.
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * @param container The group/world the player's data is associated with.
     * @param shares    What from this group needs to be saved.
     * @param profile   The player profile that will need data saved to.
     */
    private void addFromProfile(ProfileContainer container, Shares shares, PlayerProfile profile) {
        this.getFromProfiles().add(new SimplePersistingProfile(container.getDataName(), shares, profile));
    }

    /**
     * @param container The group/world the player's data is associated with.
     * @param shares    What from this group needs to be loaded.
     * @param profile   The player profile that will need data loaded from.
     */
    private void addToProfile(ProfileContainer container, Shares shares, PlayerProfile profile) {
        this.getToProfiles().add(new SimplePersistingProfile(container.getDataName(), shares, profile));
    }

    /**
     * Finalizes the transfer from one world to another.  This handles the switching
     * inventories/stats for a player and persisting the changes.
     */
    public void handleSharing() {
        MVILog.debug("=== " + this.getPlayer().getName() + " traveling from world: " + this.getFromWorld().getName()
                + " to " + "world: " + this.getToWorld().getName() + " ===");
        // Grab the profile from the world they're coming from to save their stuff to every time.
        WorldProfile fromWorldProfile = this.plugin.getProfileManager()
                .getWorldProfile(this.getFromWorld().getName());
        this.addFromProfile(fromWorldProfile, new SimpleShares(Sharable.all()),
                fromWorldProfile.getPlayerData(this.getPlayer()));

        if (MVIPerms.BYPASS_WORLD.hasBypass(this.getPlayer(),
                this.getToWorld().getName(), this.plugin.getGroupManager())) {
            this.hasBypass = true;
            completeSharing();
            return;
        }

        // Get any groups we need to save stuff to.
        List<WorldGroup> fromWorldGroups = this.plugin.getGroupManager()
                .getGroupsForWorld(this.getFromWorld().getName());
        for (WorldGroup fromWorldGroup : fromWorldGroups) {
            PlayerProfile profile = fromWorldGroup.getPlayerData(this.getPlayer());
            if (!fromWorldGroup.containsWorld(this.getToWorld().getName())) {
                this.addFromProfile(fromWorldGroup,
                        new SimpleShares(Sharable.all()), profile);
            } else {
                if (!fromWorldGroup.getShares().isSharing(Sharable.all())) {
                    EnumSet<Sharable> sharing =
                            EnumSet.complementOf(fromWorldGroup.getShares().getSharables());
                    this.addFromProfile(fromWorldGroup, new SimpleShares(sharing), profile);
                }
            }
        }
        if (fromWorldGroups.isEmpty()) {
            MVILog.debug("No groups for fromWorld.");
        }

        List<WorldGroup> toWorldGroups = this.plugin.getGroupManager()
                .getGroupsForWorld(this.getToWorld().getName());
        if (!toWorldGroups.isEmpty()) {
            // Get groups we need to load from
            for (WorldGroup toWorldGroup : toWorldGroups) {
                if (MVIPerms.BYPASS_GROUP.hasBypass(this.getPlayer(),
                        toWorldGroup.getName(), this.plugin.getGroupManager())) {
                    this.hasBypass = true;
                } else {
                    PlayerProfile profile = toWorldGroup.getPlayerData(this.getPlayer());
                    if (!toWorldGroup.containsWorld(this.getFromWorld().getName())) {
                        this.addToProfile(toWorldGroup,
                                new SimpleShares(Sharable.all()), profile);
                    } else {
                        if (!toWorldGroup.getShares().isSharing(Sharable.all())) {
                            EnumSet<Sharable> shares =
                                    EnumSet.complementOf(toWorldGroup.getShares().getSharables());
                            this.addToProfile(toWorldGroup,
                                    new SimpleShares(shares), profile);
                        }
                    }
                }
            }
        } else {
            // Get world we need to load from.
            MVILog.debug("No groups for toWorld.");
            WorldProfile toWorldProfile = this.plugin.getProfileManager()
                    .getWorldProfile(this.getToWorld().getName());
            this.addToProfile(toWorldProfile, new SimpleShares(Sharable.all()),
                    toWorldProfile.getPlayerData(this.getPlayer()));
        }

        this.completeSharing();
    }

    private void completeSharing() {
        MVILog.debug("Travel affected by " + this.getFromProfiles().size() + " fromProfiles and "
                + this.getToProfiles().size() + " toProfiles");
        // This if statement should never happen, really.
        if (this.getToProfiles().isEmpty()) {
            if (hasBypass) {
                MVILog.debug(this.getPlayer().getName() + " has bypass permission for 1 or more world/groups!");
            } else {
                MVILog.debug("No toProfiles...");
            }
            if (!this.getFromProfiles().isEmpty()) {
                updateProfile(this.getFromProfiles().get(0));
            } else {
                MVILog.warning("No fromWorld to save to");
            }
            MVILog.debug("=== " + this.getPlayer().getName() + "'s travel handling complete! ===");
            return;
        }

        for (PersistingProfile persistingProfile : this.getFromProfiles()) {
            updateProfile(persistingProfile);
        }
        for (PersistingProfile persistingProfile : this.getToProfiles()) {
            updatePlayer(persistingProfile);
        }
        MVILog.debug("=== " + this.getPlayer().getName() + "'s travel handling complete! ===");
    }

    private void updateProfile(PersistingProfile profile) {
        for (Sharable sharable : profile.getShares().getSharables()) {
            sharable.updateProfile(profile.getProfile(), this.getPlayer());
        }
        MVILog.debug("Persisting: " + profile.getShares().toString() + " to "
                + profile.getProfile().getType() + ":" + profile.getDataName()
                + " for player " + profile.getProfile().getPlayer().getName());
        this.plugin.getData().updatePlayerData(profile.getDataName(), profile.getProfile());
    }

    private void updatePlayer(PersistingProfile profile) {
        for (Sharable sharable : profile.getShares().getSharables()) {
            sharable.updatePlayer(this.getPlayer(), profile.getProfile());
        }
        MVILog.debug("Updating " + profile.getShares().toString() + " for "
                + profile.getProfile().getPlayer().getName() + "for "
                + profile.getProfile().getType() + ":" + profile.getDataName());
    }
}
