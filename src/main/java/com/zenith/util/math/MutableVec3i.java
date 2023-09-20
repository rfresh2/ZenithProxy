package com.zenith.util.math;

import lombok.*;
import org.cloudburstmc.math.vector.Vector3i;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class MutableVec3i {
    private int x;
    private int y;
    private int z;

    public static MutableVec3i from(Vector3i vector3i) {
        return new MutableVec3i(vector3i.getX(), vector3i.getY(), vector3i.getZ());
    }
}
