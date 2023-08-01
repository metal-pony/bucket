package com.sparklicorn.tetrisai.game;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.sparklicorn.bucket.tetris.TetrisState;

public abstract class RankingHeuristic {
    protected Map<Integer,Float> quantifyMap = new HashMap<>();

    public final String name;

    public RankingHeuristic(String name) {
        this.name = name;
    }

    public float quantify(TetrisState state) {
        int stateKey = Arrays.hashCode(state.board);
        if (quantifyMap.containsKey(stateKey)) {
            return quantifyMap.get(stateKey);
        }

        float quantity = quantifyImpl(state);
        quantifyMap.put(stateKey, quantity);
        return quantity;
    }

    protected abstract float quantifyImpl(TetrisState state);
}
