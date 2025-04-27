#ifndef OPTIMIZER_H
#define OPTIMIZER_H

#include "lidardata.h"

/**
 * Class for finding the optimal translation of a lidar point cloud that best aligns it with
 * a shape model. To use this class, set the point cloud by calling setPointCloud. Then call optimize().
 * Finally, you can get the optimized (translated) point cloud by calling getOptimizedPointCloud() or the
 * optimal translation by calling getOptimalTranslation.
 */
class Optimizer
{
public:

    void setPointCloud(const LidarPointCloud& pointCloud);

    void setMaxNumberControlPoints(int maxPoints);

    void setEndFractionToIgnore(double endFractionToIgnore);

    void setTranslationOnly(bool translationOnly);

    void setRotationOnly(bool rotationOnly);

    void optimize();

    LidarPointCloud getOptimizedPointCloud() const;

    void getOptimalTransformation(double translation[3], double rotation[4], double centerOfRotation[3]) const;

private:

    LidarPointCloud pointCloud__;

    int maxNumberControlPoints__;

    double endFractionToIgnore__;

    bool translationOnly__;

    bool rotationOnly__;

    LidarPointCloud optimizedPointCloud__;

    double optimalTranslation__[3];
    double optimalRotation__[4];
    double optimalCenterOfRotation__[3];
};

#endif
