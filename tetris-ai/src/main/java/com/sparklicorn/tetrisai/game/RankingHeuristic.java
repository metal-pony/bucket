package com.sparklicorn.tetrisai.game;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.sparklicorn.bucket.tetris.ITetrisGame;

public abstract class RankingHeuristic {
    protected Map<Integer,Float> quantifyMap = new HashMap<>();

    public final String name;

    public RankingHeuristic(String name) {
        this.name = name;
    }

    public float quantify(ITetrisGame gameState) {
        int area = gameState.getNumRows() * gameState.getNumCols();
        int[] blocks = gameState.getBlocksOnBoard(new int[area]);
        int stateHash = Arrays.hashCode(blocks);
        if (quantifyMap.containsKey(stateHash)) {
            return quantifyMap.get(stateHash);
        }

        float quantity = quantifyImpl(gameState);
        quantifyMap.put(stateHash, quantity);
        return quantity;
    }

    protected abstract float quantifyImpl(ITetrisGame gameState);
}
