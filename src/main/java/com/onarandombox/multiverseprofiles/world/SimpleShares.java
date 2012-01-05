package com.onarandombox.multiverseprofiles.world;

/**
 * @author dumptruckman
 */
public class SimpleShares implements SharesI {

    private boolean sharingInventory = false;
    private boolean sharingHealth = false;
    private boolean sharingHunger = false;
    private boolean sharingExp = false;
    private boolean sharingEffects = false;


    public SimpleShares() {
    }

    public SimpleShares(SharesI shares) {
        this(shares.isSharingInventory(), shares.isSharingHealth(), shares.isSharingExp(),
                shares.isSharingHunger(), shares.isSharingEffects());
    }

    public SimpleShares(boolean sharingInventory, boolean sharingHealth, boolean sharingHunger,
                        boolean sharingExp, boolean sharingEffects) {
        this.sharingInventory = sharingInventory;
        this.sharingHealth = sharingHealth;
        this.sharingHunger = sharingHunger;
        this.sharingExp = sharingExp;
        this.sharingEffects = sharingEffects;
    }

    public void mergeShares(SharesI newShares) {
        this.setSharingInventory(newShares.isSharingInventory());
        this.setSharingHealth(newShares.isSharingHealth());
        this.setSharingHunger(newShares.isSharingHunger());
        this.setSharingExp(newShares.isSharingExp());
        this.setSharingEffects(newShares.isSharingEffects());
    }

    public boolean isSharingInventory() {
        return sharingInventory;
    }

    public void setSharingInventory(boolean sharingInventory) {
        this.sharingInventory = sharingInventory;
    }

    public boolean isSharingHealth() {
        return sharingHealth;
    }

    public void setSharingHealth(boolean sharingHealth) {
        this.sharingHealth = sharingHealth;
    }

    public boolean isSharingHunger() {
        return sharingHunger;
    }

    public void setSharingHunger(boolean sharingHunger) {
        this.sharingHunger = sharingHunger;
    }

    public boolean isSharingExp() {
        return sharingExp;
    }

    public void setSharingExp(boolean sharingExp) {
        this.sharingExp = sharingExp;
    }

    public boolean isSharingEffects() {
        return sharingEffects;
    }

    public void setSharingEffects(boolean sharingEffects) {
        this.sharingEffects = sharingEffects;
    }
}