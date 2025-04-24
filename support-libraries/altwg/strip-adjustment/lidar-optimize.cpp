#include <stdlib.h>
#include <fstream>
#include <iostream>
#include <string>
#include <string.h>
#include "SpiceUsr.h"
#include "optimizer.h"
#include "lidardata.h"
#include "closest-point-vtk.h"


/************************************************************************
* This program optimizes the location of a point cloud by trying to compute the best
* translation that best aligns the point cloud with a shape model. This program
* requires 3 arguments to run in this order:
* 1. objfile - the shape model in OBJ format
* 2. outputfile - the name to be given to the file which will contain the optimal transformation to be applied to the pointt cloud.
*                 the original point cloud file is not modified
* 3. inputfile - path to input point cloud file to be optimized
************************************************************************/

void saveTransformation(double translation[3],
                        double rotation[4],
                        double centerOfRotation[3],
                        const LidarPointCloud& pointCloudBefore,
                        const LidarPointCloud& pointCloudAfter,
                        const std::string& startId,
                        const std::string& stopId,
                        const std::string& filename)
{
    ofstream fout(filename.c_str());

    if (fout.is_open())
    {
        double minErrorBefore, minErrorAfter;
        double maxErrorBefore, maxErrorAfter;
        double rmsBefore, rmsAfter;
        double meanErrorBefore, meanErrorAfter;
        double stdBefore, stdAfter;
        computePointCloudStats(pointCloudBefore, minErrorBefore, maxErrorBefore, rmsBefore, meanErrorBefore, stdBefore);
        computePointCloudStats(pointCloudAfter, minErrorAfter, maxErrorAfter, rmsAfter, meanErrorAfter, stdAfter);

        fout.precision(16);
        fout << scientific
             << "{\n"
             << "\"translation\" : [ " << translation[0] << " , " << translation[1] << " , " << translation[2] << " ],\n"
             << "\"rotation\" : [ " << rotation[0] << " , " << rotation[1] << " , " << rotation[2] << " , " << rotation[3] << " ],\n"
             << "\"centerOfRotation\" : [ " << centerOfRotation[0] << " , " << centerOfRotation[1] << " , " << centerOfRotation[2] << " ],\n"
             << "\"startId\" : " << startId << ",\n"
             << "\"stopId\" : " << stopId << ",\n"
             << "\"minErrorBefore\" : " << minErrorBefore << ",\n"
             << "\"maxErrorBefore\" : " << maxErrorBefore << ",\n"
             << "\"rmsBefore\" : " << rmsBefore << ",\n"
             << "\"meanErrorBefore\" : " << meanErrorBefore << ",\n"
             << "\"stdBefore\" : " << stdBefore << ",\n"
             << "\"minErrorAfter\" : " << minErrorAfter << ",\n"
             << "\"maxErrorAfter\" : " << maxErrorAfter << ",\n"
             << "\"rmsAfter\" : " << rmsAfter << ",\n"
             << "\"meanErrorAfter\" : " << meanErrorAfter << ",\n"
             << "\"stdAfter\" : " << stdAfter << "\n"
             << "}\n";

        fout.close();
    }
    else
    {
        cerr << "Error: Unable to open file '" << filename << "'" << endl;
        exit(1);
    }

}

static void usage()
{
    cout << "This program takes lidar data and a shape model and tries to compute the optimal\n"
         << "translation and rotation that best aligns the lidar data with the shape model.\n"
         << "It implements a variation of the Iterative Closest Point algorithm. The program\n"
         << "supports 2 ways to provide the lidar data:\n"
         << "1. as a text file\n"
         << "2. an SQLite database along with the start and stop id of the lidar points to use\n\n"
         << "Usage: lidar-min-icp [options] <shapefile> <inputfile> <start-id> <stop-id>\n"
         << "                     <transformationfile>\n\n"
         << "Where:\n"
         << "  <shapefile>\n"
         << "          Path to shape model in OBJ format which data are optimized to.\n"
         << "  <inputfile>\n"
         << "          By default this is the path to SQLite database containing lidar data. If\n"
         << "          the --load-from-file option is used, then this is the path to a text file\n"
         << "          containing the lidar data. The format of the file is as follows. Each line\n"
         << "          must contain 7 space separated values. The first is the UTC string of the\n"
         << "          the lidar shot. The next 3 are the 3D coordinates of the lidar shot in\n"
         << "          kilometers. The final 3 values are the 3D coordinates of the spacecraft\n"
         << "          position in kilometers at the time of the lidar shot.\n"
         << "  <start-id>\n"
         << "          The start id of the lidar shot in the SQLite database. This option is ignored\n"
         << "          if the --load-from-file option is specified, but it still must be specified.\n"
         << "  <stop-id>\n"
         << "          The stop id of the lidar shot in the SQLite database. This option is ignored\n"
         << "          if the --load-from-file option is specified, but it still must be specified.\n"
         << "  <transformationfile>\n"
         << "          Path to file generated by this program upon termination of the optimization\n"
         << "          which contains the optimal transformation as a 4x4 matrix that\n"
         << "          best aligns the lidar data with the shape model, as well as other error\n"
         << "          statistics.\n\n"
         << "Options:\n"
         << "  --save <leap-second-kernel>\n"
         << "          Save out the lidar data both before and after the optimization to text files.\n"
         << "          This option is implied by the --load-from-file option. A SPICE leap second\n"
         << "          kernel must be provided in order to convert from ephemeris time to UTC.\n"
         << "  --max-number-control-points <int-value>\n"
         << "          max number of control points to use when optimizing the lidar data. For\n"
         << "          example suppose the actual number of points is 10000 points and you set this\n"
         << "          flag to 500. Then when doing the strip adjustment, only 500\n"
         << "          of the 10000 points are used to drive the optimization. This can increase\n"
         << "          performance significantly without sacrificing the quality of the strip\n"
         << "          adjustment. The control points are spread out evenly within the lidar points.\n"
         << "          If the max number of control points is greater than the number of lidar\n"
         << "          points, then all lidar points are used as control points. A value of 0\n"
         << "          means use all lidar points as control points. Default value is 0.\n"
         << "  --translation-only\n"
         << "          Only estimate a translation that best aligns the points with the model,\n"
         << "          not a rotation. By default both a translation and rotation are estimated.\n"
         << "  --rotation-only\n"
         << "          Only estimate a rotation that best aligns the points with the model,\n"
         << "          not a translation. By default both a translation and rotation are estimated.\n"
         << "  --load-from-file <leap-second-kernel>\n"
         << "          If specified then the second required argument to this program, <inputfile>,\n"
         << "          is assumed to refer to a text file containing the lidar points as explained\n"
         << "          above, rather than a an SQLite database. On output, the transformed lidar\n"
         << "          data are also saved out to a separate file (as if the --save option was\n"
         << "          specified). A SPICE leap second kernel must be provided in order to convert\n"
         << "          from UTC to ephemeris time.\n"
         << "  --end-fraction-to-ignore <value>\n"
         << "          Ignore points a specified fraction away from both ends of the window.\n"
         << "          Value must be between 0 and 1. Default is 0.\n"
	 << "  --precision <value>\n"
	 << "          Number of digits to carry in the initial point cloud positions.  Default is 6.\n";

    exit(1);
}

int main(int argc, char** argv)
{
    bool save = false;
    bool translationOnly = false;
    bool rotationOnly = false;
    bool loadFromFile = false;
    std::string leapSecondKernel;
    int maxControlPoints = 0;
    double endFractionToIgnore = 0.0;
    int precision = 6;

    int i = 1;
    for(; i<argc; ++i)
    {
        if (!strcmp(argv[i], "--save"))
        {
            save = true;
            leapSecondKernel = argv[++i];
        }
        else if (!strcmp(argv[i], "--max-number-control-points"))
        {
            maxControlPoints = atoi(argv[++i]);
        }
        else if (!strcmp(argv[i], "--translation-only"))
        {
            translationOnly = true;
        }
        else if (!strcmp(argv[i], "--rotation-only"))
        {
            rotationOnly = true;
        }
        else if (!strcmp(argv[i], "--load-from-file"))
        {
            loadFromFile = true;
            save = true;
            leapSecondKernel = argv[++i];
        }
        else if (!strcmp(argv[i], "--end-fraction-to-ignore"))
        {
            endFractionToIgnore = atof(argv[++i]);
        }
        else if (!strcmp(argv[i], "--precision"))
        {
	    precision = atoi(argv[++i]);
        }
        else
        {
            break;
        }
    }

    // There must be numRequiredArgs arguments remaining after the options. Otherwise abort.
    const int numberRequiredArgs = 5;
    if (argc - i != numberRequiredArgs)
        usage();

    const string shapefile = argv[i++];
    const string inputfile = argv[i++];
    const string startId = argv[i++];
    const string stopId = argv[i++];
    const string transformationfile = argv[i++];

    initializeVtk(shapefile.c_str());

    if (save)
    {
        furnsh_c(leapSecondKernel.c_str());
    }

    LidarPointCloud pointCloud;
    if (loadFromFile)
      pointCloud = loadPointCloudFromFile(inputfile, precision);
    else
      pointCloud = loadPointCloudSQLite(inputfile, startId, stopId, precision);

    if (save)
    {
        savePointCloudSBMT(pointCloud, transformationfile + "-sbmt-before.txt");
        savePointCloudToVTK(pointCloud, transformationfile + "-sbmt-before.vtk");
    }

    Optimizer optimizer;
    optimizer.setPointCloud(pointCloud);
    optimizer.setMaxNumberControlPoints(maxControlPoints);
    optimizer.setEndFractionToIgnore(endFractionToIgnore);
    optimizer.setTranslationOnly(translationOnly);
    optimizer.setRotationOnly(rotationOnly);
    optimizer.optimize();

    double translation[3];
    double rotation[4];
    double centerOfRotation[3];
    optimizer.getOptimalTransformation(translation, rotation, centerOfRotation);
    saveTransformation(
                translation,
                rotation,
                centerOfRotation,
                pointCloud,
                optimizer.getOptimizedPointCloud(),
                startId,
                stopId,
                transformationfile);

    if (save)
    {
        savePointCloudSBMT(optimizer.getOptimizedPointCloud(), transformationfile + "-sbmt-after.txt");
        savePointCloudToVTK(optimizer.getOptimizedPointCloud(), transformationfile + "-sbmt-after.vtk");
    }

    return 0;
}
