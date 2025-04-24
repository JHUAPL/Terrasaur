#ifndef __ICP_GSL_H__
#define __ICP_GSL_H__

#include "lidardata.h"

/**
 * Run a variant of the Iterative Closest Point algorithm to estimate the optimal translation
 * and rotation that best aligns the source points to the surface.
 *
 * @param[in,out] source source point for which we want to find closest points on body.
 *                On output, this array contains the translated points
 * @param[in] n number of points in source
 * @param[in,out] additionalPoints once optimal translation is found, translate these additional points using the optimal translation
 * @param[out] translation the optimal translation computed by this function. Assumed to be a 3 element vector.
 * @param[out] rotation the optimal rotation computed by this function. Assumed to be a 4 element quaternion.
 * @param[out] centerOfRotation the center of rotation of the optimal rotation computed by this function. Assumed to be a 3 element vector.
 * @param[out] closestPoints the point on the surface of the body closest to the translated source points
 */
void icpGsl(LidarPointCloud& source, double translation[3], double rotation[4], double centerOfRotation[3], int maxNumPointsToUse, double endFractionToIgnore, bool translationOnly, bool rotationOnly);

#endif
