#include "platemodel.h"
#include "util.h"
#include <fstream>
#include <string>
#include <vector>
#include <iostream>
#include <stdlib.h>

using namespace std;

Platemodel::Platemodel()
{
}

void Platemodel::loadGaskell(std::string filename)
{
    ifstream fin(filename.c_str());
    if (fin.is_open())
    {
        int numPoints = -1;
        int numPlates = -1;
        string line;

        getline(fin, line);
        trim(line);
        vector<string> tokens = split(line);
        if (tokens.size() >= 1)
            numPoints = atoi(tokens[0].c_str());
        if (tokens.size() == 2)
            numPlates = atoi(tokens[1].c_str());
        if (tokens.size() < 1 || tokens.size() > 2)
        {
            cerr << "Error: File format incorrect " << endl;
            exit(1);
        }

        Vertex x;
        for (int i=0; i<numPoints; ++i)
        {
            getline(fin, line);
            trim(line);
            tokens = split(line);
            if (tokens.size() != 4)
            {
                cerr << "Error: File format incorrect" << endl;
                exit(1);
            }
            x.point[0] = atof(tokens[1].c_str());
            x.point[1] = atof(tokens[2].c_str());
            x.point[2] = atof(tokens[3].c_str());
            vertices.push_back(x);
        }

        if (numPlates < 0)
        {
            getline(fin, line);
            trim(line);
            vector<string> tokens = split(line);
            if (tokens.size() != 1)
            {
                cerr << "Error: File format incorrect" << endl;
                exit(1);
            }
            numPlates = atoi(tokens[0].c_str());
        }

        Plate p;
        for (int i=0; i<numPlates; ++i)
        {
            getline(fin, line);
            trim(line);
            tokens = split(line);
            if (tokens.size() != 4)
            {
                cerr << "Error: File format incorrect" << endl;
                exit(1);
            }
            p.cell[0] = atoi(tokens[1].c_str())-1;
            p.cell[1] = atoi(tokens[2].c_str())-1;
            p.cell[2] = atoi(tokens[3].c_str())-1;

            ComputeNormal(
                        vertices[p.cell[0]].point,
                        vertices[p.cell[1]].point,
                        vertices[p.cell[2]].point,
                        p.normal
                        );

            plates.push_back(p);
        }
    }
    else
    {
        cerr << "Error: Unable to open file '" << filename << "'" << endl;
        exit(1);
    }
}

void Platemodel::loadOBJ(std::string filename)
{
    ifstream fin(filename.c_str());
    if (fin.is_open())
    {
        Vertex x;
        Plate p;
        string line;
        string type;
        int lindex = 0;
        while (getline(fin, line))
        {
            trim(line);

            if (line.length() == 0 || line[0] == '#')
                continue;

            vector<string> tokens = split(line);
            if (tokens.size() != 4)
            {
                cerr << "Error: File format incorrect" << endl;
                cerr << "line content:" << line << endl;
                exit(1);
            }

            type = tokens[0];
            if (type == "v" || type == "V")
            {
                x.point[0] = atof(tokens[1].c_str());
                x.point[1] = atof(tokens[2].c_str());
                x.point[2] = atof(tokens[3].c_str());
                vertices.push_back(x);
            }
            else if (type == "f" || type == "F")
            {
                p.cell[0] = atoi(tokens[1].c_str())-1;
                p.cell[1] = atoi(tokens[2].c_str())-1;
                p.cell[2] = atoi(tokens[3].c_str())-1;

                ComputeNormal(
                            vertices[p.cell[0]].point,
                            vertices[p.cell[1]].point,
                            vertices[p.cell[2]].point,
                            p.normal
                            );

                plates.push_back(p);
            }
            else
            {
                cerr << "Warning: Only lines beginning with 'v' or 'f' are currently parsed" << endl;
            }
            lindex = lindex + 1;
        }
    }
    else
    {
        cerr << "Error: Unable to open file '" << filename << "'" << endl;
        exit(1);
    }
}

void Platemodel::load(std::string filename)
{
    bool isObj = false;

    // Try to guess the format. Assume OBJ if the first non blank or comment line
    // begins with a v or f
    {
    ifstream fin(filename.c_str());
    if (fin.is_open())
    {
        string line;
        string type;
        while (getline(fin, line))
        {
            trim(line);

            if (line.length() == 0 || line[0] == '#')
                continue;

            vector<string> tokens = split(line);
            if (tokens.size() == 0)
            {
                cerr << "Error: File format not recognized" << endl;
                exit(1);
            }


            if (tokens.size() == 1 || tokens.size() == 2)
            {
                // If true, this is the first line of the a gaskell formatted file
                // which contains either the number of vertices
                // or both the number of vertices and the number of plates.
                isObj = false;
                break;
            }

            type = tokens[0];
            if (type == "v" || type == "V" || type == "f" || type == "F")
            {
                // If true, this is an OBJ file
                isObj = true;
                break;
            }
        }
        fin.close();
    }
    }

    if (isObj)
        loadOBJ(filename);
    else
        loadGaskell(filename);
}
