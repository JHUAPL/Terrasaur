#ifndef GRAVITY_POINT_H
#define GRAVITY_POINT_H

#include "mathutil.h"

inline double getGravityPoint(const double fieldPoint[3], double acc[3])
{
    double rhat[3] = {fieldPoint[0], fieldPoint[1], fieldPoint[2]};
    double r = Normalize(rhat);

    double r2 = fieldPoint[0]*fieldPoint[0] + fieldPoint[1]*fieldPoint[1] + fieldPoint[2]*fieldPoint[2];

    double potential = 1.0 / r;
    acc[0] = -rhat[0] / r2;
    acc[1] = -rhat[1] / r2;
    acc[2] = -rhat[2] / r2;

    return potential;
}


#endif // GRAVITY_POINT_H
