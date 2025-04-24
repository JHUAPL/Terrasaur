#include "lidardata.h"
#include "closest-point-vtk.h"
#include "util.h"
#include "mathutil.h"
#include "SpiceUsr.h"
#include <limits>
#include <fstream>
#include <iostream>
#include <math.h>
#include <stdlib.h>
#include <sqlite3.h>


LidarPointCloud loadPointCloudSQLite(const string& filename,
                                     const string& startId,
                                     const string& stopId,
				     const int precision)
{
    LidarPointCloud referenceTrajectory;
    double scale = pow(10, precision);

    sqlite3 *db;
    sqlite3_stmt *stmt;

    /* Open database */
    int retval = sqlite3_open_v2(filename.c_str(), &db, SQLITE_OPEN_READONLY, 0);
    if(retval)
    {
        printf("Can't open database: %s\n", sqlite3_errmsg(db));
        exit(1);
    }
    else
    {
#ifndef NDEBUG
        printf("Opened database successfully\n");
#endif
    }


    /* Create SQL statement */
    const string& query = "SELECT et, xtarget, ytarget, ztarget, xsc, ysc, zsc from lidar where idvalid between " + startId + " and " + stopId;
    retval = sqlite3_prepare_v2(db, query.c_str(), -1, &stmt, 0);
#ifndef NDEBUG
    printf("Running query: %s\n", query.c_str());
#endif
    if(retval)
    {
      printf("Query Failed, error code: %d\n", retval);
      exit(1);
    }

    while(true)
    {
        // fetch a row's status
        retval = sqlite3_step(stmt);

        if(retval == SQLITE_ROW)
        {
            // SQLITE_ROW means fetched a row
            Point p;

            p.time = sqlite3_column_double(stmt, 0);
	    for (int i = 0; i < 3; i++) {
		p.targetpos[i] = sqlite3_column_double(stmt, i+1);
		p.targetpos[i] = round(p.targetpos[i] * scale) / scale;

		p.scpos[i] = sqlite3_column_double(stmt, i+4);
	    }

            referenceTrajectory.push_back(p);
        }
        else if(retval == SQLITE_DONE)
        {
            // All rows finished
#ifndef NDEBUG
            printf("All rows fetched\n");
#endif
            break;
        }
        else
        {
            // Some error encountered
            printf("Error encountered, error code: %d\n", retval);
            exit(1);
        }
    }

    sqlite3_finalize(stmt);

    // Close the handle to free memory
    sqlite3_close(db);

#ifndef NDEBUG
    printf("Finished loading %d points from database\n", (int)referenceTrajectory.size());
#endif

    return referenceTrajectory;
}

LidarPointCloud loadPointCloudFromFile(const string& filename, const int precision)
{
    LidarPointCloud referenceTrajectory;
    double scale = pow(10, precision);

    ifstream fin(filename.c_str());

    if (fin.is_open())
    {
        string line;
        while (getline(fin, line))
        {
            Point p;
            vector<string> tokens = split(line);

	    for (int i = 0; i < 3; i++) {
		p.targetpos[i] = atof(tokens[i+1].c_str());
		p.targetpos[i] = round(p.targetpos[i] * scale) / scale;

		p.scpos[i] = atof(tokens[i+4].c_str());
	    }

            utc2et_c(tokens[0].c_str(), &p.time);

            referenceTrajectory.push_back(p);
        }

        fin.close();
    }
    else
    {
        cerr << "Error: Unable to open file '" << filename << "'" << endl;
        exit(1);
    }

    return referenceTrajectory;
}

void computePointCloudStats(const LidarPointCloud& pointCloud, double& minError, double& maxError, double& rms, double& meanError, double& std)
{
    int size = pointCloud.size();
    int found;
    minError = std::numeric_limits<double>::max();
    maxError = 0.0;
    double errorSum  = 0.0;
    double errorSquaredSum = 0.0;
    for (int i=0; i<size; ++i)
    {
        const Point& p = pointCloud.at(i);

        // compute distance to closest point on shape model
        double closestPoint[3];
        findClosestPointVtk(p.targetpos, closestPoint, &found);

        double errorSquared = Distance2BetweenPoints(p.targetpos, closestPoint);
        double error = sqrt(errorSquared);

        errorSquaredSum += errorSquared;
        errorSum += error;

        if (error < minError)
            minError = error;
        if (error > maxError)
            maxError = error;
    }

    rms = sqrt(errorSquaredSum / (double)size);
    meanError = errorSum / (double)size;
    std = sqrt(errorSquaredSum / (double)size - meanError * meanError);
}

void savePointCloudSBMT(const LidarPointCloud& pointCloud, const std::string& filename)
{
    std::ofstream fout(filename.c_str());
    char utc[64];

    if (fout.is_open())
    {
        fout.precision(16);
        for (unsigned int i=0; i<pointCloud.size(); ++i)
        {
            const Point& p = pointCloud[i];

            et2utc_c(p.time, "ISOC", 6, sizeof(utc), utc);

            fout << utc << " "
                 << p.targetpos[0] << " "
                 << p.targetpos[1] << " "
                 << p.targetpos[2] << " "
                 << p.scpos[0] << " "
                 << p.scpos[1] << " "
                 << p.scpos[2] << " "
                 << sqrt(Distance2BetweenPoints(p.targetpos, p.scpos))
                 << "\n";
        }
        fout.close();
    }
    else
    {
        std::cerr << "Error: Unable to open file '" << filename << "'" << std::endl;
        exit(1);
    }

}
