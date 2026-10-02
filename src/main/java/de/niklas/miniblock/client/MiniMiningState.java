package de.niklas.miniblock.client;

import de.niklas.miniblock.world.MiniCell;

/** Read-only access to the active vanilla mining timer for cell-sized crack rendering. */
public interface MiniMiningState {
    MiniCell miniblock$miningCell();
    int miniblock$miningStage();
}
