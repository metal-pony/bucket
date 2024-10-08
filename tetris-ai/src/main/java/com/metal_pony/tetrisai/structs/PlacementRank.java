package com.metal_pony.tetrisai.structs;

import java.util.List;

import com.metal_pony.bucket.tetris.util.structs.Position;
import com.metal_pony.tetrisai.AiTetris;

public record PlacementRank(
    AiTetris game, // TODO #13 Remove if not needed.
    List<Position> placements,
    double rank
){};
