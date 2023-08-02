package com.sparklicorn.tetrisai.ranking;

// import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.sparklicorn.bucket.tetris.TetrisState;
// import com.sparklicorn.tetrisai.GlobalState;

public abstract class RankingHeuristic {
    protected Map<Integer,Float> quantifyMap = new HashMap<>();

    public final String name;

    // private long cacheHits = 0L;

    public RankingHeuristic(String name) {
        this.name = name;
    }

    public float quantify(TetrisState state) {
        // TODO - debug caching before re-enabling
        // int stateKey = Arrays.hashCode(state.board);
        // if (quantifyMap.containsKey(stateKey)) {
        //     cacheHits++;
        //     float quantity = quantifyMap.get(stateKey);
        //     if (GlobalState.debugging) {
        //         // System.out.printf("%s cache hit! (%d); %d -> %f\n", name, cacheHits, stateKey, quantity);
        //     }
        //     return quantity;
        // }

        float quantity = quantifyImpl(state);
        // quantifyMap.put(stateKey, quantity);
        return quantity;
    }

    protected abstract float quantifyImpl(TetrisState state);
}
