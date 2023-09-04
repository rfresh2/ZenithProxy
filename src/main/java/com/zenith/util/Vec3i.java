package com.zenith.util;

import lombok.*;
import org.cloudburstmc.math.vector.Vector3i;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class Vec3i {
    private int x;
    private int y;
    private int z;

    public static Vec3i from(Vector3i vector3i) {
        return new Vec3i(vector3i.getX(), vector3i.getY(), vector3i.getZ());
    }

//    public Vector3i to(Vec3i self) {
//        return Vector3i.from(self.x, self.y, self.z);
//    }
}
