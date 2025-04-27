#include <stdio.h>
#include "optimizer.h"
#include "icp-gsl.h"
#include "mathutil.h"

void Optimizer::setPointCloud(const LidarPointCloud &pointCloud)
{
    pointCloud__ = pointCloud;
    maxNumberControlPoints__ = 0;
    endFractionToIgnore__ = 0.0;
    translationOnly__ = false;
    rotationOnly__ = false;
}

void Optimizer::setMaxNumberControlPoints(int maxPoints)
{
    maxNumberControlPoints__ = maxPoints;
}

void Optimizer::setEndFractionToIgnore(double endFractionToIgnore)
{
    endFractionToIgnore__ = endFractionToIgnore;
}

void Optimizer::setTranslationOnly(bool translationOnly)
{
    translationOnly__ = translationOnly;
}

void Optimizer::setRotationOnly(bool rotationOnly)
{
    rotationOnly__ = rotationOnly;
}

void Optimizer::optimize()
{
#ifndef NDEBUG
    printf("Optimizing point cloud with size %d\n", (int)pointCloud__.size());
#endif

    optimizedPointCloud__ = pointCloud__;

    if (translationOnly__)
        icpGsl(optimizedPointCloud__, optimalTranslation__, optimalRotation__, optimalCenterOfRotation__, maxNumberControlPoints__, endFractionToIgnore__, true, false);
    else if (rotationOnly__)
        icpGsl(optimizedPointCloud__, optimalTranslation__, optimalRotation__, optimalCenterOfRotation__, maxNumberControlPoints__, endFractionToIgnore__, false, true);
    else
        icpGsl(optimizedPointCloud__, optimalTranslation__, optimalRotation__, optimalCenterOfRotation__, maxNumberControlPoints__, endFractionToIgnore__, false, false);

#ifndef NDEBUG
    printf("Finished optimizing point cloud\n\n");
#endif
}

LidarPointCloud Optimizer::getOptimizedPointCloud() const
{
    return optimizedPointCloud__;
}

void Optimizer::getOptimalTransformation(double translation[3], double rotation[4], double centerOfRotation[3]) const
{
    translation[0] = optimalTranslation__[0];
    translation[1] = optimalTranslation__[1];
    translation[2] = optimalTranslation__[2];
    rotation[0] = optimalRotation__[0];
    rotation[1] = optimalRotation__[1];
    rotation[2] = optimalRotation__[2];
    rotation[3] = optimalRotation__[3];
    centerOfRotation[0] = optimalCenterOfRotation__[0];
    centerOfRotation[1] = optimalCenterOfRotation__[1];
    centerOfRotation[2] = optimalCenterOfRotation__[2];
}
