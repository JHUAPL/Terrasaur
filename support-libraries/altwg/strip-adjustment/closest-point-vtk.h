#ifndef __CLOSEST_POINT_VTK_H__
#define __CLOSEST_POINT_VTK_H__

#include "lidardata.h"

void initializeVtk(const char* dskfile);
void findClosestPointVtk(const double* origin, double* closestPoint, int* found);
void savePointCloudToVTK(const LidarPointCloud& pointCloud, const std::string& filename);

#endif
