#ifndef LIDARDATA_H
#define LIDARDATA_H

#include <string>
#include <iostream>
#include <vector>

using namespace std;

struct Point
{
    double time;
    double targetpos[3];
    double scpos[3];

    void print()
    {
        std::cout << targetpos[0] << " " << targetpos[1] << " " << targetpos[2] << std::endl;
    }
};

typedef std::vector<Point> LidarPointCloud;

LidarPointCloud loadPointCloudSQLite(const string& filename,
                                     const string& startId,
                                     const string& stopId,
				     const int precision);

LidarPointCloud loadPointCloudFromFile(const string& filename, const int precision);

void computePointCloudStats(const LidarPointCloud& pointCloud, double& minError, double& maxError, double& rms, double& meanError, double& std);

void savePointCloudSBMT(const LidarPointCloud& pointCloud, const std::string& filename);


#endif // LIDARDATA_H
