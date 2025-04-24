#include <iostream>
#include <fstream>
#include <time.h>
#include <libgen.h>
#include "SpiceUsr.h"
#include "gravity-werner.h"
#include "gravity-cheng.h"
#include "gravity-point.h"
#include "util.h"
#include <fenv.h>
#include <time.h>
#include <stdlib.h>
#include <string.h>

using namespace std;

//old value for gravitational constant
//static const double g_G = 6.67384e-11 * 1.0e-9;

//gravitational constant new value as of Aug 16, 2017. units in m^3/kgs^2
static const double defaultG = 6.67408e-11;

typedef enum GravityAlgorithmType
{
    WERNER,
    CHENG,
    POINT_SOURCE
} GravityAlgorithmType;

typedef enum HowToEvaluateAtPlate
{
    EVALUATE_AT_CENTER,
    EVALUATE_AT_VERTEX,
    AVERAGE_VERTICES,
    FROM_FILE
} HowToEvaluateAtPlate;

struct GravityResult
{
    double potential;
    double acc[3];
    double elevation;
    bool filled;
};

static void saveResults(char* pltfile,
                        string outputFolder,
                        const vector<GravityResult>& results,
                        bool saveElevation,
                        string suffix)
{
    string pltfilebasename = basename(pltfile);

    string outputPot = outputFolder + "/" + pltfilebasename + "-potential.txt" + suffix;
    string outputAcc = outputFolder + "/" + pltfilebasename + "-acceleration.txt" + suffix;

	cout << "saving to potential file:" << outputPot << "\n";
	cout << "saving to acceleration file:" << outputAcc << "\n";
	
    ofstream foutP(outputPot.c_str());
    if (!foutP.is_open())
    {
        cerr << "Error: Unable to open file for writing" << endl;
        exit(1);
    }
    foutP.precision(16);

    ofstream foutA(outputAcc.c_str());
    if (!foutA.is_open())
    {
        cerr << "Error: Unable to open file for writing" << endl;
        exit(1);
    }
    foutA.precision(16);

    int size = results.size();
    for (int i=0; i<size; ++i)
    {
        foutP << results[i].potential << endl;
        foutA << results[i].acc[0] << " " << results[i].acc[1] << " " << results[i].acc[2] << endl;
    }

    foutP.close();
    foutA.close();

    if (saveElevation)
    {
        string outputElev = outputFolder + "/" + pltfilebasename + "-elevation.txt" + suffix;

        ofstream foutE(outputElev.c_str());
        if (!foutE.is_open())
        {
            cerr << "Error: Unable to open file for writing" << endl;
            exit(1);
        }
        foutE.precision(16);

        for (int i=0; i<size; ++i)
        {
            foutE << results[i].elevation << endl;
        }

        foutE.close();
    }
}

static void usage()
{
    cout << "This program computes the gravitational acceleration and potential of a \n"
         << "shape model at specified points and saves the values to files.\n\n"
         << "Usage: gravity [options] <platemodelfile>\n\n"
         << "Where:\n"
         << "  <platemodelfile>        Path to shape model file in OBJ or Gaskell PLT format.\n\n"
         << "Options:\n"
         << "  -d <value>              Density of shape model in g/cm^3 (default is 1)\n"
         << "  -r <value>              Rotation rate of shape model in radians/sec (default is 0)\n"
         << "  --gravConst <value>     Change the gravitational constant to use. Units are (in gcm^3/s^2)\n"
         << "                          (default is " << defaultG << ")\n"
         << "  --werner                Use the Werner algorithm for computing the gravity (this is the\n"
         << "                          default if neither --werner or --cheng option provided)\n"
         << "  --cheng                 Use Andy Cheng's algorithm for computing the gravity (default is to\n"
         << "                          use Werner method if neither --werner or --cheng option provided)\n"
         << "  --centers               Evaluate gravity directly at the centers of plates (this is the default\n"
         << "                          if neither --centers or -vertices or --file option provided)\n"
         << "  --average-vertices      Evaluate gravity of each plate by averaging the gravity computed at the\n"
         << "                          3 vertices of the plate (default is to evaluate at centers)\n"
         << "  --vertices              Evaluate gravity directly at each vertex (default is to evaluate at centers)\n"
         << "                          When using this option, you must also add the --cheng option since singularities\n"
         << "                          occur at the vertices with the Werner algorithm.\n"
         << "  --file <filename>       Evaluate gravity at points specified in file (default is to evaluate\n"
         << "                          at centers)\n"
         << "  --ref-potential <value> If the --file option is provided, then use this option to specify the reference\n"
         << "                          potential (in J/kg) which is needed for calculating elevation. This option is ignored if\n"
         << "                          --file is not provided. If --file is provided but --ref-potential is not\n"
         << "                          provided then no elevation data is saved out.\n"
         << "  --columns <int,int,int> If --file is provided, then this options controls which columns of the file are\n"
         << "                          assumed to contain the x, y, and z coordinates of the points. By default, columns\n"
         << "                          0, 1, and 2 are read. If you wanted, say, columns 3, 4, and 5 instead you would\n"
         << "                          include this option as for example: --columns 3,4,5. Note that they are separated\n"
         << "                          by commas (no spaces) and are zero based. If --file is not provided, then this\n"
         << "                          option is ignored.\n"
         << "  --start-index <value>\n"
         << "  --end-index <value>     use these 2 options to specify a range of plates or points to process. For example if\n"
         << "                          --start-index is 1000 and --end-index is 2000, then only plates or points 1000 through\n"
         << "                          1999 are processed. This is useful for parallelizing large shape models on\n"
         << "                          multiple machines.\n"
         << "  --suffix <value>        If specified, the suffix will be appended to all output files. This is needed when\n"
         << "                          splitting large shape models into mulitple runs so that each run will be output to\n"
         << "                          different files.\n"
         << "  --output-folder <folder>\n"
         << "                          Path to folder in which to place output files (default is current directory).\n"
         << endl;

    exit(1);
}

int main(int argc, char** argv)
{
    const int numberRequiredArgs = 1;
    if (argc-1 < numberRequiredArgs)
        usage();

    double density = 1.0;
    double omega = 0.0;
    //double density = 2.67; // for Eros
    //double density = 1.95; // for Itokawa
    //double density = 3.456; // for Vesta
    //double density = 2.5;  // for Andy Cheng's gravity poster
    //double omega = 0.0003311657616706400000;// for Eros (in radians per second)
    //double omega = 0.000143857148947075; // for Itokawa (in radians per second)
    //double omega = (1617.3329428 / 86400.0) * (M_PI / 180.0); // for Vesta (in radians per second)
    GravityAlgorithmType gravityType = WERNER;
    HowToEvaluateAtPlate howToEvalute = EVALUATE_AT_CENTER;
    char* fieldpointsfile = 0;
    double refPotential = 0.0;
    bool refPotentialProvided = false;
    int fileColumns[3] = {0, 1, 2};
    int startIndex = -1;
    int endIndex = -1;
    string suffix = "";
    string outputFolder = ".";
    double g_G = defaultG;
    
    int i = 1;
    for(; i<argc; ++i)
    {
        if (!strcmp(argv[i], "-d"))
        {
            density = atof(argv[++i]);
        }
        else if (!strcmp(argv[i], "-r"))
        {
            omega = atof(argv[++i]);
        }
        else if (!strcmp(argv[i], "--werner"))
        {
            gravityType = WERNER;
        }
        else if (!strcmp(argv[i], "--cheng"))
        {
            gravityType = CHENG;
        }
        else if (!strcmp(argv[i], "--pointsource"))
        {
            gravityType = POINT_SOURCE;
        }
        else if (!strcmp(argv[i], "--centers"))
        {
            howToEvalute = EVALUATE_AT_CENTER;
        }
        else if (!strcmp(argv[i], "--average-vertices"))
        {
            howToEvalute = AVERAGE_VERTICES;
        }
        else if (!strcmp(argv[i], "--vertices"))
        {
            howToEvalute = EVALUATE_AT_VERTEX;
        }
        else if (!strcmp(argv[i], "--file"))
        {
            howToEvalute = FROM_FILE;
            fieldpointsfile = argv[++i];
        }
        else if (!strcmp(argv[i], "--ref-potential"))
        {
            refPotential = atof(argv[++i]);
            refPotentialProvided = true;
        }
        else if (!strcmp(argv[i], "--columns"))
        {
            vector<string> tokens = split(argv[++i], ',');
            if (tokens.size() != 3)
                usage();
            fileColumns[0] = atoi(tokens[0].c_str());
            fileColumns[1] = atoi(tokens[1].c_str());
            fileColumns[2] = atoi(tokens[2].c_str());
        }
        else if (!strcmp(argv[i], "--start-index"))
        {
            startIndex = atoi(argv[++i]);
        }
        else if (!strcmp(argv[i], "--end-index"))
        {
            endIndex = atoi(argv[++i]);
        }
        else if (!strcmp(argv[i], "--suffix"))
        {
            suffix = argv[++i];
        }
        else if (!strcmp(argv[i], "--output-folder"))
        {
            outputFolder = argv[++i];
        }
        else if (!strcmp(argv[i], "--gravConst"))
        {
            g_G = atof(argv[++i]);
        }
        else
        {
            break;
        }
    }
    
    //convert grav constant to units of meters and grams
    g_G = g_G * 1.0e-9;
    
 	cout << "gravitational constant (in gcm^3/s^2) is " << g_G << "\n";

    cout << "writing to outputFolder:" << outputFolder << "\n";
    
    // There must be numRequiredArgs arguments remaining after the options. Otherwise abort.
    if (argc - i != numberRequiredArgs)
        usage();

    if ((howToEvalute == EVALUATE_AT_VERTEX || howToEvalute == AVERAGE_VERTICES) && gravityType == WERNER)
    {
        cout << "Warning: When evaluating at vertices, use the Cheng algorithm since\n"
             << "singularities occur at vertices with the Werner algorithm. Continuing anyway."
             << endl;
    }

    char* pltfile = argv[i];

    cout.setf(ios::fixed,ios::floatfield);
    cout.precision(2);
    clock_t t1, t2;

    t1 = clock();
    Platemodel* polyData = 0;
    if (gravityType == WERNER)
        polyData = initializeGravityWerner(pltfile);
    else if (gravityType == CHENG)
        polyData = initializeGravityCheng(pltfile);
    else
        abort();
    t2 = clock();
    double elapsed_time = (double)(t2 - t1) / CLOCKS_PER_SEC;
    cout << "Initialization time: " << elapsed_time  << " sec" << endl;

    double acc[3] = {0.0, 0.0, 0.0};
    double potential = 0.0;

    vector<GravityResult> plateResults;

    if (howToEvalute == FROM_FILE)
    {
        ifstream fin(fieldpointsfile);
        if (fin.is_open())
        {
            t1 = clock();
            string line;
            int count = 0;
            int lineNumber = -1;
            while (getline(fin, line))
            {
                ++lineNumber;
                if (startIndex >= 0 && endIndex >= 0)
                {
                    if (lineNumber < startIndex || lineNumber >= endIndex)
                        continue;
                }

                vector<string> tokens = split(line);
                double pt[3] = {
                    atof(tokens[ fileColumns[0] ].c_str()),
                    atof(tokens[ fileColumns[1] ].c_str()),
                    atof(tokens[ fileColumns[2] ].c_str())
                };

                if (gravityType == WERNER)
                    potential = 1.0e6*1.0e12*g_G*density*getGravityWerner(pt, acc);
                else
                    potential = 1.0e6*1.0e12*g_G*density*getGravityCheng(pt, acc);

                acc[0] *= 1.0e3 * 1.0e12 * g_G * density;
                acc[1] *= 1.0e3 * 1.0e12 * g_G * density;
                acc[2] *= 1.0e3 * 1.0e12 * g_G * density;

                // add centrifugal force
                if (omega != 0.0)
                {
                    potential -= 1.0e6 * 0.5 * omega*omega * (pt[0]*pt[0] + pt[1]*pt[1]);
                    acc[0] += 1.0e3 * omega*omega * pt[0];
                    acc[1] += 1.0e3 * omega*omega * pt[1];
                    // do nothing for z component
                }

                GravityResult result;
                result.potential = potential;
                result.acc[0] = acc[0];
                result.acc[1] = acc[1];
                result.acc[2] = acc[2];
                if (refPotentialProvided)
                    result.elevation = (potential - refPotential) / Norm(acc);
                plateResults.push_back(result);

                if ((count+1) % 10000 == 0)
                {
                    t2 = clock();
                    double elapsed_time = (double)(t2 - t1) / CLOCKS_PER_SEC;
                    cout << "Time to evaluate at " << count+1 << " points: " << elapsed_time  << " sec" << endl;
                }

                ++count;
            }
            t2 = clock();
            double elapsed_time = (double)(t2 - t1) / CLOCKS_PER_SEC;
            cout << "Time to evaluate total of " << count << " points: " << elapsed_time  << " sec" << endl;
        }
        else
        {
            cerr << "Error: Unable to open file '" << fieldpointsfile << "'" << endl;
            exit(1);
        }
    }
    else
    {
        t1 = clock();

        vector<GravityResult> pointResults;
        int numFilled = 0;
        if (howToEvalute == AVERAGE_VERTICES)
        {
            int numPoints = polyData->getNumberOfPoints();
            pointResults.resize(numPoints);
            for (i=0; i<numPoints; ++i)
                pointResults[i].filled = false;
        }

        int idList[3];

        int numPlates = polyData->getNumberOfPlates();
        int numPoints = polyData->getNumberOfPoints();

        if (startIndex < 0 || endIndex < 0)
        {
            startIndex = 0;
            if (howToEvalute == EVALUATE_AT_VERTEX)
                endIndex = numPoints;
            else
                endIndex = numPlates;
        }

        cout << "total number of indicies to process: " << (endIndex - startIndex) << endl;
        int counter = 1;
        for (i=startIndex; i<endIndex; ++i)
        {
            if (howToEvalute == EVALUATE_AT_CENTER)
            {
                polyData->getPlatePoints(i, idList);

                double pt1[3];
                double pt2[3];
                double pt3[3];
                polyData->getPoint(idList[0], pt1);
                polyData->getPoint(idList[1], pt2);
                polyData->getPoint(idList[2], pt3);

                double center[3];

                TriangleCenter(pt1, pt2, pt3, center);

                if (gravityType == WERNER)
                    potential = 1.0e6*1.0e12*g_G*density*getGravityWerner(center, acc);
                else
                    potential = 1.0e6*1.0e12*g_G*density*getGravityCheng(center, acc);

                acc[0] *= 1.0e3 * 1.0e12 * g_G * density;
                acc[1] *= 1.0e3 * 1.0e12 * g_G * density;
                acc[2] *= 1.0e3 * 1.0e12 * g_G * density;

                // add centrifugal force
                if (omega != 0.0)
                {
                    potential -= 1.0e6 * 0.5 * omega*omega * (center[0]*center[0] + center[1]*center[1]);
                    acc[0] += 1.0e3 * omega*omega * center[0];
                    acc[1] += 1.0e3 * omega*omega * center[1];
                    // do nothing for z component
                }

                if (counter % 10000 == 0)
                {
                    t2 = clock();
                    double elapsed_time = (double)(t2 - t1) / CLOCKS_PER_SEC;
                    cout << "Time to evaluate " << counter << " plates: " << elapsed_time  << " sec" << endl;
                    
                }
            }
            else if(howToEvalute == AVERAGE_VERTICES)
            {
                polyData->getPlatePoints(i, idList);

                double pt[3];
                acc[0] = 0.0;
                acc[1] = 0.0;
                acc[2] = 0.0;
                potential = 0.0;
                for (int j=0; j<3; ++j)
                {
                    int ptId = idList[j];
                    GravityResult& result = pointResults[ptId];
                    if (!result.filled)
                    {
                        polyData->getPoint(ptId, pt);

                        if (gravityType == WERNER)
                            result.potential = 1.0e6*1.0e12*g_G*density*getGravityWerner(pt, result.acc);
                        else
                            result.potential = 1.0e6*1.0e12*g_G*density*getGravityCheng(pt, result.acc);

                        result.acc[0] *= 1.0e3 * 1.0e12 * g_G * density;
                        result.acc[1] *= 1.0e3 * 1.0e12 * g_G * density;
                        result.acc[2] *= 1.0e3 * 1.0e12 * g_G * density;

                        // add centrifugal force
                        if (omega != 0.0)
                        {
                            result.potential -= 1.0e6 * 0.5 * omega*omega * (pt[0]*pt[0] + pt[1]*pt[1]);
                            result.acc[0] += 1.0e3 * omega*omega * pt[0];
                            result.acc[1] += 1.0e3 * omega*omega * pt[1];
                            // do nothing for z component
                        }

                        result.filled = true;

                        if ((numFilled+1) % 10000 == 0)
                        {
                            t2 = clock();
                            double elapsed_time = (double)(t2 - t1) / CLOCKS_PER_SEC;
                            cout << "Time to evaluate " << numFilled+1 << " vertices: " << elapsed_time  << " sec" << endl;
                        }
                        ++numFilled;
                    }

                    potential += result.potential;
                    acc[0] += result.acc[0];
                    acc[1] += result.acc[1];
                    acc[2] += result.acc[2];
                }

                potential /= 3.0;
                acc[0] /= 3.0;
                acc[1] /= 3.0;
                acc[2] /= 3.0;
            }
            else if (howToEvalute == EVALUATE_AT_VERTEX)
            {
                double pt[3];
                polyData->getPoint(i, pt);

                if (gravityType == WERNER)
                    potential = 1.0e6*1.0e12*g_G*density*getGravityWerner(pt, acc);
                else
                    potential = 1.0e6*1.0e12*g_G*density*getGravityCheng(pt, acc);

                acc[0] *= 1.0e3 * 1.0e12 * g_G * density;
                acc[1] *= 1.0e3 * 1.0e12 * g_G * density;
                acc[2] *= 1.0e3 * 1.0e12 * g_G * density;

                // add centrifugal force
                if (omega != 0.0)
                {
                    potential -= 1.0e6 * 0.5 * omega*omega * (pt[0]*pt[0] + pt[1]*pt[1]);
                    acc[0] += 1.0e3 * omega*omega * pt[0];
                    acc[1] += 1.0e3 * omega*omega * pt[1];
                    // do nothing for z component
                }

                if (counter % 10000 == 0)
                {
                    t2 = clock();
                    double elapsed_time = (double)(t2 - t1) / CLOCKS_PER_SEC;
                    cout << "Time to evaluate " << counter << " vertices: " << elapsed_time  << " sec" << endl;
                }
            }

            GravityResult result;
            result.potential = potential;
            result.acc[0] = acc[0];
            result.acc[1] = acc[1];
            result.acc[2] = acc[2];
            plateResults.push_back(result);
            
            counter++;
        }

        t2 = clock();
        double elapsed_time = (double)(t2 - t1) / CLOCKS_PER_SEC;
        cout << "Total loops: " << counter << endl;
        cout << "Total time: " << elapsed_time  << " sec" << endl;
    }

    // Only save out elevation if user provided a list of points and
    // also specified a reference potential. If calculating at plate
    // centers only, then a separate program must be used to calculate
    // elevation and slope.
    bool saveElevation = refPotentialProvided && howToEvalute == FROM_FILE;
    saveResults(pltfile, outputFolder, plateResults, saveElevation, suffix);

    return 0;
}
