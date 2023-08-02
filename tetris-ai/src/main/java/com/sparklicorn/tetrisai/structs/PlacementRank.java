package com.sparklicorn.tetrisai.structs;

import java.util.List;

import com.sparklicorn.bucket.tetris.util.structs.Position;
import com.sparklicorn.tetrisai.AiTetris;

public record PlacementRank(
    AiTetris game, // TODO #13 Remove if not needed.
    List<Position> placements,
    double rank
){};
